package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.colors.BrowserColorParser;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.declarations.MBorderDeclaration;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.interfaces.ICssTransformer;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.*;


/**
 * Created by axel on 5/27/2015.
 *
 * This class is responsible for normalizing declaration values contained in MSelectors
 * 1) split shorthand declarations into separate parts
 * 2) normalize zero values
 * 3) normalize url values
 */
public class NormalizeAndSplitPlugin implements ICssTransformer
{
    private BrowserColorParser _browserColorParser = new BrowserColorParser();
    private int _normalizedColors = 0;
    private int _normalizedUrls = 0;
    private int _normalizedZeroes = 0;

    @Override
    public void getStatistics(SuiteStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<NC>%d</NC>", prefix, _normalizedColors);
        builder.appendLine("%s<NU>%d</NU>", prefix, _normalizedUrls);
        builder.appendLine("%s<NZ>%d</NZ>", prefix, _normalizedZeroes);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[CssNormalizer] Start normalization of declarations for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).getRules())
            {
                LogHandler.debug("Rule: %s", mRule);
                for(MSelector mSelector : mRule.getSelectors())
                {
                    normalizeColors(mSelector);
                    splitShortHandDeclarations(mSelector);
                    normalizeZeroes(mSelector);
                    normalizeUrls(mSelector);

                    //sort declarations again
                    mSelector.getDeclarations().sort((p1, p2) -> Integer.compare(p1.getOrder(), p2.getOrder()));
                }
            }
        }

        return cssRules;
    }


    /**
     * Normalize any rgb color to it's hexadecimal representation
     * Normalize any rbga color by filtering whitespace between it's color parts
     * @param mSelector
     */
    private void normalizeColors(MSelector mSelector)
    {
        for(MDeclaration mDeclaration : mSelector.getDeclarations())
        {
            if(mDeclaration.isIgnored())
            {
                continue;
            }

            final String origValue = mDeclaration.getValue();
            String newValue = origValue;

            if(origValue.contains("rgb"))
            {
                // transform any rgb(...) value into it's hexadecimal representation
                while (newValue.contains("rgb("))
                {
                    _normalizedColors++;

                    String rgbValue = tryFindRgb(newValue);
                    String rgbReplace = rgbValue.replace("(", "\\(").replace(")", "\\)");

                    String[] rgbParts = rgbValue.replaceFirst("rgb\\(", "").replaceFirst("\\)", "").split(",");
                    String hexValue = rgbToHex(Integer.parseInt(rgbParts[0].trim()), Integer.parseInt(rgbParts[1].trim()), Integer.parseInt(rgbParts[2].trim()));
                    newValue = newValue.replaceFirst(rgbReplace, hexValue);
                }

                // filter any whitespace in a rbga(...) value
                if(newValue.contains("rgba("))
                {
                    String text = newValue;

                    while (text.contains("rgba("))
                    {
                        _normalizedColors++;

                        String rgbaOrig = tryFindRgba(text);
                        String rgbaReplace = rgbaOrig.replace("(", "\\(").replace(")", "\\)");
                        text = text.replaceFirst(rgbaReplace, "");

                        String[] rgbaParts = rgbaOrig.replaceFirst("rgba\\(", "").replaceFirst("\\)", "").split(",");
                        if(rgbaParts.length == 3 || (rgbaParts.length == 4 && (rgbaParts[3].trim().equals("1") || rgbaParts[3].trim().equals("100"))))
                        {
                            String hexValue = rgbToHex(Integer.parseInt(rgbaParts[0].trim()), Integer.parseInt(rgbaParts[1].trim()), Integer.parseInt(rgbaParts[2].trim()));
                            newValue = newValue.replaceFirst(rgbaReplace, hexValue);
                        }
                        else
                        {
                            String rgbaNew = rgbaOrig.replaceAll("\\s", "");
                            newValue = newValue.replaceFirst(rgbaReplace, rgbaNew);
                        }
                    }
                }
            }

            String[] parts = newValue.split("\\s");

            for(String part : parts)
            {
                String newPart = _browserColorParser.tryParseColorToHex(part);
                if(!part.equals(newPart))
                {
                    _normalizedColors++;
                    newValue = newValue.replace(part, newPart);
                }
                else if(part.equals("transparent"))
                {
                    newValue = newValue.replace("transparent", "rgba(0,0,0,0)");
                }
            }

            mDeclaration.setNormalizedValue(newValue);
        }
    }


    /**
     * Normalize zero values and url values
     * @param mSelector
     */
    private void normalizeZeroes(MSelector mSelector)
    {
        for(MDeclaration mDeclaration : mSelector.getDeclarations())
        {
            if(mDeclaration.isIgnored())
                continue;

            final String origValue = mDeclaration.getValue();
            final String value = mDeclaration.getValue().replaceAll("\\s", "");

            if (value.equals("0px") || value.equals("0pt") || value.equals("0%") || value.equals("0pc") || value.equals("0in") || value.equals("0mm") || value.equals("0cm") || //absolute
                    value.equals("0em") || value.equals("0rem") || value.equals("0ex") || value.equals("0ch") || value.equals("0vw") || value.equals("0vh") || value.equals("0vmin") || value.equals("0vmax")) //relative
            {
                _normalizedZeroes++;
                mDeclaration.setNormalizedValue("0");
                LogHandler.debug("[CssNormalizer] Normalized zeroes in '%s' -> original: '%s', new: '%s'", mSelector, mDeclaration.getOriginalValue(), mDeclaration.getValue());
            }
            else if (mDeclaration.getOriginalValue().contains("0."))
            {
                _normalizedZeroes++;
                mDeclaration.setNormalizedValue(origValue.replaceAll("0\\.", "\\."));
            }
        }
    }


    /**
     * Normalize zero values and url values
     * @param mSelector
     */
    private void normalizeUrls(MSelector mSelector)
    {
        for(MDeclaration mDeclaration : mSelector.getDeclarations())
        {
            if(mDeclaration.isIgnored())
                continue;

            final String origValue = mDeclaration.getValue();

            if(origValue.contains("http://"))
            {
                _normalizedUrls++;
                mDeclaration.setNormalizedValue(origValue.replaceAll("http://", ""));
            }
            else if(origValue.contains("https://"))
            {
                _normalizedUrls++;
                mDeclaration.setNormalizedValue(origValue.replaceAll("https://", ""));
            }
        }
    }


    /**
     *
     * @param r
     * @param g
     * @param b
     * @return
     */
    private static String rgbToHex(int r, int g, int b)
    {
        return String.format("#%02x%02x%02x", r, g, b);
    }


    /**
     *
     * @param value
     * @return
     */
    private static String tryFindRgb(String value)
    {
        if (value.contains("rgb("))
        {
            int s =  value.indexOf("rgb(");
            int e = value.indexOf(")", s);
            return value.substring(s, e+1);
        }

        return "";
    }


    /**
     *
     * @param value
     * @return
     */
    private static String tryFindRgba(String value)
    {
        if(value.contains("rgba("))
        {
            int s =  value.indexOf("rgba(");
            int e = value.indexOf(")", s);
            return value.substring(s, e+1);
        }

        return "";
    }


    /**
     * Split any shorthand margin, padding, border, border-radius, outline and background declaration into parts
     * @param mSelector
     */
    private static void splitShortHandDeclarations(MSelector mSelector)
    {
        List<MDeclaration> newDeclarations = new ArrayList<>();

        for(MDeclaration mDeclaration : mSelector.getDeclarations())
        {
            if(mDeclaration.isIgnored())
            {
                newDeclarations.add(mDeclaration);
                continue;
            }

            final String name = mDeclaration.getName();
            final String value = mDeclaration.getValue();
            final boolean isImportant = mDeclaration.isImportant();
            final int order = mDeclaration.getOrder();


            try
            {
                if (name.equals("margin") || name.equals("padding"))
                {
                    newDeclarations.addAll(boxToDeclarations(value, isImportant, order, name + "-%s"));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand '%s' declaration value into parts: '%s', important=%s", name, value, isImportant);
                }
                else if(name.equals("border-width") || name.equals("border-style") || name.equals("border-color"))
                {
                    String spec = name.replace("border-","");
                    newDeclarations.addAll(boxToDeclarations(value, isImportant, order, "border-%s-" + spec));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand '%s' declaration value into parts: '%s', important=%s", name, value, isImportant);
                }
                else if (name.contains("border"))
                {
                    if(name.equals("border-radius"))
                    {
                        newDeclarations.addAll(borderRadiusToDeclarations(value, mDeclaration.getNameVendor(), isImportant, order));
                        LogHandler.debug("[CssNormalizer] Transformed shorthand border-radius declaration into parts: '%s' : '%s', important=%s", name, value, isImportant);
                    }
                    else if(name.equals("border"))
                    {
                        //todo: transform border parts into box parts, however difficult to implement significant impact in merger...
                        //especially because of ordering...

                        newDeclarations.addAll(borderToDeclarations(name, value, isImportant, order));
                        LogHandler.debug("[CssNormalizer] Transformed shorthand border declaration into parts: '%s' : '%s', important=%s", name, value, isImportant);
                    }
                    else if(name.equals("border-top") || name.equals("border-right") || name.equals("border-bottom") || name.equals("border-left"))
                    {
                        newDeclarations.addAll(borderToDeclarations(name, value, isImportant, order));
                        LogHandler.debug("[CssNormalizer] Transformed shorthand border declaration into parts: '%s' : '%s', important=%s", name, value, isImportant);
                    }
                    else
                    {
                        newDeclarations.add(mDeclaration);
                    }
                }
                else if(name.equals("outline"))
                {
                    newDeclarations.addAll(borderToDeclarations(name, value, isImportant, order));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand outline declaration into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else if (name.equals("background") && !value.contains(",")) // do not support multiple backgrounds
                {
                    newDeclarations.addAll(backgroundToDeclarations(value, isImportant, order));
                    LogHandler.debug("[CssNormalizer] Transformed shorthand background declaration into parts: '%s' : '%s', important=%s", name, value, isImportant);
                }
                else
                {
                    newDeclarations.add(mDeclaration);
                }
            }
            catch (CssSuiteException e)
            {
                LogHandler.warn(e, "[CssNormalizer] CssSuiteException on selector '%s', in declaration '%s'", mSelector, mDeclaration);
                newDeclarations.add(mDeclaration);
            }
            catch (Exception e)
            {
                LogHandler.error("[CssNormalizer] Exception on selector '%s', in declaration '%s'", mSelector, mDeclaration);
                newDeclarations.add(mDeclaration);
            }
        }

        mSelector.setNewDeclarations(newDeclarations);
    }



    /**
     * Split any box declaration into four parts: top, right, bottom, left
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MDeclaration> boxToDeclarations(String value, boolean isImportant, int order, String formatter) throws CssSuiteException
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

        List<MDeclaration> props = new ArrayList<>();

        props.add(new MDeclaration(String.format(formatter, "top"), top, isImportant, order));
        props.add(new MDeclaration(String.format(formatter, "right"), right, isImportant, order));
        props.add(new MDeclaration(String.format(formatter, "bottom"), bottom, isImportant, order));
        props.add(new MDeclaration(String.format(formatter, "left"), left, isImportant, order));

        return props;
    }


    /**
     * Split a shorthand border declaration into separate declarations
     * @param name
     * @param value
     * @param isImportant
     * @return
     */
    private static List<MDeclaration> borderToDeclarations(String name, String value, boolean isImportant, int order)
    {
        List<MDeclaration> props = new ArrayList<>();

        // either 'outline' or 'border'
        String base = name.split("-")[0];

        String[] parts = value.split("\\s");

        for(int i = 0; i < parts.length; i++)
        {
            String part = parts[i];

            if(part.equals("none") || part.equals("solid") || part.equals("dotted")  || part.equals("dashed") ||  part.equals("double")
                    || part.equals("groove") || part.equals("ridge") || part.equals("inset") || part.equals("outset"))
            {
                props.add(new MBorderDeclaration(String.format("%s-style", name), part, isImportant, order, String.format("%s-style", base)));
            }
            else if (containsUnitLength(part) || part.equals("0"))
            {
                props.add(new MBorderDeclaration(String.format("%s-width", name), part, isImportant, order, String.format("%s-width", base)));
            }
            else
            {
                props.add(new MBorderDeclaration(String.format("%s-color", name), part, isImportant, order, String.format("%s-color", base)));
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
    private static List<MDeclaration> borderRadiusToDeclarations(String value, String vendor, boolean isImportant, int order) throws CssSuiteException
    {
        String[] parts = value.split("/");

        List<String> radii = parseRadiiParts(parts[0]);
        String topLeft = radii.get(0);
        String topRight = radii.get(1);
        String bottomRight = radii.get(2);
        String bottomLeft = radii.get(3);

        if(parts.length == 2)
        {
            radii = parseRadiiParts(parts[1]);
            topLeft += " " + radii.get(0);
            topRight += " " + radii.get(1);
            bottomRight += " " + radii.get(2);
            bottomLeft += " " + radii.get(3);
        }

        List<MDeclaration> result = new ArrayList<>();

        result.add( new MBorderDeclaration(String.format("%sborder-top-left-radius", vendor), topLeft, isImportant, order, "border-radius"));
        result.add( new MBorderDeclaration(String.format("%sborder-top-right-radius", vendor), topRight, isImportant, order, "border-radius"));
        result.add( new MBorderDeclaration(String.format("%sborder-bottom-right-radius", vendor), bottomRight, isImportant, order, "border-radius"));
        result.add( new MBorderDeclaration(String.format("%sborder-bottom-left-radius", vendor), bottomLeft, isImportant, order, "border-radius"));

        return result;
    }


    /**
     * Split any box declaration into four parts: top, right, bottom, left
     * @param value
     * @return
     * @throws CssSuiteException
     */
    private static List<String> parseRadiiParts(String value) throws CssSuiteException
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
     * Split a shorthand background declaration into separate declararations
     * @param value
     * @param isImportant
     * @return
     * @throws CssSuiteException
     */
    private static List<MDeclaration> backgroundToDeclarations(String value, boolean isImportant, int order) throws CssSuiteException
    {
        List<MDeclaration> props = new ArrayList<>();

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
                props.add(new MDeclaration("background-repeat", part, isImportant, order));
            }
            else if (part.equals("scroll") || part.equals("fixed") || part.equals("local"))
            {
                props.add(new MDeclaration("background-attachment", part, isImportant, order));
            }
            else if(part.equals("padding-box") || part.equals("border-box") || part.equals("content-box"))
            {
                if(!originSet)
                {
                    props.add(new MDeclaration("background-origin", part, isImportant, order));
                    originSet = true;
                }
                else
                {
                    props.add(new MDeclaration("background-clip", part, isImportant, order));
                }
            }
            else if (part.contains("url") || part.equals("none"))
            {
                props.add(new MDeclaration("background-image", part, isImportant, order));
            }
            else if (part.equals("left") || part.equals("right") || part.equals("center") || part.equals("bottom") || part.equals("top") ||
                        containsUnitLength(part) || part.contains("%") || part.equals("0") || part.equals("auto") || !part.contains("url") && part.contains("/"))
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
                        if(containsUnitLength(part2) || part2.contains("%") || part2.equals("0") || part2.equals("auto"))
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
                            containsUnitLength(part2) || part2.contains("%") || part2.equals("0"))
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

                props.add(new MDeclaration("background-position", position, isImportant, order));

                if(!size.isEmpty())
                {
                    props.add(new MDeclaration("background-size", size, isImportant, order));
                }
            }
            else
            {
                props.add(new MDeclaration("background-color", part, isImportant, order));
            }
        }

        return props;
    }


    /**
     *
     * @param value
     * @return
     */
    private static boolean containsUnitLength(String value)
    {
        return value.contains("px") || value.contains("pt") || value.contains("pc") || value.contains("in") || value.contains("mm") || value.contains("cm") || //absolute
                value.contains("em") || value.contains("rem") || value.contains("ex") || value.contains("ch") || value.contains("vw") || value.contains("vh") || value.contains("vmin") || value.contains("vmax"); //relative
    }
}