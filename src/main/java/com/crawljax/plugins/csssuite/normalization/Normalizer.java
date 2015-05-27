package com.crawljax.plugins.csssuite.normalization;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by axel on 5/27/2015.
 */
public class Normalizer implements ICssPostCrawlPlugin
{
    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {
        for (String file : cssRules.keySet())
        {
            for(MCssRule mRule : cssRules.get(file).GetRules())
            {
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    NormalizeZeroes(mSelector);
                    NormalizeToShortHand(mSelector);
                }
            }
        }

        return cssRules;
    }


    /**
     *
     * @param mSelector
     */
    private static void NormalizeZeroes(MSelector mSelector)
    {
        for(MProperty mProperty : mSelector.GetProperties())
        {
            final String orig = mProperty.GetOriginalValue().replaceAll("\\s","");
            if(orig.equals("0px") || orig.equals("0pt") || orig.equals("0%"))
            {
                mProperty.SetNormalizedValue("0");
                LogHandler.info("[CssNormalizer] Normalized zeroes in '%s' -> original: '%s', new: '%s'", mSelector,  mProperty.GetOriginalValue(), mProperty.GetValue());
            }
        }
    }


    /**
     *
     * @param mSelector
     */
    private static void NormalizeToShortHand(MSelector mSelector)
    {
        List<MProperty> newProps = new ArrayList<>();

        for(MProperty mProperty : mSelector.GetProperties())
        {
            final String name = mProperty.GetName();
            final String value = mProperty.GetValue();
            final boolean isImportant = mProperty.IsImportant();

            try
            {
                if (name.equals("margin"))
                {
                    newProps.addAll(BoxToProps(name, value, isImportant));
                    LogHandler.info("[CssNormalizer] Parsed shorthand margin property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if (name.equals("padding"))
                {
                    newProps.addAll(BoxToProps(name, value, isImportant));
                    LogHandler.info("[CssNormalizer] Parsed shorthand padding property into parts '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if (name.contains("border"))
                {
                    newProps.addAll(BorderToProps(name, value, isImportant));
                    LogHandler.info("[CssNormalizer] Parsed shorthand border property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if (name.equals("background"))
                {
                    newProps.addAll(BackgroundToProps(value, isImportant));
                    LogHandler.info("[CssNormalizer] Parsed shorthand background property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else
                {
                    newProps.add(mProperty);
                }
            }
            catch (Exception e)
            {
                LogHandler.error(e, "[CssNormalizer] NormalizeException on selector '%s', in property '%s'", mSelector, mProperty);
                newProps.add(mProperty);
            }
        }

        mSelector.ReplaceProperties(newProps);
    }


    /**
     *
     * @param name
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MProperty> BoxToProps(String name, String value, boolean isImportant) throws CssSuiteException
    {
        String[] parts = value.split("\\s");

        String top;
        String right;
        String bottom;
        String left;

        switch (parts.length)
        {
            case 1:
                top = right = bottom = left = parts[0];
                break;
            case 2:
                top = bottom = parts[0];
                right = left = parts[1];
                break;
            case 3:
                top = parts[0];
                right = parts[1];
                bottom = parts[2];
                left = right;
                break;
            case 4:
                top = parts[0];
                right = parts[1];
                bottom = parts[2];
                left = parts[3];
                break;
            default:
                throw new CssSuiteException("Cannot normalize value '%s', because number of parts is larger than 4 or smaller than 1", value);
        }

        List<MProperty> props = new ArrayList<>();

        props.add(new MProperty(String.format("%s-top", name), top, isImportant));
        props.add(new MProperty(String.format("%s-right", name), right, isImportant));
        props.add(new MProperty(String.format("%s-bottom", name), bottom, isImportant));
        props.add(new MProperty(String.format("%s-left", name), left, isImportant));

        return props;
    }


    /**
     *
     * @param name
     * @param value
     * @param isImportant
     * @return
     */
    private static List<MProperty> BorderToProps(String name, String value, boolean isImportant)
    {
        List<MProperty> props = new ArrayList<>();

        // first filter rgb from value, since it contains whitespace
        String rgbColor = TryParseRgb(value);
        if(!rgbColor.isEmpty())
        {
            String replace = rgbColor.replaceFirst("\\(","\\\\(").replaceFirst("\\)", "\\\\)");
            value = value.replaceFirst(replace, "");
            props.add(new MProperty(String.format("%s-color", name), rgbColor, isImportant));
        }

        String[] parts = value.split("\\s");

        for(int i = 0; i < parts.length; i++)
        {
            String part = parts[i];

            if(part.equals("none") || part.equals("solid") || part.equals("dotted")  || part.equals("dashed") ||  part.equals("double")
                || part.equals("groove") || part.equals("ridge") || part.equals("inset") || part.equals("outset"))
            {
                props.add(new MProperty(String.format("%s-style", name), part, isImportant));
            }
            else if (part.contains("px") || part.contains("pt") || part.contains("em") || part.contains("rem"))
            {
                props.add(new MProperty(String.format("%s-width", name), part, isImportant));
            }
            else
            {
                props.add(new MProperty(String.format("%s-color", name), part, isImportant));
            }
        }

        return props;
    }


    /**
     *
     * @param name
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MProperty> BackgroundToProps(String value, boolean isImportant) throws CssSuiteException
    {
        List<MProperty> props = new ArrayList<>();

        // first filter rgb from value, since it contains whitespace
        String rgbColor = TryParseRgb(value);
        if(!rgbColor.isEmpty())
        {
            String replace = rgbColor.replaceFirst("\\(","\\\\(").replaceFirst("\\)", "\\\\)");
            value = value.replaceFirst(replace, "");
            props.add(new MProperty("background-color", rgbColor, isImportant));
        }

        String[] parts = value.split("\\s");

        for(int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if(part.isEmpty())
            {
                continue;
            }

            if(part.contains("repeat"))
            {
                props.add(new MProperty("background-repeat", part, isImportant));
            }
            else if (part.equals("scroll") || part.equals("fixed") || part.equals("local"))
            {
                props.add(new MProperty("background-attachment", part, isImportant));
            }
            else if (part.contains("url") || part.equals("none"))
            {
                props.add(new MProperty("background-image", part, isImportant));
            }
            else if (part.equals("left") || parts.equals("right") || part.equals("center"))
            {
                String position = part;
                if(i+1 < parts.length)
                {
                    String part2 = parts[i + 1];
                    if (part2.equals("top") || part2.equals("bottom") || part2.equals("center"))
                    {
                        position += " " + part2;
                        i++;
                    }
                }
                props.add(new MProperty("background-position", position, isImportant));
            }
            else if (part.contains("%"))
            {
                String position = part;
                if(i+1 < parts.length)
                {
                    String part2 = parts[i + 1];
                    if (part2.contains("%"))
                    {
                        position += " " + part2;
                        i++;
                    }
                }
                props.add(new MProperty("background-position", position, isImportant));
            }
            else if (part.contains("px") || part.contains("pt") || part.contains("em") || part.contains("rem"))
            {
                String position = part;
                if(i+1 < parts.length)
                {
                    String part2 = parts[i + 1];
                    if (part2.contains("px") || part2.contains("pt") || part2.contains("em") || part2.contains("rem"))
                    {
                        position += " " + part2;
                        i++;
                    }
                }
                props.add(new MProperty("background-position", position, isImportant));
            }
            else
            {
                props.add(new MProperty("background-color", part, isImportant));
            }
        }

        return props;
    }


    /**
     *
     * @param value
     * @return
     */
    private static String TryParseRgb(String value)
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
}