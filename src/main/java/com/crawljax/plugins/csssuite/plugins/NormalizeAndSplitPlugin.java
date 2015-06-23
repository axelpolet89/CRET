package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.properties.MBorderProperty;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.ColorHelper;

import java.lang.reflect.Array;
import java.util.*;


/**
 * Created by axel on 5/27/2015.
 *
 * This class is responsible for normalizing property values contained in MSelectors
 * 1) split shorthand declarations into separate parts
 * 2) normalize zero values
 * 3) normalize url values
 */
public class NormalizeAndSplitPlugin implements ICssPostCrawlPlugin
{
    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[CssNormalizer] Start normalization of properties for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).GetRules())
            {
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    SplitShortHandDeclarations(mSelector);
                    Normalize(mSelector);

                    //sort properties again
                    mSelector.GetProperties().sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));
                }
            }
        }

        return cssRules;
    }


    /**
     * Split any shorthand margin, padding, border, border-radius, outline and background property into parts
     * @param mSelector
     */
    private static void SplitShortHandDeclarations(MSelector mSelector)
    {
        List<MProperty> newProps = new ArrayList<>();

        for(MProperty mProperty : mSelector.GetProperties())
        {
            if(mProperty.IsIgnored() || !mProperty.GetValueVendor().isEmpty())
            {
                newProps.add(mProperty);
                continue;
            }

            final String name = mProperty.GetName();
            final String value = mProperty.GetValue();
            final boolean isImportant = mProperty.IsImportant();
            final int order = mProperty.GetOrder();

            if(value.contains("-gradient") || value.contains("progid:"))
            {
                newProps.add(mProperty);
                continue;
            }

            try
            {
                if (name.equals("margin") || name.equals("padding"))
                {
                    newProps.addAll(BoxToProps(value, isImportant, order, name + "-%s"));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand '%s' property value into parts: '%s', important=%s", name, value, isImportant);
                }
                else if(name.equals("border-width") || name.equals("border-style") || name.equals("border-color"))
                {
                    String spec = name.replace("border-","");
                    newProps.addAll(BoxToProps(value, isImportant, order, "border-%s-" + spec ));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand '%s' property value into parts: '%s', important=%s", name, value, isImportant);
                }
                else if (name.contains("border"))
                {
                    if(name.equals("border-radius"))
                    {
                        newProps.addAll(BorderRadiusToProps(value, mProperty.GetNameVendor(), isImportant, order));
                        LogHandler.debug("[CssNormalizer] Transformed shorthand border-radius property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                    }
                    else if(name.equals("border") || name.equals("border-top") || name.equals("border-right") || name.equals("border-bottom") || name.equals("border-left"))
                    {
                        newProps.addAll(BorderToProps(name, value, isImportant, order));
                        LogHandler.debug("[CssNormalizer] Transformed shorthand border property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                    }
                    else
                    {
                        newProps.add(mProperty);
                    }
                }
                else if(name.equals("outline"))
                {
                    newProps.addAll(BorderToProps(name, value, isImportant, order));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand outline property into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if (name.equals("background"))
                {
                    newProps.addAll(BackgroundToProps(value, isImportant, order));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand background property into parts: '%s' : '%s', important=%s", name, value, isImportant);
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

        mSelector.SetNewProperties(newProps);
    }


    /**
     * Normalize zero values and url values
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
     * Split any box declaration into four parts: top, right, bottom, left
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MProperty> BoxToProps(String value, boolean isImportant, int order, String formatter) throws CssSuiteException
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

        props.add(new MProperty(String.format(formatter, "top"), top, isImportant, order));
        props.add(new MProperty(String.format(formatter, "right"), right, isImportant, order));
        props.add(new MProperty(String.format(formatter, "bottom"), bottom, isImportant, order));
        props.add(new MProperty(String.format(formatter, "left"), left, isImportant, order));

        return props;
    }


    /**
     * Split a shorthand border declaration into separate declarations
     * @param name
     * @param value
     * @param isImportant
     * @return
     */
    private static List<MProperty> BorderToProps(String name, String value, boolean isImportant, int order)
    {
        List<MProperty> props = new ArrayList<>();

        // either 'outline' or 'border'
        String base = name.split("-")[0];

        // first filter rgb from value, since it contains whitespace
        String rgbColor = ColorHelper.TryParseRgb(value);
        if(!rgbColor.isEmpty())
        {
            String replace = rgbColor.replaceFirst("\\(","\\\\(").replaceFirst("\\)", "\\\\)");
            value = value.replaceFirst(replace, "");
            props.add(new MBorderProperty(String.format("%s-color", name), rgbColor, isImportant, order, String.format("%s-color", base)));
        }

        String[] parts = value.split("\\s");

        for(int i = 0; i < parts.length; i++)
        {
            String part = parts[i];

            if(part.equals("none") || part.equals("solid") || part.equals("dotted")  || part.equals("dashed") ||  part.equals("double")
                    || part.equals("groove") || part.equals("ridge") || part.equals("inset") || part.equals("outset"))
            {
                props.add(new MBorderProperty(String.format("%s-style", name), part, isImportant, order, String.format("%s-style", base)));
            }
            else if (ContainsUnitLength(part) || part.equals("0"))
            {
                props.add(new MBorderProperty(String.format("%s-width", name), part, isImportant, order, String.format("%s-width", base)));
            }
            else
            {
                props.add(new MBorderProperty(String.format("%s-color", name), part, isImportant, order, String.format("%s-color", base)));
            }
        }

        return props;
    }


    /**
     *
     * @param value
     * @param isImportant
     * @return
     */
    private static List<MProperty> BorderRadiusToProps(String value, String vendor, boolean isImportant, int order) throws CssSuiteException
    {
        String[] parts = value.split("/");

        List<String> radii = ParseRadiiParts(parts[0]);
        String topLeft = radii.get(0);
        String topRight = radii.get(1);
        String bottomRight = radii.get(2);
        String bottomLeft = radii.get(3);

        if(parts.length == 2)
        {
            radii = ParseRadiiParts(parts[1]);
            topLeft += " " + radii.get(0);
            topRight += " " + radii.get(1);
            bottomRight += " " + radii.get(2);
            bottomLeft += " " + radii.get(3);
        }

        List<MProperty> result = new ArrayList<>();

        result.add( new MBorderProperty(String.format("%sborder-top-left-radius", vendor), topLeft, isImportant, order, "border-radius"));
        result.add( new MBorderProperty(String.format("%sborder-top-right-radius", vendor), topRight, isImportant, order, "border-radius"));
        result.add( new MBorderProperty(String.format("%sborder-bottom-right-radius", vendor), bottomRight, isImportant, order, "border-radius"));
        result.add( new MBorderProperty(String.format("%sborder-bottom-left-radius", vendor), bottomLeft, isImportant, order, "border-radius"));

        return result;
    }


    /**
     * Split any box declaration into four parts: top, right, bottom, left
     * @param value
     * @return
     * @throws CssSuiteException
     */
    private static List<String> ParseRadiiParts(String value) throws CssSuiteException
    {
        String topLeft, topRight, bottomRight, bottomLeft;
        String[] parts = value.split("\\s");

        switch (parts.length)
        {
            case 1:
                topLeft = topRight = bottomRight = bottomLeft = parts[0];
                break;
            case 2:
                topLeft = bottomRight = parts[0];
                topRight = bottomLeft = parts[1];
                break;
            case 3:
                topLeft = parts[0];
                topRight = parts[1];
                bottomRight = parts[2];
                bottomLeft = topRight;
                break;
            case 4:
                topLeft = parts[0];
                topRight = parts[1];
                bottomRight = parts[2];
                bottomLeft = parts[3];
                break;
            default:
                throw new CssSuiteException("Cannot normalize value '%s', because number of parts is larger than 4 or smaller than 1", value);
        }

        return Arrays.asList(topLeft, topRight, bottomRight, bottomLeft);
    }



    /**
     * Split a shorthand background property into separate declararations
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MProperty> BackgroundToProps(String value, boolean isImportant, int order) throws CssSuiteException
    {
        List<MProperty> props = new ArrayList<>();

        // first filter rgb from value, since it contains whitespace
        String rgbColor = ColorHelper.TryParseRgb(value);
        if(!rgbColor.isEmpty())
        {
            String replace = rgbColor.replaceFirst("\\(","\\\\(").replaceFirst("\\)", "\\\\)");
            value = value.replaceFirst(replace, "");
            props.add(new MProperty("background-color", rgbColor, isImportant, order));
        }

        String[] parts = value.split("\\s");

        boolean originSet = false;
        boolean positionSet = false;

        for(int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if(part.isEmpty())
            {
                continue;
            }

            if(part.contains("repeat"))
            {
                props.add(new MProperty("background-repeat", part, isImportant, order));
            }
            else if (part.equals("scroll") || part.equals("fixed") || part.equals("local"))
            {
                props.add(new MProperty("background-attachment", part, isImportant, order));
            }
            else if(part.equals("padding-box") || part.equals("border-box") || part.equals("content-box"))
            {
                if(!originSet)
                {
                    props.add(new MProperty("background-origin", part, isImportant, order));
                    originSet = true;
                }
                else
                {
                    props.add(new MProperty("background-clip", part, isImportant, order));
                }
            }
            else if (part.contains("url") || part.equals("none"))
            {
                props.add(new MProperty("background-image", part, isImportant, order));
            }
            else if (part.equals("left") || part.equals("right") || part.equals("center") || part.equals("bottom") || part.equals("top") ||
                        ContainsUnitLength(part) || part.contains("%") || part.equals("0") || (!part.contains("url") && part.contains("/")))
            {
                String position = "";
                String size = "";

                boolean sizeProp = false;

                int j;
                for(j = i; j < parts.length; j++)
                {
                    String part2 = parts[j];

                    if(part2.equals("/"))
                    {
                        sizeProp = true;
                    }
                    else if (!part2.contains("url") && part2.contains("/"))
                    {
                        position += " " + part2.replace("/", "");
                        sizeProp = true;
                    }
                    else if(sizeProp)
                    {
                        if(ContainsUnitLength(part2) || part2.contains("%") || part2.equals("0"))
                        {
                            size += " " + part2;
                        }
                        else
                        {
                            i = j;
                            break;
                        }
                    }
                    else if (part2.equals("left") || part2.equals("right") || part2.equals("center") || part2.equals("bottom") || part2.equals("top") ||
                            ContainsUnitLength(part2) || part2.contains("%") || part2.equals("0"))
                    {
                        if(!position.isEmpty())
                        {
                            position += " " + part2;
                        }
                        else
                        {
                            position = part2;
                        }
                    }
                    else
                    {
                        i = j;
                        break;
                    }

                    i++;
                }

                i--;

                props.add(new MProperty("background-position", position, isImportant, order));

                if(!size.isEmpty())
                {
                    props.add(new MProperty("background-size", size, isImportant, order));
                }
            }
            else
            {
                props.add(new MProperty("background-color", part, isImportant, order));
            }
        }

        return props;
    }


    /**
     *
     * @param value
     * @return
     */
    private static boolean ContainsUnitLength(String value)
    {
        return value.contains("px") || value.contains("pt") || value.contains("pc") || value.contains("in") || value.contains("mm") || value.contains("cm") || //absolute
                value.contains("em") || value.contains("rem") || value.contains("ex") || value.contains("ch") || value.contains("vw") || value.contains("vh") || value.contains("vmin") || value.contains("vmax"); //relative
    }
}