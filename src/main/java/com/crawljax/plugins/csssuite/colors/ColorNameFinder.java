package com.crawljax.plugins.csssuite.colors;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;

import java.io.*;
import java.util.*;

/**
 * Created by axel on 6/9/2015.
 *
 * Adapted from Name that Color JavaScript
 * http://chir.ag/projects/ntc
 *
 * Tries to find a name for a given color using a table with over 1500 colors
 * If the table does not contain the given color, the nearest color to the given color
 * is located and that name is used.
 */
public class ColorNameFinder
{
    private class Color
    {
        public final String name;
        public final Rgb rgb;
        public final Hsl hsl;

        public Color(String name, Rgb rgb, Hsl hsl)
        {
            this.name = name;
            this.rgb = rgb;
            this.hsl = hsl;
        }
    }

    private class Rgb
    {
        public final int r;
        public final int g;
        public final int b;

        public Rgb(int r, int g, int b)
        {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private class Hsl
    {
        public final int h;
        public final int s;
        public final int l;

        public Hsl(int h, int s, int l)
        {
            this.h = h;
            this.s = s;
            this.l = l;
        }
    }

    private final Map<String, Color> _hexColorMap;
    private final Map<String, String> _hexNameMap;

    public ColorNameFinder()
    {
        _hexColorMap = new LinkedHashMap<>();
        _hexNameMap = new LinkedHashMap<>();

        init();
        buildColors();
    }


    /**
     *
     * @return
     */
    private void init()
    {
        File file = new File("./src/main/java/com/crawljax/plugins/csssuite/colors/color_names_hex.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                String[] parts = line.split(":");
                _hexNameMap.put(parts[0], parts[1].replace(" ","_").toLowerCase());
            }
        }
        catch (IOException e)
        {
            LogHandler.error(e, "[ColorToName] Error while reading color_names_hex.txt");
            _hexNameMap.clear();
        }
    }


    /**
     *
     * @return
     */
    private void buildColors()
    {
        for(String hex : _hexNameMap.keySet())
        {
            Rgb rgb = hexToRgb(hex);
            Hsl hsl = rgbToHsl(rgb);
            _hexColorMap.put(hex, new Color(_hexNameMap.get(hex), rgb, hsl));
        }
    }


    /**
     *
     * @param r
     * @param g
     * @param b
     * @return
     */
    private String rgbToHex(int r, int g, int b)
    {
        return String.format("#%02x%02x%02x", r, g, b);
    }


    /**
     *
     * @param hex
     * @return
     */
    private Rgb hexToRgb(String hex)
    {
        if(hex.length() == 6)
            hex = "#" + hex;

        return new Rgb(Integer.valueOf(hex.substring(1, 3), 16), Integer.valueOf(hex.substring(3, 5), 16), Integer.valueOf(hex.substring(5, 7), 16));
    }


    /**
     *
     * @param hex
     * @return
     */
    private Hsl hexToHsl(String hex)
    {
        return rgbToHsl(hexToRgb(hex));
    }


    /**
     *
     * @param rgb
     * @return
     */
    private Hsl rgbToHsl(Rgb rgb)
    {
        final double r = (double)rgb.r/(double)255;
        double g = (double)rgb.g/(double)255;
        double b = (double)rgb.b/(double)255;

        double min = Math.min(r, Math.min(g, b));
        double max = Math.max(r, Math.max(g, b));

        double h, s, l = (min + max) / 2;

        if(max == min)
        {
            h = s = 0;
        }
        else
        {
            double delta = max - min;

            s = l > 0.5 ? (2 - max - min) : delta / (max + min);

            h = 0;
            {
                if (max == r) h = (g - b) / delta + (g < b ? 6 : 0);
                else if (max == g) h = (b - r) / delta + 2;
                else if (max == b) h = (r - g) / delta + 4;
            }
            h /= 6;
        }

        return new Hsl((int)(h*255), (int)(s*255), (int)(l*255));
    }


    public String tryGetNameForHex(String hex) throws CssSuiteException
    {
        Rgb rgb = hexToRgb(hex);
        return tryGetNameForRgb(rgb.r, rgb.g, rgb.b);
    }


    /**
     *
     * @param r
     * @param g
     * @param b
     * @return
     * @throws CssSuiteException
     */
    public String tryGetNameForRgb(int r, int g, int b) throws CssSuiteException
    {
        String color = rgbToHex(r, g, b);
        color = color.toUpperCase();

        if(color.length() < 3 || color.length() > 7)
        {
            throw new CssSuiteException("Invalid color: rgb '(%s, %s, %s)' hex '%s'", r, g, b, color);
        }

        if(color.length() % 3 == 0)
        {
            color = "#" + color;
        }

        if(color.length() == 4)
        {
            color = "#" + color.substring(1, 1) + color.substring(1, 1) + color.substring(2, 1) + color.substring(2, 1) + color.substring(3, 1) + color.substring(3, 1);
        }

        String key = color.replaceFirst("#","");
        if(_hexColorMap.containsKey(key))
            return _hexColorMap.get(key).name;

        Hsl hsl = hexToHsl(color);

        double ndf1;
        double ndf2;
        double ndf;
        double df = -1;

        String nearest = "";
        for(String hex : _hexColorMap.keySet())
        {
            Color option = _hexColorMap.get(hex);

            ndf1 = Math.pow(r - option.rgb.r, 2) + Math.pow(g - option.rgb.g, 2) + Math.pow(b - option.rgb.b, 2);
            ndf2 = Math.pow(hsl.h - option.hsl.h, 2) + Math.pow(hsl.s - option.hsl.s, 2) + Math.pow(hsl.l - option.hsl.l, 2);
            ndf = ndf1 + ndf2 * 2;
            if(df < 0 || df > ndf)
            {
                df = ndf;
                nearest = hex;
            }
        }

        if(nearest.isEmpty())
        {
            throw new CssSuiteException("Invalid color: rgb '(%s, %s, %s)' hex '%s'", r, g, b, color);
        }

        return _hexColorMap.get(nearest).name;
    }
}


