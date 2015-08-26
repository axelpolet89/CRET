package com.crawljax.plugins.cret.util;

/**
 * Created by axel on 5/19/2015.
 * Extension of the StringBuilder, by providing a appendLine() function
 */
public class CretStringBuilder
{
    private final StringBuilder _builder;

    public CretStringBuilder()
    {
        _builder = new StringBuilder();
    }

    public void append(String string)
    {
        _builder.append(string);
    }

    public void appendLine(String string)
    {
        _builder.append("\n" + string);
    }

    public void append(String format, Object... arguments)
    {
        _builder.append(String.format(format, varArgsToArray(arguments)));
    }

    public void appendLine(String format, Object... arguments)
    {
        _builder.append("\n" + String.format(format, varArgsToArray(arguments)));
    }

    private Object[] varArgsToArray(Object... arguments)
    {
        //need to copy varargs to Object[], otherwise String.format fails
        Object[] args = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 0, arguments.length);
        return args;
    }

    @Override
    public String toString()
    {
        return _builder.toString();
    }
}
