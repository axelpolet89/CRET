package com.crawljax.plugins.cret.transformation;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.interfaces.ICssTransformer;
import com.crawljax.plugins.cret.transformation.matcher.MatchedElements;
import com.crawljax.plugins.cret.util.DefaultStylesHelper;
import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/2/2015.
 *
 * Responsible for finding invalid undo styles. A declaration is defined to be an invalid undo style if:
 * the declaration's value is a default value for that declaration
 * the declaration is effective
 * the declaration overrides no other effective declaration
 */
public class DefaultStylesPlugin implements ICssTransformer
{
    private int _defaultDeclarationsRemoved = 0;
    private int _emptySelectorsRemoved = 0;

    @Override
    public void getStatistics(CretStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<DD>%d</DD>", prefix, _defaultDeclarationsRemoved);
        builder.appendLine("%s<DS>%d</DS>", prefix, _emptySelectorsRemoved);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        LogHandler.info("[DefaultStyles] Performing analysis of invalid undo styles on matched CSS selectors...");

        Map<String, String> defaultStyles = DefaultStylesHelper.createDefaultStyles();

        // performance
        Set<Set<MSelector>> processedSets = new HashSet<>();

        for (String keyElement : matchedElements.getMatchedElements())
        {
            List<MSelector> matchedSelectors = matchedElements.sortSelectorsForMatchedElem(keyElement);
            List<MSelector> effectiveSelectors = matchedSelectors.stream().filter(s -> s.hasEffectiveDeclarations()).collect(Collectors.toList());

            // performance
            if(processedSets.contains(new HashSet<>(effectiveSelectors)))
            {
                LogHandler.debug("Set of effective selectors for element '%s' already processed", keyElement);
                continue;
            }

            for (int i = 0; i < effectiveSelectors.size(); i++)
            {
                MSelector selector = effectiveSelectors.get(i);
                List<MDeclaration> declarations = selector.getDeclarations();

                for (MDeclaration declaration : declarations)
                {
                    final String name = declaration.getName();
                    final String value = declaration.getValue();
                    final boolean important = declaration.isImportant();

                    // skip declarations that we do not support yet
                    if(!defaultStyles.containsKey(name))
                    {
                        continue;
                    }

                    final String defaultValue = defaultStyles.get(name);

                    // verify this declaration is effective and has a default value
                    if (value.equals(defaultValue))
                    {
                        LogHandler.debug("[DefaultStyles] Found possible undoing declaration: '%s' with a (default) value '%s' in selector '%s'",
                                name, value, selector);

                        // an important value is always a valid undo for now...
                        boolean validUndo = important;

                        if(!validUndo)
                        {
                            // if this declaration is allowed to coexist besides another declaration in the same selector,
                            // even with a 0 value, then it is a valid undo (f.e. border-top-width: 0; besides border-width: 4px;)
                            for (MDeclaration property2 : declarations)
                            {
                                if (declaration != property2)
                                {
                                    if (declaration.allowCoexistence(property2))
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

                                if (selector.getMediaQueries().size() > 0 && nextSelector.getMediaQueries().size() == 0)
                                {
                                    continue;
                                }

                                if (selector.getMediaQueries().size() == 0 && nextSelector.getMediaQueries().size() > 0)
                                {
                                    continue;
                                }

                                if (selector.getMediaQueries().size() > 0 && nextSelector.getMediaQueries().size() > 0
                                        && !selector.hasEqualMediaQueries(nextSelector))
                                {
                                    continue;
                                }

                                if (selector.hasPseudoElement() || nextSelector.hasPseudoElement())
                                {
                                    if (!selector.hasEqualPseudoElement(nextSelector))
                                    {
                                        continue;
                                    }
                                }

                                if (selector.isNonStructuralPseudo() || nextSelector.isNonStructuralPseudo())
                                {
                                    if (!selector.hasEqualPseudoClass(nextSelector))
                                    {
                                        continue;
                                    }
                                }

                                for (MDeclaration otherProperty : nextSelector.getDeclarations())
                                {
                                    final String otherName = otherProperty.getName();
                                    final String otherValue = otherProperty.getValue();

                                    if (declaration.allowCoexistence(otherProperty) || otherName.equals(name) && !otherValue.equals(value))
                                    {
                                        // verify whether this declaration is allowed to co-exist to another declaration, whatever the value
                                        // verify whether another effective declaration in a less-specific selector has the same name
                                        // verify that it has a different value
                                        validUndo = true;
                                        LogHandler.debug("[DefaultStyles] Found an effective declaration '%s' with a different value '%s' in (less-specific) selector '%s'\n" +
                                                        "that is (correctly) undone by effective declaration with value '%s' in (more-specific) selector '%s'",
                                                otherProperty.getName(), otherProperty.getValue(), nextSelector, declaration.getValue(), selector);
                                        break;
                                    }
                                }

                                if (validUndo)
                                {
                                    break;
                                }
                            }
                        }

                        declaration.setInvalidUndo(!validUndo);
                    }
                }
            }

            // performance
            processedSets.add(new HashSet<>(effectiveSelectors));
        }

        for(MCssFile file : cssRules.values())
        {
            filterUndoRules(file);
        }

        return cssRules;
    }


    /**
     * Filter all declarations that perform an invalid undo from given CSS file
     */
    private MCssFile filterUndoRules(MCssFile file)
    {
        for(MCssRule mRule : file.getRules())
        {
            List<MSelector> emptySelectors = new ArrayList<>();

            for(MSelector mSelector : mRule.getSelectors())
            {
                for(MDeclaration mDeclaration : mSelector.getDeclarations())
                {
                    if(mDeclaration.isInvalidUndo())
                    {
                        LogHandler.debug("[DefaultStyles] Declaration %s with value %s in selector %s is an INVALID undo style", mDeclaration.getName(), mDeclaration.getValue(), mSelector);
                        _defaultDeclarationsRemoved++;
                    }
                }

                mSelector.removeInvalidUndoDeclarations();

                if(!mSelector.hasEffectiveDeclarations())
                {
                    emptySelectors.add(mSelector);
                    _emptySelectorsRemoved++;
                }
            }

            mRule.removeSelectors(emptySelectors);
        }

        return file;
    }
}
