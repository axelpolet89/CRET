package com.crawljax.plugins.csssuite.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by axel on 6/22/2015.
 */
public class DefaultStylesHelper
{
    /**
     *
     * @return
     */
    public static Map<String, String> CreateDefaultStyles()
    {
        Map<String, String> defaultStyles = new HashMap<>();

        defaultStyles.put("width", "0");
        defaultStyles.put("min-width", "0");
        defaultStyles.put("max-width", "none");
        defaultStyles.put("height", "0");
        defaultStyles.put("min-height", "0");
        defaultStyles.put("max-height", "none");

        SetSeparateStyles("padding-%s", "0", defaultStyles);
        SetSeparateStyles("margin-%s", "0", defaultStyles);

        defaultStyles.put("border-width", "0");
        defaultStyles.put("border-style", "none");
        SetSeparateStyles("border-%s-width", "0", defaultStyles);
        SetSeparateStyles("border-%s-style", "none", defaultStyles);
        defaultStyles.put("border-top-left-radius", "0");
        defaultStyles.put("border-top-right-radius", "0");
        defaultStyles.put("border-bottom-right-radius", "0");
        defaultStyles.put("border-bottom-left-radius", "0");

        defaultStyles.put("outline-width", "0");
        defaultStyles.put("outline-style", "none");

        defaultStyles.put("background-image", "none");
        defaultStyles.put("background-color", "rgba(0,0,0,0)");
        defaultStyles.put("background-repeat", "repeat");
        defaultStyles.put("background-position", "0% 0%");
        defaultStyles.put("background-attachment", "scroll");
        defaultStyles.put("background-size", "auto");
        defaultStyles.put("background-clip", "border-box");
        defaultStyles.put("background-origin", "padding-box");

        return defaultStyles;
    }


    /**
     *
     * @param formatter
     * @param value
     * @param styles
     */
    public static void SetSeparateStyles(String formatter, String value, Map<String, String> styles)
    {
        styles.put(String.format(formatter, "top"), value);
        styles.put(String.format(formatter, "right"), value);
        styles.put(String.format(formatter, "bottom"), value);
        styles.put(String.format(formatter, "left"), value);
    }
}
