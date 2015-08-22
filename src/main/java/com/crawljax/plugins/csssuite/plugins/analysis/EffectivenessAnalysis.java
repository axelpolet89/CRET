package com.crawljax.plugins.csssuite.plugins.analysis;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;

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
            for (MDeclaration declaration : selector.GetDeclarations())
            {
                // find out if declaration was already deemed effective previously
                boolean alreadyEffective = declaration.IsEffective();

                if (!declaration.GetStatus().equals(overridden))
                {
                    declaration.SetEffective(true);

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
                            CompareDeclarationsPs(declaration, nextSelector, overridden, alreadyEffective);
                        }
                        else if (mediaCompare)
                        {
                            CompareDeclarationsMq(declaration, nextSelector, overridden);
                        }
                        else
                        {
                            // by default: if both selectors apply under the same condition, simply check matching declaration names
                            // otherwise, the only way for next selector to be ineffective is too have same declaration name AND value
                            CompareDeclarations(declaration, nextSelector, overridden, alreadyEffective);
                        }
                    }
                }
            }
        }
    }


    /**
     * Compare declarations of a (less specific) selector with a given declaration on ONLY their name
     * set the other (less specific) declarations overridden or set 'this' declaration overridden due to !important
     * @param declaration
     * @param otherSelector
     * @param overridden
     */
    private static Void CompareDeclarations(MDeclaration declaration, MSelector otherSelector, String overridden, boolean alreadyEffective)
    {
        for (MDeclaration nextDeclaration : otherSelector.GetDeclarations())
        {
            if (declaration.GetName().equalsIgnoreCase(nextDeclaration.GetName()))
            {
                // it is possible, due to specificity ordering, that 'this' declaration was already deemed effective,
                // but a less specific ('next') selector contained an !important declaration
                // this declaration should not be !important or not previously deemed effective
                if(!alreadyEffective && nextDeclaration.IsImportant() && !declaration.IsImportant())
                {
                    declaration.SetStatus(overridden);
                    declaration.SetEffective(false);
                }
                else
                {
                    nextDeclaration.SetStatus(overridden);
                }
            }
        }
        return null;
    }


    /**
     * Compare declarations of a (less specific) selector with a given declaration on their name AND value
     * set the other (less specific) declarations overridden or set 'this' declaration overridden due to !important
     * @param declaration
     * @param otherSelector
     * @param overridden
     */
    private static void CompareDeclarationsPs(MDeclaration declaration, MSelector otherSelector, String overridden, boolean alreadyEffective)
    {
        for (MDeclaration nextDeclaration : otherSelector.GetDeclarations())
        {
            if (declaration.GetName().equalsIgnoreCase(nextDeclaration.GetName())
                    && declaration.GetValue().equalsIgnoreCase(nextDeclaration.GetValue()))
            {
                // it is possible, due to specificity ordering, that 'this' declaration was already deemed effective,
                // but a less specific ('next') selector contained an !important declaration
                // this declaration should not be !important or not previously deemed effective
                if(!alreadyEffective && nextDeclaration.IsImportant() && !declaration.IsImportant())
                {
                    declaration.SetStatus(overridden);
                    declaration.SetEffective(false);
                }
                else
                {
                    nextDeclaration.SetStatus(overridden);
                }
            }
        }
    }


    /**
     * Compare declarations of a (less specific) selector with a given declaration on their name
     * and the absence of the !important statement in the less-specific declaration OR presence in the more-specific declaration
     * @param declaration
     * @param otherSelector
     * @param overridden
     */
    private static void CompareDeclarationsMq(MDeclaration declaration, MSelector otherSelector, String overridden)
    {
        for (MDeclaration nextDeclaration : otherSelector.GetDeclarations())
        {
            if (declaration.GetName().equalsIgnoreCase(nextDeclaration.GetName()))
            {
                if(!nextDeclaration.IsImportant() || declaration.IsImportant())
                {
                    nextDeclaration.SetStatus(overridden);
                }
            }
        }
    }
}
