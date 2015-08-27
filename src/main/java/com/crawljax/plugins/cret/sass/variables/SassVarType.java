package com.crawljax.plugins.cret.sass.variables;

/**
 * Created by axel on 6/10/2015.
 */
public enum SassVarType
{
    COLOR("1"),
    ALPHA_COLOR("2"),
    URL("3"),
    FONT("4");

    private final String _stringRepresentation;

    SassVarType(String value)
    {
        _stringRepresentation = value;
    }

    @Override
    public String toString()
    {
        return _stringRepresentation;
    }
}
