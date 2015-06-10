package com.crawljax.plugins.csssuite.util;

/**
 * Created by axel on 6/9/2015.
 */
public class ColorHelper
{
    /**
     *
     * @param value
     * @return
     */
    public static String TryParseRgb(String value)
    {
        if(value.contains("rgba("))
        {
            int s =  value.indexOf("rgba(");
            int e = value.indexOf(")");
            if(s > e)
                e = value.lastIndexOf(")");
            return value.substring(s, e+1);
        }

        if (value.contains("rgb("))
        {
            int s =  value.indexOf("rgb(");
            int e = value.indexOf(")");
            if(s > e)
                e = value.lastIndexOf(")");
            return value.substring(s, e+1);
        }

        return "";
    }


    public static String TryParseUrl(String value)
    {
        int s =  value.indexOf("url(");
        int e = value.indexOf(")");
        if(s > e)
            e = value.lastIndexOf(")");
        return value.substring(s, e+1);
    }
}
