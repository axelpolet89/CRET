package com.crawljax.plugins.cret.colors;

import com.crawljax.plugins.cret.LogHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/9/2015.
 *
 * Transforms browser color names (X11 color scheme) into its hexadecimal representation.
 * Uses a table of X11 colors mapped to hexadecimal colors from https://developer.mozilla.org/en-US/docs/Web/CSS/color_value
 *
 */
public class BrowserColorParser
{
    private final Map<String, String> _colorHexMap;

    public BrowserColorParser()
    {
        _colorHexMap = new HashMap<>();

        try
        {
            List<String> lines = Files.readAllLines(new File("./src/main/java/com/crawljax/plugins/cret/colors/color_names_browser.txt").toPath());

            for(String line : lines)
            {
                String[] parts = line.split(":");
                _colorHexMap.put(parts[0].trim(), parts[1].trim());
            }
        }
        catch (IOException e)
        {
            LogHandler.error(e, "[BrowserColorParser] Error while reading color_names_browser.txt");
            _colorHexMap.clear();
        }
    }

    /**
     * @param browserColor the browser color (X11)
     * @return an hexadecimal representation for the given browserColor, if it is a valid X11 browser color
     */
    public String tryParseColorToHex(String browserColor)
    {
        if(_colorHexMap.containsKey(browserColor))
        {
            return _colorHexMap.get(browserColor);
        }

        return browserColor;
    }
}


