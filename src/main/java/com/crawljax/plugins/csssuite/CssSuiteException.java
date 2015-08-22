package com.crawljax.plugins.csssuite;

/**
 * Created by axel on 5/27/2015.
 */
public class CssSuiteException extends Exception
{
    public CssSuiteException(String format, Object... args)
    {
        super(String.format(format, varArgsToArray(args)));
    }

    private static Object[] varArgsToArray(Object... arguments)
    {
        //need to copy varargs to Object[], otherwise String.format fails
        Object[] args = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 0, arguments.length);
        return args;
    }
}
