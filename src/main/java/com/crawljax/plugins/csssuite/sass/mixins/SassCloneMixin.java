package com.crawljax.plugins.csssuite.sass.mixins;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/5/2015.
 */
public class SassCloneMixin
{
    private final List<MProperty> _properties;
    private final List<MSelector> _extractedFrom;
    private int _number;

    public SassCloneMixin()
    {
        _properties = new ArrayList<>();
        _extractedFrom = new ArrayList<>();
    }

    public void addProperty(MProperty mProperty)
    {
        _properties.add(mProperty);
    }

    public void addSelector(MSelector mSelector)
    {
        _extractedFrom.add(mSelector);
    }

    public void SetNumber(int number)
    {
        _number = number;
    }

    public boolean sameSelectors(List<MSelector> selectors)
    {
        return _extractedFrom.size() == selectors.size() && selectors.containsAll(_extractedFrom);
    }

    public List<MProperty> GetProperties()
    {
        return _properties;
    }

    public List<MSelector> GetRelatedSelectors()
    {
        return _extractedFrom;
    }

    public int GetNumber()
    {
        return _number;
    }

    public void Print(SuiteStringBuilder builder)
    {
        builder.append("@mixin mixin_%d{", _number);
        for(MProperty property : _properties)
        {
            builder.appendLine("\t%s", property);
        }
        builder.appendLine("}");
    }

    @Override
    public String toString()
    {
        return String.format("mixin_%d", _number);
    }
}
