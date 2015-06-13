package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/2/2015.
 *
 * Responsible for finding invalid undo styles. A property is defind to be an invalid undo style if:
 * the property's value is a default value for that property
 * the property is effective
 * the property overrides no other effective property
 */
public class DetectUndoingPlugin implements ICssPostCrawlPlugin
{
    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {
        LogHandler.info("[CssAnalyzer] Performing analysis of invalid undo styles on matched CSS selectors...");

        Map<String, String> defaultStyles = CreateDefaultStyles();

        // performance
        Set<Set<MSelector>> processedSets = new HashSet<>();

        for (String keyElement : MatchedElements.GetMatchedElements())
        {
            List<MSelector> matchedSelectors = MatchedElements.SortSelectorsForMatchedElem(keyElement);
            List<MSelector> effectiveSelectors = matchedSelectors.stream().filter(s -> s.HasEffectiveProperties()).collect(Collectors.toList());

            // performance
            if(processedSets.contains(new HashSet<>(effectiveSelectors)))
            {
                LogHandler.debug("Set of effective selectors for element '%s' already processed", keyElement);
                continue;
            }

            for (int i = 0; i < effectiveSelectors.size(); i++)
            {
                MSelector selector = effectiveSelectors.get(i);
                List<MProperty> properties = selector.GetProperties();

                for (MProperty property : properties)
                {
                    final String name = property.GetName();
                    final String value = property.GetValue();

                    // skip declarations that we do not check for default values
                    if(!defaultStyles.containsKey(name))
                    {
                        continue;
                    }

                    final String defaultValue = defaultStyles.get(name);

                    // verify this property is effective and has a default value
                    if (property.IsEffective() && value.equals(defaultValue))
                    {
                        LogHandler.debug("[CssUndoDetector] Found possible undoing property: '%s' with a (default) value '%s' in selector '%s'",
                                name, value, selector);

                        boolean validUndo = false;

                        // if this property is allowed to coexist besides another property in the same selector,
                        // even with a 0 value, then it is a valid undo (f.e. border-top-width: 0; besides border-width: 4px;)
                        for(MProperty property2 : properties)
                        {
                            if(property != property2)
                            {
                                if(property.AllowCoexistence(property2))
                                {
                                    validUndo = true;
                                    break;
                                }
                            }
                        }

                        if(validUndo)
                        {
                            break;
                        }

                        for (int j = i + 1; j < effectiveSelectors.size(); j++)
                        {
                            MSelector nextSelector = effectiveSelectors.get(j);

                            if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() == 0)
                            {
                                continue;
                            }

                            if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() > 0
                                    && !selector.HasEqualMediaQueries(nextSelector))
                            {
                                continue;
                            }

                            if (selector.HasPseudoElement() || nextSelector.HasPseudoElement())
                            {
                                if (!selector.HasEqualPseudoElement(nextSelector))
                                {
                                    continue;
                                }
                            }

                            if (selector.IsNonStructuralPseudo() || nextSelector.IsNonStructuralPseudo())
                            {
                                if (!selector.HasEqualPseudoClass(nextSelector))
                                {
                                    continue;
                                }
                            }

                            for (MProperty otherProperty : nextSelector.GetProperties())
                            {
                                final String otherName = otherProperty.GetName();
                                final String otherValue = otherProperty.GetValue();

                                // verify whether this property is allowed to co-exist to another property, whatever the value
                                // verify whether another effective property in a less-specific selector has the same name
                                // verify that it has a different value
                                if (property.AllowCoexistence(otherProperty) || (otherProperty.IsEffective() && otherName.equals(name) && !otherValue.equals(value)))
                                {
                                    validUndo = true;
                                    LogHandler.debug("[CssUndoDetector] Found an effective property '%s' with a different value '%s' in (less-specific) selector '%s'\n" +
                                                    "that is (correctly) undone by effective property with value '%s' in (more-specific) selector '%s'",
                                            otherProperty.GetName(), otherProperty.GetValue(), nextSelector, property.GetValue(), selector);
                                    break;
                                }
                            }

                            if(validUndo)
                            {
                                break;
                            }
                        }

                        if(!validUndo)
                        {
                            property.SetInvalidUndo();
                        }
                    }
                }
            }

            // performance
            processedSets.add(new HashSet<>(effectiveSelectors));
        }

        for(MCssFile file : cssRules.values())
        {
            FilterUndoRules(file);
        }

        return cssRules;
    }


    /**
     * Filter all properties that perform an invalid undo
     * @param file
     * @return
     */
    private static MCssFile FilterUndoRules(MCssFile file)
    {
        for(MCssRule mRule : file.GetRules())
        {
            List<MSelector> emptySelectors = new ArrayList<>();

            for(MSelector mSelector : mRule.GetSelectors())
            {
                for(MProperty mProperty : mSelector.GetProperties())
                {
                    if(mProperty.IsInvalidUndo())
                    {
                        LogHandler.debug("[CssUndoDetector] Property %s with value %s in selector %s is an INVALID undo style", mProperty.GetName(), mProperty.GetValue(), mSelector);
                    }
                }

                mSelector.RemoveInvalidUndoProperties();

                if(!mSelector.HasEffectiveProperties())
                {
                    emptySelectors.add(mSelector);
                }
            }

            mRule.RemoveSelectors(emptySelectors);
        }

        return file;
    }


    /**
     *
     * @return
     */
    private static Map<String, String> CreateDefaultStyles()
    {
        Map<String, String> defaultStyles = new HashMap<>();

        defaultStyles.put("width", "0");
        defaultStyles.put("min-width", "0");
        defaultStyles.put("max-width", "none");
        defaultStyles.put("height", "0");
        defaultStyles.put("min-height", "0");
        defaultStyles.put("max-height", "none");

        SetSeparateStyles("padding-%s", "0", defaultStyles);
        SetSeparateStyles("margin-%s", "0", defaultStyles);

        SetSeparateStyles("border-%s-width", "0", defaultStyles);
        SetSeparateStyles("border-%s-style", "none", defaultStyles);
        defaultStyles.put("border-top-left-radius", "0");
        defaultStyles.put("border-top-right-radius", "0");
        defaultStyles.put("border-bottom-right-radius", "0");
        defaultStyles.put("border-bottom-left-radius", "0");

        defaultStyles.put("outline-width", "0");
        defaultStyles.put("outline-style", "none");

        defaultStyles.put("background-image", "none");
        defaultStyles.put("background-color", "transparent");
        defaultStyles.put("background-repeat", "repeat");
        defaultStyles.put("background-position", "0% 0%");
        defaultStyles.put("background-attachment", "scroll");
        defaultStyles.put("background-size", "auto");
        defaultStyles.put("background-clip", "border-box");
        defaultStyles.put("background-origin", "padding-box");

        return defaultStyles;
    }


    /**
     *
     * @param formatter
     * @param value
     * @param styles
     */
    private static void SetSeparateStyles(String formatter, String value, Map<String, String> styles)
    {
        styles.put(String.format(formatter, "top"), value);
        styles.put(String.format(formatter, "right"), value);
        styles.put(String.format(formatter, "bottom"), value);
        styles.put(String.format(formatter, "left"), value);
    }
}
