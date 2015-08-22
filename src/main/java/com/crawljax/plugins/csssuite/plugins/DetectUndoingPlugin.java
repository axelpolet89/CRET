package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssTransformer;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.DefaultStylesHelper;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

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
public class DetectUndoingPlugin implements ICssTransformer
{
    private int _defaultDeclarationsRemoved = 0;
    private int _emptySelectorsRemoved = 0;

    @Override
    public void getStatistics(SuiteStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<DD>%d</DD>", prefix, _defaultDeclarationsRemoved);
        builder.appendLine("%s<DS>%d</DS>", prefix, _emptySelectorsRemoved);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        LogHandler.info("[CssAnalyzer] Performing analysis of invalid undo styles on matched CSS selectors...");

        Map<String, String> defaultStyles = DefaultStylesHelper.CreateDefaultStyles();

        // performance
        Set<Set<MSelector>> processedSets = new HashSet<>();

        for (String keyElement : matchedElements.GetMatchedElements())
        {
            List<MSelector> matchedSelectors = matchedElements.SortSelectorsForMatchedElem(keyElement);
            List<MSelector> effectiveSelectors = matchedSelectors.stream().filter(s -> s.HasEffectiveDeclarations()).collect(Collectors.toList());

            // performance
            if(processedSets.contains(new HashSet<>(effectiveSelectors)))
            {
                LogHandler.debug("Set of effective selectors for element '%s' already processed", keyElement);
                continue;
            }

            for (int i = 0; i < effectiveSelectors.size(); i++)
            {
                MSelector selector = effectiveSelectors.get(i);
                List<MDeclaration> properties = selector.GetDeclarations();

                for (MDeclaration declaration : properties)
                {
                    final String name = declaration.GetName();
                    final String value = declaration.GetValue();
                    final boolean important = declaration.IsImportant();

                    // skip declarations that we do not support yet
                    if(!defaultStyles.containsKey(name))
                    {
                        continue;
                    }

                    final String defaultValue = defaultStyles.get(name);

                    // verify this property is effective and has a default value
                    if (value.equals(defaultValue))
                    {
                        LogHandler.debug("[CssUndoDetector] Found possible undoing property: '%s' with a (default) value '%s' in selector '%s'",
                                name, value, selector);

                        // an important value is always a valid undo for now...
                        boolean validUndo = important;

                        if(!validUndo)
                        {
                            // if this property is allowed to coexist besides another property in the same selector,
                            // even with a 0 value, then it is a valid undo (f.e. border-top-width: 0; besides border-width: 4px;)
                            for (MDeclaration property2 : properties)
                            {
                                if (declaration != property2)
                                {
                                    if (declaration.AllowCoexistence(property2))
                                    {
                                        validUndo = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if(!validUndo)
                        {
                            for (int j = i + 1; j < effectiveSelectors.size(); j++)
                            {
                                MSelector nextSelector = effectiveSelectors.get(j);

                                if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() == 0)
                                {
                                    continue;
                                }

                                if (selector.GetMediaQueries().size() == 0 && nextSelector.GetMediaQueries().size() > 0)
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

                                for (MDeclaration otherProperty : nextSelector.GetDeclarations())
                                {
                                    final String otherName = otherProperty.GetName();
                                    final String otherValue = otherProperty.GetValue();

                                    if (declaration.AllowCoexistence(otherProperty) || otherName.equals(name) && !otherValue.equals(value))
                                    {
                                        // verify whether this property is allowed to co-exist to another property, whatever the value
                                        // verify whether another effective property in a less-specific selector has the same name
                                        // verify that it has a different value
                                        validUndo = true;
                                        LogHandler.debug("[CssUndoDetector] Found an effective property '%s' with a different value '%s' in (less-specific) selector '%s'\n" +
                                                        "that is (correctly) undone by effective property with value '%s' in (more-specific) selector '%s'",
                                                otherProperty.GetName(), otherProperty.GetValue(), nextSelector, declaration.GetValue(), selector);
                                        break;
                                    }
                                }

                                if (validUndo)
                                {
                                    break;
                                }
                            }
                        }

                        declaration.SetInvalidUndo(!validUndo);
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
    private MCssFile FilterUndoRules(MCssFile file)
    {
        for(MCssRule mRule : file.GetRules())
        {
            List<MSelector> emptySelectors = new ArrayList<>();

            for(MSelector mSelector : mRule.GetSelectors())
            {
                for(MDeclaration mDeclaration : mSelector.GetDeclarations())
                {
                    if(mDeclaration.IsInvalidUndo())
                    {
                        LogHandler.debug("[CssUndoDetector] Property %s with value %s in selector %s is an INVALID undo style", mDeclaration.GetName(), mDeclaration.GetValue(), mSelector);
                        _defaultDeclarationsRemoved++;
                    }
                }

                mSelector.RemoveInvalidUndoDeclarations();

                if(!mSelector.HasEffectiveDeclarations())
                {
                    emptySelectors.add(mSelector);
                    _emptySelectorsRemoved++;
                }
            }

            mRule.RemoveSelectors(emptySelectors);
        }

        return file;
    }
}
