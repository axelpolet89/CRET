package com.crawljax.plugins.cret.sass.mixins;

import com.crawljax.plugins.cret.data.MSelector;
import com.crawljax.plugins.cret.data.declarations.MDeclaration;
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

    public void addProperty(MDeclaration mDeclaration)
    {
        _declarations.add(mDeclaration);
    }

    public void addSelector(MSelector mSelector)
    {
        _extractedFrom.add(mSelector);

        // retain original declaration-ordering, to maintain intra-selector semantics
        Map<String, Integer> declarationOrdering = new HashMap<>();
        mSelector.getDeclarations().forEach(p -> declarationOrdering.put(p.getName(), p.getOrder()));
        _declarationOrdering.put(mSelector, declarationOrdering);
    }

    public void setNumber(int number)
    {
        _number = number;
    }

    public boolean sameSelectors(List<MSelector> selectors)
    {
        return _extractedFrom.size() == selectors.size() && selectors.containsAll(_extractedFrom);
    }

    public List<MDeclaration> getDeclarations()
    {
        return _declarations;
    }

    public List<MSelector> getRelatedSelectors()
    {
        return _extractedFrom;
    }

    public int getDeclarationOrderForSelector(MSelector mSelector, MDeclaration mDeclaration)
    {
        return _declarationOrdering.get(mSelector).get(mDeclaration.getName());
    }

    public int getNumber()
    {
        return _number;
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
