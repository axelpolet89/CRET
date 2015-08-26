package com.crawljax.plugins.cret.plugins;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.dommodel.ElementWrapper;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.interfaces.ICssTransformer;
import com.crawljax.plugins.cret.plugins.matcher.MatchedElements;
import com.crawljax.plugins.cret.util.CretStringBuilder;

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
public class ChildCombinatorPlugin implements ICssTransformer
{
    private final Map<DescendantSelectorImpl, Boolean> _descendants = new HashMap<>();
    private int _selectorsTransformed = 0;


    @Override
    public void getStatistics(CretStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<DCS>%d</DCS>", prefix, _selectorsTransformed);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for(String fileName : cssRules.keySet())
        {
            LogHandler.info("[ChildCombinator] Analyzing selectors for over-qualified descendant-combinators in file '%s'", fileName);
            int count = 0;

            for(MCssRule mRule : cssRules.get(fileName).getRules())
            {
                // possible replacements for this mRule
                Map<MSelector, MSelector> newSelectors = new HashMap<>();

                for(MSelector mSelector : mRule.getSelectors())
                {
                    if(mSelector.isIgnored())
                    {
                        continue;
                    }

                    LogHandler.debug("[ChildCombinator] [%s] Selector may transformed using a child-combinator instead of a descendant-combinator", mSelector);

                    Selector w3cSelector = mSelector.getW3CSelector();
                    List<ElementWrapper> selectorElements = mSelector.getMatchedElements();

                    _descendants.clear();

                    // verify whether ALL elements selected by given selector allow for child-combinators instead of descendant-combinators
                    for (ElementWrapper ew : selectorElements)
                    {
                        recursiveFindDescendants(tryFilterPseudoElement(w3cSelector), ew.getElement(), mSelector);
                    }

                    // are there any descendant selectors in given selector, which could be transformable?
                    if(_descendants.values().contains(true))
                    {
                        long size = _descendants.values().stream().filter(d -> d).count();
                        count += size;

                        LogHandler.debug("[ChildCombinator] [%s] Selector contains '%d' descendant-combinators that can be replaced by child-combinators", mSelector, size);
                        Selector newW3cSelector = recursiveUpdateSelector(w3cSelector);

                        // call copy constructor to create MSelector replacement
                        MSelector newSelector = new MSelector(newW3cSelector, mSelector);
                        newSelectors.put(mSelector, newSelector);
                        LogHandler.debug("[ChildCombinator] [%s] New MSelector created: '%s', will replace old", mSelector, newSelector);

                        _selectorsTransformed++;
                    }
                }

                // replace in CSS rule
                for(MSelector oldSelector : newSelectors.keySet())
                {
                    mRule.replaceSelector(oldSelector, newSelectors.get(oldSelector));
                }
            }

            LogHandler.info("[ChildCombinator] Removed a total of %d over-qualified descendant-combinators, replaced them by child-combinators", count);
        }

        return cssRules;
    }



    /**
     * Filter pseudo-element from a selector, since it is recognized as a descendant combinator (while it is not in scope of this plug-in)
     * It can only be applied once, at the end of the subject selector sequence (http://www.w3.org/TR/css3-selectors/)
     * Therefore, it only has to be filtered once
     * @return selector without pseudo element
     */
    private static Selector tryFilterPseudoElement(Selector selector)
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
     * of the given node, with the ancestor or sibling declaration of the given selector.
     * We then need to find the right node (either parent or previous sibling), before proceeding with further analysis of the remaining selector parts
     */
    private void recursiveFindDescendants(Selector selector, Node node, MSelector mSelector)
    {
        if(selector instanceof ChildSelectorImpl)
        {
            recursiveFindDescendants(((ChildSelectorImpl) selector).getAncestorSelector(), node.getParentNode(), mSelector);
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
                    found = trySelectNodeWithCss(previousSelector, previousNode, mSelector);
                    previousNode = previousNode.getPreviousSibling();
                }
                while(!found);
            }

            recursiveFindDescendants(previousSelector, previousNode, mSelector);
        }
        else if (selector instanceof DescendantSelector)
        {
            DescendantSelectorImpl dSel = (DescendantSelectorImpl)selector;
            Node parent = node.getParentNode();
            Selector ancestor = dSel.getAncestorSelector();

            LogHandler.debug("[ChildCombinator] [%s] Trying to match direct parent node '%s' of node '%s' with the parent selector '%s' of descendant-selector '%s'", mSelector, parent, node, ancestor, selector);

            boolean atDocumentRoot = false;

            if (trySelectNodeWithCss(ancestor, parent, mSelector))
            {
                if(!_descendants.containsKey(dSel))
                {
                    _descendants.put(dSel, true);
                    LogHandler.debug("[ChildCombinator] [%s] Direct parent node '%s' is selectable by ancestor-part '%s' of descendant-selector '%s', child-combinator MAY be allowed", mSelector, parent, ancestor, selector);
                }
            }
            else
            {
                _descendants.put(dSel, false);
                LogHandler.debug("[ChildCombinator] [%s] Direct parent node '%s' is NOT selectable by ancestor-part '%s' of descendant-selector '%s', child-combinator NOT allowed", mSelector, parent, ancestor, selector);

                // direct parent node is not selectable by ancestor-part of descendant-selector,
                // need to search up in DOM to find the parent that matched the ancestor-part, before continuing
                boolean found = false;
                while(!found)
                {
                    parent = parent.getParentNode();
                    if (parent instanceof Document)
                    {
                        atDocumentRoot = true;
                        LogHandler.warn("[ChildCombinator] [%s] Found document root while trying to find parent DOM element that should be selectable by ancestor '%s' of selector '%s'", mSelector, ancestor, selector);
                        break;
                    }
                    else
                    {
                        found = trySelectNodeWithCss(ancestor, parent, mSelector);
                    }
                }
            }

            if(!atDocumentRoot)
            {
                recursiveFindDescendants(ancestor, parent, mSelector);
            }
        }
    }


    /**
     * Filter method for selecting a node with a css selector
     * Since the caller of this method can pass another chained (e.g. Child, Sibling or Descendant) selector
     * and we are just interested in the 'last' selector part (e.g. the 'a' in div a), we need to find it using the selector type
     * @return true if CSS selector matches given DOM node
     */
    private static boolean trySelectNodeWithCss(Selector selector, Node node, MSelector mSelector)
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

        if (trySelectNodeWithCss(node, selToMatch))
        {
            LogHandler.debug("[ChildCombinator] [%s] Node '%s' is selectable by simple selector '%s' of selector '%s'", mSelector, printNode(node), selToMatch, selector);
            return true;
        }

        LogHandler.debug("[ChildCombinator] [%s] Node '%s' is NOT selectable by simple selector '%s' of selector '%s'", mSelector, printNode(node), selToMatch, selector);
        return false;
    }


    /**
     * Main method to select a given node by a given selector using the node's name (as html tag) and attribute and the selector's type
     * If selector is element-selector, just match by node name
     * If selector contains class or ID, match by node name and attribute equivalence of class and ID
     * If selector contains attribute with or without value, match by node name and attribute name and/or value
     * If selector contains pseudo-class condition, just match by node name
     * @return true if node is selectable by given selector
     */
    private static boolean trySelectNodeWithCss(Node node, Selector selector)
    {
        try
        {
            if (selector instanceof ElementSelectorImpl)
            {
                return matchNodeWithElementSelector(node, selector);
            }
            else if (selector instanceof ConditionalSelectorImpl)
            {
                ConditionalSelectorImpl cSelector = (ConditionalSelectorImpl) selector;

                Selector innerSelector = cSelector.getSimpleSelector();
                Condition innerCondition = cSelector.getCondition();

                return recursiveMatchConditionToNode(innerSelector, innerCondition, node);
            }
            else
            {
                LogHandler.warn("[ChildCombinator] Unsupported: Selector '%s' is no ElementSelector or ConditionalSelector, but a %s", selector, selector.getClass());
            }
        }
        catch(Exception ex)
        {
            LogHandler.error(ex, "[ChildCombinator] Error while matching node to selector for node '%s' and selector '%s'", printNode(node), selector);
        }

        return false;
    }


    /**
     * Recursively match multiple conditions of a selector to a single node
     * Recursive call is executed when selector like '.class1.class2.class3'
     * Otherwise match single condition to single node
     * @return true if given all CSS conditions (ID, class, attribute, substring attribute or pseudo-element)
     */
    private static boolean recursiveMatchConditionToNode(Selector innerSelector, Condition condition, Node node)
    {
        if(condition instanceof AndConditionImpl)
        {
            AndConditionImpl andCondition = (AndConditionImpl)condition;
            return  matchConditionToNode(innerSelector, andCondition.getSecondCondition(), node)
                    && recursiveMatchConditionToNode(innerSelector, andCondition.getFirstCondition(), node);
        }

        return matchConditionToNode(innerSelector, condition, node);
    }


    /**
     * Main method to match a given condition to a given node, by parsing the attributes contained in that node (except for pseudo-elements)
     * Supports IDs, classes, attributes, substring attributes and pseudo-elements
     * @return true if DOM nodes attributes match given CSS condition
     */
    private static boolean matchConditionToNode(Selector innerSelector, Condition innerCondition, Node node)
    {
        if (innerCondition instanceof IdConditionImpl)
        {
            IdConditionImpl idCondition = (IdConditionImpl) innerCondition;
            String attr = getAttributeValue(node.getAttributes(), "id");
            if (matchNodeWithElementSelector(node, innerSelector) && attr != null && attr.equals(idCondition.getValue()))
            {
                return true;
            }
        }
        else if (innerCondition instanceof ClassConditionImpl)
        {
            ClassConditionImpl classCondition = (ClassConditionImpl) innerCondition;
            String attr = getAttributeValue(node.getAttributes(), "class");
            if (matchNodeWithElementSelector(node, innerSelector) && attr != null && findMatchInClass(attr, classCondition.getValue()))
            {
                return true;
            }
        }
        else if (innerCondition instanceof AttributeConditionImpl)
        {
            AttributeConditionImpl attrCondition = (AttributeConditionImpl) innerCondition;
            String attr = getAttributeValue(node.getAttributes(), attrCondition.getLocalName());
            if(matchNodeWithElementSelector(node, innerSelector) && attr != null)
            {
                if(attrCondition.getValue() == null || attrCondition.getValue().isEmpty())
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
            String nodeAttribute = getAttributeValue(node.getAttributes(), selectorAttribute.getLocalName());
            if(matchNodeWithElementSelector(node, innerSelector) && nodeAttribute != null)
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
                return matchNodeWithElementSelector(node, innerSelector);
            }
        }
        else
        {
            LogHandler.warn("[ChildCombinator] Unsupported: ConditionalSelector '%s' is no ID, CLASS or ATTRIBUTE SELECTOR, but a '%s'", innerSelector, innerSelector.getClass());
        }

        return false;
    }


    /**
     * @return true if given node matches the given element-selector (div, body, etc..) by name, or if element-selector equals '*'
     */
    private static boolean matchNodeWithElementSelector(Node node, Selector elementSelector)
    {
        if(elementSelector.toString().equals("*"))
        {
            return true;
        }

        return node.getNodeName().equalsIgnoreCase(elementSelector.toString());
    }


    /**
     * @return the value for the given attribute, or NULL
     */
    private static String getAttributeValue(NamedNodeMap attributes, String attributeName)
    {
        if(attributes == null)
            return null;

        Node node = attributes.getNamedItem(attributeName);

        if(node != null)
            return node.getNodeValue();

        return null;
    }


    /**
     * @return true if a given CSS class text is contained in given  class attribute
     */
    private static boolean findMatchInClass(String classAttribute, String search)
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
     * Update given selector in place and return it
     * Update starting with the first selector, by seeking up using sibling and ancestor operators
     * @return selector with one or more descendants replaced by childs
     */
    private Selector recursiveUpdateSelector(Selector selector)
    {
        if(selector instanceof ChildSelectorImpl)
        {
            ChildSelectorImpl cSel = (ChildSelectorImpl) selector;
            cSel.setAncestorSelector(recursiveUpdateSelector(cSel.getAncestorSelector()));
        }
        else if(selector instanceof SiblingSelector)
        {
            if(selector instanceof DirectAdjacentSelectorImpl)
            {
                DirectAdjacentSelectorImpl sSel = (DirectAdjacentSelectorImpl) selector;
                sSel.setSelector(recursiveUpdateSelector(sSel.getSelector()));
            }
            else
            {
                GeneralAdjacentSelectorImpl sSel = (GeneralAdjacentSelectorImpl) selector;
                sSel.setSelector(recursiveUpdateSelector(sSel.getSelector()));
            }
        }
        else if (selector instanceof DescendantSelectorImpl)
        {
            DescendantSelectorImpl dSel = (DescendantSelectorImpl)selector;
            dSel.setAncestorSelector(recursiveUpdateSelector(dSel.getAncestorSelector()));

            // here we actually replace a given descendant-combinator with a child-combinator
            if(_descendants.containsKey(dSel) && _descendants.get(dSel))
            {
                return new ChildSelectorImpl(dSel.getAncestorSelector(), dSel.getSimpleSelector());
            }
        }

        return selector;
    }


    /**
     * @return simple print of a node in HTML format, including attributes
     */
    private static String printNode(Node node)
    {
        CretStringBuilder builder = new CretStringBuilder();

        if(node instanceof Document)
        {
            return "DOCUMENT ROOT";
        }

        builder.append("<%s", node.getNodeName());

        NamedNodeMap nodeAttributes = node.getAttributes();
        if(nodeAttributes != null)
        {
            for (int i = 0; i < nodeAttributes.getLength(); i++)
            {
                builder.append(" %s=\"%s\"", nodeAttributes.item(i).getNodeName(), nodeAttributes.item(i).getNodeValue());
            }
        }

        builder.append("></%s>", node.getNodeName());

        return builder.toString();
    }
}