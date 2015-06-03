package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import com.steadystate.css.parser.selectors.*;

import org.w3c.css.sac.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/2/2015.
 */
public class DescendantToChild implements ICssPostCrawlPlugin
{
    private final Map<DescendantSelectorImpl, Boolean> _descendants;

    public DescendantToChild()
    {
        _descendants = new HashMap<>();
    }

    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {
        for(String fileName : cssRules.keySet())
        {
            LogHandler.info("[DescToChild] Analyzing selectors for over-qualified descendant-combinators in file '%s'", fileName);
            int count = 0;

            for(MCssRule mRule : cssRules.get(fileName).GetRules())
            {
                Map<MSelector, MSelector> newSelectors = new HashMap<>();

                for(MSelector mSelector : mRule.GetSelectors())
                {
                    _descendants.clear();

                    Selector w3cSelector = mSelector.GetW3cSelector();

                    LogHandler.debug("[DescToChild] [%s] Selector sequence may transformed using a child-combinator instead of a descendant-combinator", mSelector);

                    List<ElementWrapper> matchedElements = mSelector.GetMatchedElements();

                    for (ElementWrapper ew : matchedElements)
                    {
                        Element element = ew.GetElement();

                        RecursiveFindDescendants(TryFilterPseudoElement(w3cSelector), element, mSelector);
                    }

                    if(_descendants.values().contains(true))
                    {
                        long size =  _descendants.values().stream().filter(d -> d).count();

                        LogHandler.debug("[DescToChild] [%s] Selector sequence contains '%d' descendant-combinators that can be replaced by child-combinators", mSelector, size);

                        LogHandler.debug("[DescToChild] [%s] Old selector text: '%s'", mSelector, w3cSelector);

                        Selector newW3cSelector = RecursiveUpdateSelector(w3cSelector);

                        LogHandler.debug("[DescToChild] [%s] New selector text: '%s'", mSelector, newW3cSelector);

                        count += size;

                        MSelector newSelector = new MSelector(newW3cSelector, mSelector.GetProperties(), mSelector.GetRuleNumber(), mSelector.GetMediaQueries());
                        newSelectors.put(mSelector, newSelector);
                        LogHandler.debug("[DescToChild] [%s] New selector created: '%s'", mSelector, newSelector);
                    }
                }

                for(MSelector oldSelector : newSelectors.keySet())
                {
                    mRule.ReplaceSelector(oldSelector, newSelectors.get(oldSelector));
                }
            }

            LogHandler.info("[DescToChild] Removed %d over-qualified descendant-combinators, replaced by child-combinators", count);
        }

        return cssRules;
    }



    /**
     *
     * @param selector
     * @return
     */
    private Selector TryFilterPseudoElement(Selector selector)
    {
        if(selector instanceof DescendantSelectorImpl)
        {
            DescendantSelectorImpl dSel = (DescendantSelectorImpl) selector;
            if (dSel.getSimpleSelector() instanceof PseudoElementSelectorImpl)
                return dSel.getAncestorSelector();
        }

        return selector;
    }


    /**
     *
     * @param selector
     * @param node
     * @param mSelector
     */
    private void RecursiveFindDescendants(Selector selector, Node node, MSelector mSelector)
    {
        if(selector instanceof ChildSelectorImpl)
        {
            RecursiveFindDescendants(((ChildSelectorImpl)selector).getAncestorSelector(), node.getParentNode(), mSelector);
        }
        else if (selector instanceof SiblingSelector)
        {
            Node previousNode = node.getPreviousSibling();
            Selector previousSelector = ((SiblingSelector) selector).getSelector();

            // node is not directly adjacent to previous node, need to search previous sibling nodes
            // until we find the one we can select with previousSelector
            if(selector instanceof GeneralAdjacentSelectorImpl)
            {
                boolean found;
                do
                {
                    found = MatchSimpleSelector(previousSelector, previousNode, mSelector);
                    previousNode = previousNode.getPreviousSibling();
                }
                while(!found);
            }

            RecursiveFindDescendants(previousSelector, previousNode, mSelector);
        }
        else if (selector instanceof DescendantSelector)
        {
            LogHandler.debug("[DescToChild] [%s] Try match direct parent node '%s' with the parent selector '%s'", mSelector, node, selector);

            DescendantSelectorImpl dSel = (DescendantSelectorImpl)selector;

            Node parent = node.getParentNode();
            Selector ancestor = dSel.getAncestorSelector();

            if (MatchSimpleSelector(ancestor, parent, mSelector))
            {
                if(!_descendants.containsKey(dSel))
                    _descendants.put(dSel, true);
            }
            else
            {
                _descendants.put(dSel, false);

                boolean found = false;
                while(!found)
                {
                    parent = parent.getParentNode();
                    if (parent instanceof Document)
                    {
                        LogHandler.warn("[DescToChild] [%s] Found document while finding DOM element that should match '%s' of selector '%s'", mSelector, ancestor, selector);
                    }
                    else
                    {
                        found = MatchSimpleSelector(ancestor, parent, mSelector);
                    }
                }
            }

            RecursiveFindDescendants(ancestor, parent, mSelector);
        }
    }


    /**
     *
     * @param selector
     * @param node
     * @param mSelector
     * @return
     */
    private boolean MatchSimpleSelector(Selector selector, Node node, MSelector mSelector)
    {
        Selector selToMatch = null;
        if(selector instanceof SimpleSelector || selector instanceof PseudoElementSelectorImpl)
        {
            selToMatch = selector;
        }
        else if(selector instanceof ChildSelectorImpl)
        {
            selToMatch = ((ChildSelectorImpl)selector).getSimpleSelector();
        }
        else if (selector instanceof SiblingSelector)
        {
            selToMatch = ((SiblingSelector)selector).getSiblingSelector();
        }
        else if (selector instanceof DescendantSelector)
        {
            selToMatch = ((DescendantSelector)selector).getSimpleSelector();
        }

        if (TrySelectNode(node, selToMatch))
        {
            LogHandler.debug("[DescToChild] [%s] Direct parent node '%s' is selectable by selector '%s' of selector sequence '%s', direct child may be allowed", mSelector, PrintNode(node), selToMatch, selector);

            return true;
        }

        LogHandler.debug("[DescToChild] [%s] Direct parent node '%s' can not be selected by selector '%s', direct child not allowed", mSelector, PrintNode(node), selToMatch, selToMatch);
        return false;
    }


    /**
     *
     * @param selector
     * @return
     */
    private Selector RecursiveUpdateSelector(Selector selector)
    {
        if(selector instanceof ChildSelectorImpl)
        {
            ChildSelectorImpl cSel = (ChildSelectorImpl) selector;
            cSel.setAncestorSelector(RecursiveUpdateSelector(cSel.getAncestorSelector()));
        }
        else if(selector instanceof SiblingSelector)
        {
            if(selector instanceof DirectAdjacentSelectorImpl)
            {
                DirectAdjacentSelectorImpl sSel = (DirectAdjacentSelectorImpl) selector;
                sSel.setSelector(RecursiveUpdateSelector(sSel.getSelector()));
            }
            else
            {
                GeneralAdjacentSelectorImpl sSel = (GeneralAdjacentSelectorImpl) selector;
                sSel.setSelector(RecursiveUpdateSelector(sSel.getSelector()));
            }
        }
        else if (selector instanceof DescendantSelectorImpl)
        {
            DescendantSelectorImpl dSel = (DescendantSelectorImpl)selector;
            dSel.setAncestorSelector(RecursiveUpdateSelector(dSel.getAncestorSelector()));

            if(_descendants.containsKey(dSel) && _descendants.get(dSel))
            {
                return new ChildSelectorImpl(dSel.getAncestorSelector(), dSel.getSimpleSelector());
            }
        }

        return selector;
    }


    /**
     *
     * @param node
     * @param selector
     * @return
     */
    private boolean TrySelectNode(Node node, Selector selector)
    {
        try
        {
            if (selector instanceof ElementSelectorImpl)
            {
                if (node.getNodeName().equalsIgnoreCase(((ElementSelectorImpl) selector).getLocalName()))
                    return true;
            }
            else if (selector instanceof ConditionalSelectorImpl)
            {
                ConditionalSelectorImpl cSelector = (ConditionalSelectorImpl) selector;
                Condition condition = cSelector.getCondition();

                if (condition instanceof IdConditionImpl)
                {
                    IdConditionImpl idCondition = (IdConditionImpl) condition;
                    String attr = GetAttributeValue(node.getAttributes(), "id");
                    if (attr != null && attr.equals(idCondition.getValue()))
                    {
                        return true;
                    }
                }
                else if (condition instanceof ClassConditionImpl)
                {
                    ClassConditionImpl classCondition = (ClassConditionImpl) condition;
                    String attr = GetAttributeValue(node.getAttributes(), "class");
                    if (attr != null && attr.contains(classCondition.getValue()))
                    {
                        return true;
                    }
                }
                else if (condition instanceof AttributeConditionImpl)
                {
                    AttributeConditionImpl attrCondition = (AttributeConditionImpl) condition;
                    String attr = GetAttributeValue(node.getAttributes(), attrCondition.getLocalName());
                    if(attr != null)
                    {
                        if(attrCondition.getValue().isEmpty())
                        {
                            return true;
                        }

                        if (attr.equals(attrCondition.getValue()))
                        {
                            return true;
                        }
                    }
                }
                else if (condition instanceof PseudoClassConditionImpl)
                {
                    if (node.getNodeName().equalsIgnoreCase(cSelector.getSimpleSelector().toString()))
                        return true;
                }
                else
                {
                    LogHandler.warn("[DescToChild] Unsupported: ConditionalSelector '%s' is no ID, CLASS or ATTRIBUTE SELECTOR, but a '%s'", selector, selector.getSelectorType());
                }
            }
            else
            {
                LogHandler.warn("[DescToChild] Unsupported: Selector '%s' is no ElementSelector or ConditionalSelector", selector);
            }
        }
        catch(Exception ex)
        {
            LogHandler.error(ex, "[DescToChild] Error while matching node to selector for node '%s' and selector '%s'", PrintNode(node), selector);
            return false;
        }

        return false;
    }


    /**
     *
     * @param attributes
     * @param attributeName
     * @return
     */
    private static String GetAttributeValue(NamedNodeMap attributes, String attributeName)
    {
        Node node = attributes.getNamedItem(attributeName);

        if(node != null)
            return node.getNodeValue();

        return null;
    }


    /**
     *
     * @param node
     * @return
     */
    private static String PrintNode(Node node)
    {
        SuiteStringBuilder builder = new SuiteStringBuilder();

        builder.append("<%s", node.getNodeName());

        NamedNodeMap map = node.getAttributes();

        if(map != null)
        {
            for (int i = 0; i < map.getLength(); i++)
            {
                builder.append(" %s=\"%s\"", map.item(i).getNodeName(), map.item(i).getNodeValue());
            }
        }

        builder.append("></%s>", node.getNodeName());

        return builder.toString();
    }
}