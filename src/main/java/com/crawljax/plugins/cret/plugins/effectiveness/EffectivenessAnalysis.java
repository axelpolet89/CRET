package com.crawljax.plugins.cret.plugins.effectiveness;

import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;

import java.util.List;

/**
 * Created by axel on 6/24/2015.
 */
public class EffectivenessAnalysis
{
    public static void computeEffectiveness(List<MSelector> selectors, String randomString)
    {
        String overridden = randomString;

        for (int i = 0; i < selectors.size(); i++)
        {
            MSelector selector = selectors.get(i);
            for (MDeclaration declaration : selector.getDeclarations())
            {
                // find out if declaration was already deemed effective previously
                boolean alreadyEffective = declaration.isEffective();

                if (!declaration.getStatus().equals(overridden))
                {
                    declaration.setEffective(true);

                    for (int j = i + 1; j < selectors.size(); j++)
                    {
                        boolean pseudoCompare = false;
                        boolean mediaCompare = false;

                        MSelector nextSelector = selectors.get(j);

                        // a regular selector is less-specific, but will still apply
                        if (selector.getMediaQueries().size() > 0 && nextSelector.getMediaQueries().size() == 0)
                        {
                            continue;
                        }

                        // a regular selector is more specific, but the media-query selector may contain !important statements,
                        // however, regular will also apply
                        if (selector.getMediaQueries().size() == 0 && nextSelector.getMediaQueries().size() > 0)
                        {
                            mediaCompare = true;
                        }

                        // both selectors have different media-queries
                        if (selector.getMediaQueries().size() > 0 && nextSelector.getMediaQueries().size() > 0
                                && !selector.HasEqualMediaQueries(nextSelector))
                        {
                            continue;
                        }

                        // when 'this' selector includes a pseudo-element (as selector-key),
                        // it is always effective and does not affect other selectors, so we can break
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
                                pseudoCompare = true;
                            }
                        }

                        if (pseudoCompare)
                        {
                            compareDeclarationsPs(declaration, nextSelector, overridden, alreadyEffective);
                        }
                        else if (mediaCompare)
                        {
                            compareDeclarationsMq(declaration, nextSelector, overridden);
                        }
                        else
                        {
                            // by default: if both selectors apply under the same condition, simply check matching declaration names
                            // otherwise, the only way for next selector to be ineffective is too have same declaration name AND value
                            compareDeclarations(declaration, nextSelector, overridden, alreadyEffective);
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
    private static void compareDeclarations(MDeclaration declaration, MSelector otherSelector, String overridden, boolean alreadyEffective)
    {
        for (MDeclaration nextDeclaration : otherSelector.getDeclarations())
        {
            if (declaration.getName().equalsIgnoreCase(nextDeclaration.getName()))
            {
                // it is possible, due to specificity ordering, that 'this' declaration was already deemed effective,
                // but a less specific ('next') selector contained an !important declaration
                // this declaration should not be !important or not previously deemed effective
                if(!alreadyEffective && nextDeclaration.isImportant() && !declaration.isImportant())
                {
                    declaration.setStatus(overridden);
                    declaration.setEffective(false);
                }
                else
                {
                    nextDeclaration.setStatus(overridden);
                }
            }
        }
    }


    /**
     * Compare declarations of a (less specific) selector with a given declaration on their name AND value
     * set the other (less specific) declarations overridden or set 'this' declaration overridden due to !important
     * @param declaration
     * @param otherSelector
     * @param overridden
     */
    private static void compareDeclarationsPs(MDeclaration declaration, MSelector otherSelector, String overridden, boolean alreadyEffective)
    {
        for (MDeclaration nextDeclaration : otherSelector.getDeclarations())
        {
            if (declaration.getName().equalsIgnoreCase(nextDeclaration.getName())
                    && declaration.getValue().equalsIgnoreCase(nextDeclaration.getValue()))
            {
                // it is possible, due to specificity ordering, that 'this' declaration was already deemed effective,
                // but a less specific ('next') selector contained an !important declaration
                // this declaration should not be !important or not previously deemed effective
                if(!alreadyEffective && nextDeclaration.isImportant() && !declaration.isImportant())
                {
                    declaration.setStatus(overridden);
                    declaration.setEffective(false);
                }
                else
                {
                    nextDeclaration.setStatus(overridden);
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
    private static void compareDeclarationsMq(MDeclaration declaration, MSelector otherSelector, String overridden)
    {
        for (MDeclaration nextDeclaration : otherSelector.getDeclarations())
        {
            if (declaration.getName().equalsIgnoreCase(nextDeclaration.getName()))
            {
                if(!nextDeclaration.isImportant() || declaration.isImportant())
                {
                    nextDeclaration.setStatus(overridden);
                }
            }
        }
    }
}