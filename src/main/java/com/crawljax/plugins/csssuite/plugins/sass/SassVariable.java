package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

/**
 * Created by axel on 6/9/2015.
 */
public class SassVariable
{
    private final String _name;
    private final String _value;
    private final MProperty _relatedProperty;

    public SassVariable(String name, String value, MProperty property)
    {
        _name = name;
        _value = value;
        _relatedProperty = property;
    }

    public void Print(SuiteStringBuilder builder)
    {
        builder.append("$%s: %s", _name, _value);
    }

    @Override
    public String toString()
    {
        return String.format("$%s", _name);
    }
}
