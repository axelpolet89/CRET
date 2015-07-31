package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import com.steadystate.css.parser.selectors.*;

import org.w3c.css.sac.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/2/2015.
 *
 * Responsible for transforming descendant-combinators into child-combinators if possible
 * Performs analysis on a MSelector and it's matched elements,
 * by crawling the parents and siblings of those DOM elements using the specification of the selector
 */
public class ChildCombinatorPlugin implements ICssPostCrawlPlugin
{
    private final Map<DescendantSelectorImpl, Boolean> _descendants = new HashMap<>();
    private int _selectorsTransformed = 0;


    @Override
    public void getStatistics(SuiteStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<DCS>%d</DCS>", prefix, _selectorsTransformed);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for(String fileName : cssRules.keySet())
        {
            LogHandler.info("[DescToChild] Analyzing selectors for over-qualified descendant-combinators in file '%s'", fileName);
            int count = 0;

            for(MCssRule mRule : cssRules.get(fileName).GetRules())
            {
                // possible replacements for this mRule
                Map<MSelector, MSelector> newSelectors = new HashMap<>();

                for(MSelector mSelector : mRule.GetSelectors())
                {
                    if(mSelector.IsIgnored())
                    {
                        continue;
                    }

                    _descendants.clear();

                    Selector w3cSelector = mSelector.GetW3cSelector();

                    LogHandler.debug("[DescToChild] [%s] Selector sequence may transformed using a child-combinator instead of a descendant-combinator", mSelector);

                    List<ElementWrapper> selectorElements = mSelector.GetMatchedElements();

                    // verify whether matched elements allow for child-combinators instead of descendant-combinators
                    for (ElementWrapper ew : selectorElements)
                    {
                        RecursiveFindDescendants(TryFilterPseudoElement(w3cSelector), ew.GetElement(), mSelector);
                    }

                    // if the selector does contains those selectors, replace them
                    if(_descendants.values().contains(true))
                    {
                        long size = _descendants.values().stream().filter(d -> d).count();
                        count += size;

                        LogHandler.debug("[DescToChild] [%s] Selector sequence contains '%d' descendant-combinators that can be replaced by child-combinators", mSelector, size);

                        LogHandler.debug("[DescToChild] [%s] Old selector text: '%s'", mSelector, w3cSelector);
                        Selector newW3cSelector = RecursiveUpdateSelector(w3cSelector);
                        LogHandler.debug("[DescToChild] [%s] New selector text: '%s'", mSelector, newW3cSelector);

                        // call copy constructor to create MSelector replacement
                        MSelector newSelector = new MSelector(newW3cSelector, mSelector);
                        newSelectors.put(mSelector, newSelector);
                        LogHandler.debug("[DescToChild] [%s] New MSelector created: '%s', will replace old", mSelector, newSelector);

                        _selectorsTransformed++;
                    }
                }

                // finally, replace some selector in this mRule
                for(MSelector oldSelector : newSelectors.keySet())
                {
                    mRule.ReplaceSelector(oldSelector, newSelectors.get(oldSelector));
                }
            }

            LogHandler.info("[DescToChild] Removed a total of %d over-qualified descendant-combinators, replaced by child-combinators", count);
        }

        return cssRules;
    }



    /**
     * Filter pseudo-element from a selector, since it is recognized as a descendant combinator (while it is not)
     * It can only be applied once, at the end of the subject selector sequence (http://www.w3.org/TR/css3-selectors/)
     * Therefore, it only has to be filtered once
     * @param selector
     * @return
     */
    private static Selector TryFilterPseudoElement(Selector selector)
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
     * Main method to find descendant selectors in a given selector, that can actually be replaced by child selectors
     * In order to discover them, we need to recursively process each selector part in the given selector starting at the end, looking up
     * We try to match each part in this selector by the node the 'end' of the selector matched to (by CssAnalyzer)
     *
     * In some cases (Descendant and GeneralAdjacent), we will not match a direct parent or direct previous sibling
     * of the given node, with the ancestor or sibling property of the given selector.
     * We then need to find the right node (either parent or previous sibling), before proceeding with further analysis of the remaining selector parts
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
            // until we find the one we can select with previousSelector, before continuing
            if(selector instanceof GeneralAdjacentSelectorImpl)
            {
                boolean found;
                do
                {
                    found = TrySelectNodeWithCss(previousSelector, previousNode, mSelector);
                    previousNode = previousNode.getPreviousSibling();
                }
                while(!found);
            }

            RecursiveFindDescendants(previousSelector, previousNode, mSelector);
        }
        else if (selector instanceof DescendantSelector)
        {
            DescendantSelectorImpl dSel = (DescendantSelectorImpl)selector;

            Node parent = node.getParentNode();
            Selector ancestor = dSel.getAncestorSelector();

            LogHandler.debug("[DescToChild] [%s] Trying to match direct parent node '%s' of node '%s' with the parent selector '%s' of descendant-selector '%s'", mSelector, parent, node, ancestor, selector);

            boolean atDocumentRoot = false;

            if (TrySelectNodeWithCss(ancestor, parent, mSelector))
            {
                if(!_descendants.containsKey(dSel))
                {
                    _descendants.put(dSel, true);
                    LogHandler.debug("[DescToChild] [%s] Direct parent node '%s' is selectable by ancestor-part '%s' of descendant-selector '%s', child-combinator MAY be allowed", mSelector, parent, ancestor, selector);
                }
            }
            else
            {
                _descendants.put(dSel, false);
                LogHandler.debug("[DescToChild] [%s] Direct parent node '%s' is NOT selectable by ancestor-part '%s' of descendant-selector '%s', child-combinator NOT allowed", mSelector, parent, ancestor, selector);

                // direct parent node is not selectable by ancestor-part of descendant-selector,
                // need to search up in DOM to find the parent that matched the ancestor-part, before continuing
                boolean found = false;
                while(!found)
                {
                    parent = parent.getParentNode();
                    if (parent instanceof Document)
                    {
                        atDocumentRoot = true;
                        LogHandler.warn("[DescToChild] [%s] Found document root while trying to find parent DOM element that should be selectable by ancestor '%s' of selector '%s'", mSelector, ancestor, selector);
                        break;
                    }
                    else
                    {
                        found = TrySelectNodeWithCss(ancestor, parent, mSelector);
                    }
                }
            }

            if(!atDocumentRoot)
            {
                RecursiveFindDescendants(ancestor, parent, mSelector);
            }
        }
    }


    /**
     * Filter method for selecting a node with a css selector
     * Since the caller of this method can pass another chained (e.g. Child, Sibling or Descendant) selector
     * and we are just interested in the 'last' selector part (e.g. the 'a' in div a), we need to find it using the selector type
     * @param selector
     * @param node
     * @param mSelector
     * @return
     */
    private static boolean TrySelectNodeWithCss(Selector selector, Node node, MSelector mSelector)
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

        if (TrySelectNodeWithCss(node, selToMatch))
        {
            LogHandler.debug("[DescToChild] [%s] Node '%s' is selectable by simple selector '%s' of selector '%s'", mSelector, PrintNode(node), selToMatch, selector);

            return true;
        }

        LogHandler.debug("[DescToChild] [%s] Node '%s' is NOT selectable by simple selector '%s' of selector '%s'", mSelector, PrintNode(node), selToMatch, selector);
        return false;
    }


    /**
     * Main method to select a given node by a given selector using the node's name (as html tag) and attribute and the selector's type
     * If selector is element-selector, just match by node name
     * If selector contains class or ID, match by node name and attribute equivalence of class and ID
     * If selector contains attribute with or without value, match by node name and attribute name and/or value
     * If selector contains pseudo-class condition, just match by node name
     * @param node
     * @param selector
     * @return true if node is selectable by given selector
     */
    private static boolean TrySelectNodeWithCss(Node node, Selector selector)
    {
        try
        {
            if (selector instanceof ElementSelectorImpl)
            {
                return MatchNodeWithElementSelector(node, selector);
            }
            else if (selector instanceof ConditionalSelectorImpl)
            {
                ConditionalSelectorImpl cSelector = (ConditionalSelectorImpl) selector;

                Selector innerSelector = cSelector.getSimpleSelector();
                Condition innerCondition = cSelector.getCondition();

                return RecursiveMatchConditionToNode(innerSelector, innerCondition, node);
            }
            else
            {
                LogHandler.warn("[DescToChild] Unsupported: Selector '%s' is no ElementSelector or ConditionalSelector, but a %s", selector, selector.getClass());
            }
        }
        catch(Exception ex)
        {
            LogHandler.error(ex, "[DescToChild] Error while matching node to selector for node '%s' and selector '%s'", PrintNode(node), selector);
        }

        return false;
    }


    private static boolean RecursiveMatchConditionToNode(Selector innerSelector, Condition condition, Node node)
    {
        if(condition instanceof AndConditionImpl)
        {
            AndConditionImpl andCondition = (AndConditionImpl)condition;
            return  MatchConditionToNode(innerSelector, andCondition.getSecondCondition(), node)
                    && RecursiveMatchConditionToNode(innerSelector, andCondition.getFirstCondition(), node);
        }

        return MatchConditionToNode(innerSelector, condition, node);
    }


    private static boolean MatchConditionToNode(Selector innerSelector, Condition innerCondition, Node node)
    {
        if (innerCondition instanceof IdConditionImpl)
        {
            IdConditionImpl idCondition = (IdConditionImpl) innerCondition;
            String attr = GetAttributeValue(node.getAttributes(), "id");
            if (MatchNodeWithElementSelector(node, innerSelector) && attr != null && attr.equals(idCondition.getValue()))
            {
                return true;
            }
        }
        else if (innerCondition instanceof ClassConditionImpl)
        {
            ClassConditionImpl classCondition = (ClassConditionImpl) innerCondition;
            String attr = GetAttributeValue(node.getAttributes(), "class");
            if (MatchNodeWithElementSelector(node, innerSelector) && attr != null && FindMatchInClass(attr, classCondition.getValue()))
            {
                return true;
            }
        }
        else if (innerCondition instanceof AttributeConditionImpl)
        {
            AttributeConditionImpl attrCondition = (AttributeConditionImpl) innerCondition;
            String attr = GetAttributeValue(node.getAttributes(), attrCondition.getLocalName());
            if(MatchNodeWithElementSelector(node, innerSelector) && attr != null)
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
        else if(innerCondition instanceof SubstringAttributeConditionImpl)
        {
            SubstringAttributeConditionImpl selectorAttribute = (SubstringAttributeConditionImpl) innerCondition;
            String nodeAttribute = GetAttributeValue(node.getAttributes(), selectorAttribute.getLocalName());
            if(MatchNodeWithElementSelector(node, innerSelector) && nodeAttribute != null)
            {
                String fulltext = selectorAttribute.toString();
                String subsValue = selectorAttribute.getValue();

                if(subsValue.isEmpty())
                {
                    return true;
                }

                if(fulltext.contains("*="))
                {
                    if (nodeAttribute.contains(subsValue))
                    {
                        return true;
                    }
                }
                else if(fulltext.contains("^="))
                {
                    if(nodeAttribute.startsWith(subsValue))
                    {
                        return true;
                    }
                }
                else
                {
                    if(nodeAttribute.endsWith(subsValue))
                    {
                        return true;
                    }
                }
            }
        }
        else if (innerCondition instanceof PseudoClassConditionImpl)
        {
            if(innerCondition.toString().equals(":root"))
            {
                return node instanceof Document;
            }
            else
            {
                return MatchNodeWithElementSelector(node, innerSelector);
            }
        }
        else
        {
            LogHandler.warn("[DescToChild] Unsupported: ConditionalSelector '%s' is no ID, CLASS or ATTRIBUTE SELECTOR, but a '%s'", innerSelector, innerSelector.getClass());
        }

        return false;
    }


    private static boolean FindMatchInClass(String classAttribute, String search)
    {
        for(String part : classAttribute.split("\\s"))
        {
            if(part.equals(search))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * @param node
     * @param elementSelector
     * @return true if given node matches the given element-selector (div, body, etc..) by name, or if element-selector equals '*'
     */
    private static boolean MatchNodeWithElementSelector(Node node, Selector elementSelector)
    {
        if(elementSelector.toString().equals("*"))
        {
            return true;
        }

        return node.getNodeName().equalsIgnoreCase(elementSelector.toString());
    }


    /**
     * @param attributes
     * @param attributeName
     * @return the value for the given attribute, or NULL
     */
    private static String GetAttributeValue(NamedNodeMap attributes, String attributeName)
    {
        if(attributes == null)
            return null;

        Node node = attributes.getNamedItem(attributeName);

        if(node != null)
            return node.getNodeValue();

        return null;
    }



    /**
     * Update given selector in place and return it
     * Update starting with the first selector, by seeking up using sibling and ancestor operators
     * @param selector
     * @return selector with one or more descendants replaced by childs
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

            // here we actually replace a given descendant-combinator with a child-combinator
            if(_descendants.containsKey(dSel) && _descendants.get(dSel))
            {
                return new ChildSelectorImpl(dSel.getAncestorSelector(), dSel.getSimpleSelector());
            }
        }

        return selector;
    }



    /**
     * @param node
     * @return Simple print of a node in HTML format, including attributes
     */
    private static String PrintNode(Node node)
    {
        SuiteStringBuilder builder = new SuiteStringBuilder();

        if(node instanceof Document)
            return "DOCUMENT ROOT";

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