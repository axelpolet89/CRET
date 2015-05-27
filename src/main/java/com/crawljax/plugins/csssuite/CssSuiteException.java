package com.crawljax.plugins.csssuite;

/**
 * Created by axel on 5/27/2015.
 */
public class CssSuiteException extends Exception
{
    public CssSuiteException(String format, Object... args)
    {
        super(String.format(format, VarArgsToArray(args)));
    }

    private static Object[] VarArgsToArray(Object... arguments)
    {
        //need to copy varargs to Object[], otherwise String.format fails
        Object[] args = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 0, arguments.length);
        return args;
    }
}
