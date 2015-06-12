package com.crawljax.plugins.csssuite.sass.variables;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

/**
 * Created by axel on 6/9/2015.
 */
public class SassVariable
{
    private final SassVarType _type;
    private final String _name;
    private final String _value;
    private final MProperty _relatedProperty;

    public SassVariable(SassVarType type, String name, String value, MProperty property)
    {
        _type = type;
        _name = name;
        _value = value;
        _relatedProperty = property;
    }

    public void Print(SuiteStringBuilder builder)
    {
        builder.append("$%s: %s;", _name, _value);
    }

    public SassVarType getVarType()
    {
        return _type;
    }

    @Override
    public String toString()
    {
        return String.format("$%s", _name);
    }
}
