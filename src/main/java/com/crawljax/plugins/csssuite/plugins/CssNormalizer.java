package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by axel on 5/27/2015.
 *
 * This class is responsible for normalizing property values:
 * 1) split shorthand declarations into separate parts
 * 2) normalize zero values
 * 3) normalize url values
 */
public class CssNormalizer implements ICssPostCrawlPlugin
{
    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[CssNormalizer] Start normalization of properties for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).GetRules())
            {
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    RemoveShortHandDeclarations(mSelector);
                    Normalize(mSelector);
                }
            }
        }

        return cssRules;
    }


    /**
     *
     * @param mSelector
     */
    private static void Normalize(MSelector mSelector)
    {
        for(MProperty mProperty : mSelector.GetProperties())
        {
            if(mProperty.IsIgnored())
                continue;

            final String origValue = mProperty.GetOriginalValue();
            final String value = mProperty.GetOriginalValue().replaceAll("\\s", "");

            if (value.contains("0"))
            {
                if (value.equals("0px") || value.equals("0pt") || value.equals("0%") || value.equals("0pc") || value.equals("0in") || value.equals("0mm") || value.equals("0cm") || //absolute
                        value.equals("0em") || value.equals("0rem") || value.equals("0ex") || value.equals("0ch") || value.equals("0vw") || value.equals("0vh") || value.equals("0vmin") || value.equals("0vmax")) //relative
                {
                    mProperty.SetNormalizedValue("0");
                    LogHandler.debug("[CssNormalizer] Normalized zeroes in '%s' -> original: '%s', new: '%s'", mSelector, mProperty.GetOriginalValue(), mProperty.GetValue());
                }
                else if (mProperty.GetOriginalValue().contains("0."))
                {
                    mProperty.SetNormalizedValue(origValue.replaceAll("0\\.", "\\."));
                }
            }

            if(value.contains("http://"))
            {
                mProperty.SetNormalizedValue(origValue.replaceAll("http://", ""));
            }
            else if(value.contains("https://"))
            {
                mProperty.SetNormalizedValue(origValue.replaceAll("https://", ""));
            }
        }
    }


    /**
     *
     * @param mSelector
     */
    private static void RemoveShortHandDeclarations(MSelector mSelector)
    {
        List<MProperty> newProps = new ArrayList<>();

        for(MProperty mProperty : mSelector.GetProperties())
        {
            if(mProperty.IsIgnored())
                continue;

            final String name = mProperty.GetName();
            final String value = mProperty.GetValue();
            final boolean isImportant = mProperty.IsImportant();

            try
            {
                if (name.equals("margin") || name.equals("padding"))
                {
                    newProps.addAll(BoxToProps(value, isImportant, name + "-%s"));
                    LogHandler.debug("[CssNormalizer] Parsed shorthand '%s' property value into parts: '%s', important=%s", name, value, isImportant);
                }
                else if(name.equals("border-width") || name.equals("border-style") || name.equals("border-color"))
                {
                    String spec = name.replace("border-","");
                    newProps.addAll(BoxToProps(value, isImportant, "border-%s-" + spec ));
                    LogHandler.debug("[CssNormalizer] Parsed shorthand '%s' property value into parts: '%s', important=%s", name, value, isImportant);
                }
                else if (name.contains("border"))
                {
                    if(name.equals("border-radius"))
                    {
                        newProps.add(new MProperty("border-top-left-radius", value, isImportant));
                        newProps.add(new MProperty("border-top-right-radius", value, isImportant));
                        newProps.add(new MProperty("border-bottom-right-radius", value, isImportant));
                        newProps.add(new MProperty("border-bottom-left-radius", value, isImportant));
                        LogHandler.info("[CssNormalizer] Parsed shorthand border-radius property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                    }

                    newProps.addAll(BorderToProps(name, value, isImportant));
                    LogHandler.debug("[CssNormalizer] Parsed shorthand border property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if(name.equals("outline"))
                {
                    newProps.addAll(BorderToProps(name, value, isImportant));
                    LogHandler.debug("[CssNormalizer] Parsed shorthand outline property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if (name.equals("background"))
                {
                    newProps.addAll(BackgroundToProps(value, isImportant));
                    LogHandler.debug("[CssNormalizer] Parsed shorthand background property into parts: '%s' : '%s', important=%s", name, value, isImportant);
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
     * Split any box declaration into four parts: top, right, bottom, left
     * @param name
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MProperty> BoxToProps(String value, boolean isImportant, String formatter) throws CssSuiteException
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

        props.add(new MProperty(String.format(formatter, "top"), top, isImportant));
        props.add(new MProperty(String.format(formatter, "right"), right, isImportant));
        props.add(new MProperty(String.format(formatter, "bottom"), bottom, isImportant));
        props.add(new MProperty(String.format(formatter, "left"), left, isImportant));

        return props;
    }


    /**
     * Split a shorthand border declaration into separate declarations
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
            else if (ContainsUnitLength(part))
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
     * Split a shorthand background property into separate declararations
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
            else if (ContainsUnitLength(part) || part.contains("%"))
            {
                String position = part;
                if(i+1 < parts.length)
                {
                    String part2 = parts[i + 1];
                    if (ContainsUnitLength(part2) || part.contains("%"))
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


    /**
     *
     * @param value
     * @return
     */
    private static boolean ContainsUnitLength(String value)
    {
        return value.contains("px") || value.equals("pt") || value.equals("pc") || value.equals("in") || value.equals("mm") || value.equals("cm") || //absolute
                value.equals("em") || value.equals("rem") || value.equals("ex") || value.equals("ch") || value.equals("vw") || value.equals("vh") || value.equals("vmin") || value.equals("vmax"); //relative
    }
}