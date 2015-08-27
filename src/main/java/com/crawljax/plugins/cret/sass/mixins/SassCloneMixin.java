package com.crawljax.plugins.cret.sass.mixins;

import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/5/2015.
 */
public class SassCloneMixin
{
    private final List<MDeclaration> _declarations;
    private final List<MSelector> _extractedFrom;
    private final Map<MSelector, Map<String, Integer>> _declarationOrdering;
    private int _number;

    public SassCloneMixin()
    {
        _declarations = new ArrayList<>();
        _extractedFrom = new ArrayList<>();
        _declarationOrdering = new HashMap<>();
    }

    /** Getter */
    public List<MDeclaration> getDeclarations()
    {
        return _declarations;
    }

    /** Getter */
    public List<MSelector> getRelatedSelectors()
    {
        return _extractedFrom;
    }

    /** Getter */
    public int getNumber()
    {
        return _number;
    }


    /**
     * Add declaration to be included in this mixin
     */
    public void addDeclaration(MDeclaration mDeclaration)
    {
        _declarations.add(mDeclaration);
    }


    /**
     * Add a selector that will require an @include to this mixin
     * Also retain ordering of the declarations contained in that selector,
     */
    public void addSelector(MSelector mSelector)
    {
        _extractedFrom.add(mSelector);

        // retain original declaration-ordering, to maintain intra-selector semantics
        Map<String, Integer> declarationOrdering = new HashMap<>();
        mSelector.getDeclarations().forEach(p -> declarationOrdering.put(p.getName(), p.getOrder()));
        _declarationOrdering.put(mSelector, declarationOrdering);
    }


    /**
     * Set number that will be used in this mixins name
     */
    public void setNumber(int number)
    {
        _number = number;
    }


    /**
     * Check if given list of selectors is equal to selectors related to this mixin
     * @return
     */
    public boolean sameSelectors(List<MSelector> selectors)
    {
        return _extractedFrom.size() == selectors.size() && selectors.containsAll(_extractedFrom);
    }


    /**
     * @return order of given declaration in given selector, which is require to correctly place an @include in the selector
     */
    public int getDeclarationOrderForSelector(MSelector mSelector, MDeclaration mDeclaration)
    {
        return _declarationOrdering.get(mSelector).get(mDeclaration.getName());
    }


    public void print(CretStringBuilder builder)
    {
        builder.append("@mixin mixin_%d{", _number);
        for(MDeclaration declaration : _declarations)
        {
            builder.appendLine("\t%s", declaration);
        }
        builder.appendLine("}");
    }

    @Override
    public String toString()
    {
        return String.format("mixin_%d", _number);
    }
}
