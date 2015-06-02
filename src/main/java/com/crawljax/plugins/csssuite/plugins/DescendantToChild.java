package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import com.steadystate.css.parser.selectors.*;

import org.w3c.css.sac.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/2/2015.
 */
public class DescendantToChild implements ICssPostCrawlPlugin
{
    private final CSSOMParser _parser;

    public DescendantToChild()
    {
        _parser = new CSSOMParser(new SACParserCSS3());
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
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    Selector w3cSelector = mSelector.GetW3cSelector();
                    List<Selector> descendants = new ArrayList<>();

                    RecursiveCheck(w3cSelector, descendants);

                    if(descendants.size() == 2)
                    {
                        LogHandler.debug("[DescToChild] [%s] Selector sequence may transformed using a child-combinator instead of a descendant-combinator", mSelector);

                        List<ElementWrapper> matchedElements = mSelector.GetMatchedElements();

                        boolean allowChild = true;

                        for (ElementWrapper ew : matchedElements)
                        {
                            Element element = ew.GetElement();

                            // get direct parent of matched element
                            Node parentNode = element.getParentNode();

                            // get previous selector in sequence
                            Selector prev = descendants.get(1);

                            LogHandler.debug("[DescToChild] [%s] Try match direct parent node '%s' with the parent selector '%s'", mSelector, parentNode, prev);

                            if (!TrySelectNode(element.getParentNode(), descendants.get(1)))
                            {
                                allowChild = false;
                                LogHandler.debug("[DescToChild] [%s] Direct parent node '%s' can not be selected by selector '%s', direct child not allowed", mSelector, PrintNode(parentNode), prev);
                                break;
                            }
                            else
                            {
                                LogHandler.debug("[DescToChild] [%s] Direct parent node '%s' is selectable by selector '%s', direct child may be allowed", mSelector, PrintNode(parentNode), prev);
                            }
                        }

                        if(allowChild)
                        {
                            LogHandler.debug("[DescToChild] [%s] Selector sequence can be replaced by a child selector, because all matched elements adhere to direct-child relation", mSelector);

                            Selector newW3cSelector = CreateW3cSelector(descendants);
                            if(newW3cSelector == null)
                            {
                                LogHandler.warn("[DescToChild] [%s] Error while creating new selector, skip");
                                continue;
                            }

                            MSelector newSelector = new MSelector(newW3cSelector, mSelector.GetProperties(), mSelector.GetRuleNumber(), mSelector.GetMediaQueries());
                            LogHandler.debug("[DescToChild] [%s] New selector created: '%s'", mSelector, newSelector);

                            mRule.ReplaceSelector(mSelector, newSelector);
                            count++;
                        }
                    }
                }
            }

            LogHandler.info("[DescToChild] Removed %d over-qualified descendant-combinators, replaced by child-combinators", count);
        }

        return cssRules;
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
                if (node.getNodeName().equalsIgnoreCase(((ElementSelectorImpl) selector).getLocalName())) ;
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
                    if (attr != null && attr.equals(classCondition.getValue()))
                    {
                        return true;
                    }
                }
                else if (condition instanceof AttributeConditionImpl)
                {
                    AttributeConditionImpl attrCondition = (AttributeConditionImpl) condition;
                    String attr = GetAttributeValue(node.getAttributes(), attrCondition.getLocalName());
                    if (attr != null && attr.equals(attrCondition.getValue()))
                    {
                        return true;
                    }
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
     * @param selector
     * @param descendants
     */
    private void RecursiveCheck(Selector selector, List<Selector> descendants)
    {
        if(descendants.size() > 2)
            return;

        if(selector instanceof PseudoElementSelectorImpl)
        {
            descendants.add(selector);
        }
        else if(selector instanceof SimpleSelector)
        {
            descendants.add(selector);
        }
        else if (selector instanceof DescendantSelector && !(selector instanceof ChildSelectorImpl))
        {
            DescendantSelector dSelector = (DescendantSelector)selector;
            RecursiveCheck(dSelector.getSimpleSelector(), descendants);
            RecursiveCheck(dSelector.getAncestorSelector(), descendants);
        }
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
     * @param descendants
     * @return
     */
    private Selector CreateW3cSelector(List<Selector> descendants)
    {
        InputSource inputSource = new InputSource(new StringReader(descendants.get(1) + " > " + descendants.get(0) + "{}"));
        try
        {
            CSSStyleRuleImpl rule = (CSSStyleRuleImpl)_parser.parseRule(inputSource);
            return rule.getSelectors().item(0);
        }
        catch (Exception ex)
        {
            LogHandler.error(ex, "Cannot create new selector using '%s' and '%s'", descendants.get(1), descendants.get(0));
        }

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
        for(int i = 0; i < map.getLength(); i++)
        {
            builder.append(" %s=\"%s\"", map.item(i).getNodeName(), map.item(i).getNodeValue());
        }

        builder.append("></%s>", node.getNodeName());

        return builder.toString();
    }
}
