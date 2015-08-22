package com.crawljax.plugins.csssuite.sass.variables;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

/**
 * Created by axel on 6/9/2015.
 */
public class SassVariable
{
    private final SassVarType _type;
    private final String _name;
    private final String _value;

    public SassVariable(SassVarType type, String name, String value)
    {
        _type = type;
        _name = name;
        _value = value;
    }

    public void Print(SuiteStringBuilder builder)
    {
        builder.append("$%s: %s;", _name, _value);
    }

    public SassVarType getVarType()
    {
        return _type;
    }

    public String getValue() { return _value; }

    @Override
    public String toString()
    {
        return String.format("$%s", _name);
    }
}
