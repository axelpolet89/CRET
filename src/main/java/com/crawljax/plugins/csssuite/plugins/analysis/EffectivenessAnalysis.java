package com.crawljax.plugins.csssuite.plugins.analysis;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.List;

/**
 * Created by axel on 6/24/2015.
 */
public class EffectivenessAnalysis
{
    public static void ComputeEffectiveness(List<MSelector> selectors, String randomString)
    {
        String overridden = randomString;

        for (int i = 0; i < selectors.size(); i++)
        {
            MSelector selector = selectors.get(i);
            for (MProperty property : selector.GetProperties())
            {
                // find out if property was already deemed effective previously
                boolean alreadyEffective = property.IsEffective();

                if (!property.GetStatus().equals(overridden))
                {
                    property.SetEffective(true);

                    for (int j = i + 1; j < selectors.size(); j++)
                    {
                        boolean pseudoCompare = false;
                        boolean mediaCompare = false;

                        MSelector nextSelector = selectors.get(j);

                        // a regular selector is less-specific, but will still apply
                        if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() == 0)
                        {
                            continue;
                        }

                        // a regular selector is more specific, but the media-query selector may contain !important statements,
                        // however, regular will also apply
                        if (selector.GetMediaQueries().size() == 0 && nextSelector.GetMediaQueries().size() > 0)
                        {
                            mediaCompare = true;
                        }

                        // both selectors have different media-queries
                        if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() > 0
                                && !selector.HasEqualMediaQueries(nextSelector))
                        {
                            continue;
                        }

                        // when 'this' selector includes a pseudo-element (as selector-key),
                        // it is always effective and does not affect other selectors, so we can break
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
                                pseudoCompare = true;
                            }
                        }

                        if (pseudoCompare)
                        {
                            CompareDeclarationsPs(property, nextSelector, overridden, alreadyEffective);
                        }
                        else if (mediaCompare)
                        {
                            CompareDeclarationsMq(property, nextSelector, overridden);
                        }
                        else
                        {
                            // by default: if both selectors apply under the same condition, simply check matching property names
                            // otherwise, the only way for next selector to be ineffective is too have same property name AND value
                            CompareDeclarations(property, nextSelector, overridden, alreadyEffective);
                        }
                    }
                }
            }
        }
    }


    /**
     * Compare properties of a (less specific) selector with a given property on ONLY their name
     * set the other (less specific) properties overridden or set 'this' property overridden due to !important
     * @param property
     * @param otherSelector
     * @param overridden
     */
    private static Void CompareDeclarations(MProperty property, MSelector otherSelector, String overridden, boolean alreadyEffective)
    {
        for (MProperty nextProperty : otherSelector.GetProperties())
        {
            if (property.GetName().equalsIgnoreCase(nextProperty.GetName()))
            {
                // it is possible, due to specificity ordering, that 'this' property was already deemed effective,
                // but a less specific ('next') selector contained an !important declaration
                // this property should not be !important or not previously deemed effective
                if(!alreadyEffective && nextProperty.IsImportant() && !property.IsImportant())
                {
                    property.SetStatus(overridden);
                    property.SetEffective(false);
                }
                else
                {
                    nextProperty.SetStatus(overridden);
                }
            }
        }
        return null;
    }


    /**
     * Compare properties of a (less specific) selector with a given property on their name AND value
     * set the other (less specific) properties overridden or set 'this' property overridden due to !important
     * @param property
     * @param otherSelector
     * @param overridden
     */
    private static void CompareDeclarationsPs(MProperty property, MSelector otherSelector, String overridden, boolean alreadyEffective)
    {
        for (MProperty nextProperty : otherSelector.GetProperties())
        {
            if (property.GetName().equalsIgnoreCase(nextProperty.GetName())
                    && property.GetValue().equalsIgnoreCase(nextProperty.GetValue()))
            {
                // it is possible, due to specificity ordering, that 'this' property was already deemed effective,
                // but a less specific ('next') selector contained an !important declaration
                // this property should not be !important or not previously deemed effective
                if(!alreadyEffective && nextProperty.IsImportant() && !property.IsImportant())
                {
                    property.SetStatus(overridden);
                    property.SetEffective(false);
                }
                else
                {
                    nextProperty.SetStatus(overridden);
                }
            }
        }
    }


    /**
     * Compare properties of a (less specific) selector with a given property on their name
     * and the absence of the !important statement in the less-specific property OR presence in the more-specific property
     * @param property
     * @param otherSelector
     * @param overridden
     */
    private static void CompareDeclarationsMq(MProperty property, MSelector otherSelector, String overridden)
    {
        for (MProperty nextProperty : otherSelector.GetProperties())
        {
            if (property.GetName().equalsIgnoreCase(nextProperty.GetName()))
            {
                if(!nextProperty.IsImportant() || property.IsImportant())
                {
                    nextProperty.SetStatus(overridden);
                }
            }
        }
    }
}
