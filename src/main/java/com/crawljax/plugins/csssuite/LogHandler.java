package com.crawljax.plugins.csssuite;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sun.rmi.runtime.Log;

/**
 * Created by axel on 5/21/2015.
 */
public class LogHandler
{
    private final static Logger LOGGER = LogManager.getLogger("css.suite.logger");

    public static void info(String text)
    {
        LOGGER.info(text);
    }

    public static void info(String text, Object... arguments)
    {
        LOGGER.info(String.format(text, VarArgsToArray(arguments)));
    }

    public static void warn(String text)
    {
        LOGGER.warn(text);
    }

    public static void warn(String text, Object... arguments)
    {
        LOGGER.warn(String.format(text, VarArgsToArray(arguments)));
    }

    public static void error(String text, Object... arguments)
    {
        LOGGER.error(String.format(text, VarArgsToArray(arguments)));
    }

    public static void error(Exception ex, String text, Object... arguments)
    {
        String stackTrace = "";
        for(StackTraceElement traceElement : ex.getStackTrace())
        {
            stackTrace += String.format("%s\n", traceElement);
        }

        String message = String.format(text, VarArgsToArray(arguments));
        message += String.format("\n[Exception] %s\n[StackTrace] %s", ex.getMessage(), stackTrace);
        LOGGER.error(message);
    }

    public static void error(Exception ex)
    {
        String stackTrace = "";
        for(StackTraceElement traceElement : ex.getStackTrace())
        {
            stackTrace += String.format("%s\n", traceElement);
        }

        LOGGER.error(String.format("\n[Exception] %s\n[StackTrace] %s", ex.getMessage(), stackTrace));
    }

    private static Object[] VarArgsToArray(Object... arguments)
    {
        //need to copy varargs to Object[], otherwise String.format fails
        Object[] args = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 0, arguments.length);
        return args;
    }
}
