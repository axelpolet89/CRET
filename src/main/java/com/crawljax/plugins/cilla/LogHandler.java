package com.crawljax.plugins.cilla;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Created by axel on 5/21/2015.
 */
public class LogHandler
{
    private final static Logger LOGGER = LogManager.getLogger("css.suite.logger");

    public static void info(String text, Object... arguments)
    {
        LOGGER.info(String.format(text, arguments));
    }

    public static void warn(String text, Object... arguments)
    {
        LOGGER.warn(String.format(text, arguments));
    }

    public static void error(String text, Object... arguments)
    {
        LOGGER.error(String.format(text, arguments));
    }

    public static void error(Exception ex, String text, Object... arguments)
    {
        String stackTrace = "";
        for(StackTraceElement traceElement : ex.getStackTrace())
        {
            stackTrace += String.format("%s\n", traceElement);
        }

        //need to copy varargs to Object[], otherwise String.format fails
        Object[] args = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 0, arguments.length);

        String message = String.format(text, args);
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
}
