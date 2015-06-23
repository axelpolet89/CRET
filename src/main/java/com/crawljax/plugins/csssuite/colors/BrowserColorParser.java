package com.crawljax.plugins.csssuite.colors;

import com.crawljax.plugins.csssuite.LogHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by axel on 6/9/2015.
 *
 * Enables transformation of a browser color name (X11 color scheme) into its rgb representation.
 */
public class BrowserColorParser
{
    private final Map<String, String> _colorHexMap;

    public BrowserColorParser()
    {
        _colorHexMap = new HashMap<>();

        try
        {
            List<String> lines = Files.readAllLines(new File("./src/main/java/com/crawljax/plugins/csssuite/colors/color_names_browser.txt").toPath());

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
     * @param browserColor the browser color (text)
     * @return an rgb representation for the given browserColor, if it is a valid X11 browser color
     */
    public String TryParseToRgb(String browserColor)
    {
        if(_colorHexMap.containsKey(browserColor))
        {
            return HexToRgb(_colorHexMap.get(browserColor));
        }

        return browserColor;
    }


    public Set GetBrowserColors()
    {
        return _colorHexMap.keySet();
    }

    /**
     * Return a RGB string representation for the given hexadecimal string value
     * @param hex
     * @return
     */
    private String HexToRgb(String hex)
    {
        if(hex.length() == 6)
        {
            hex = "#" + hex;
        }

        return String.format("rgb(%d, %d, %d)", Integer.valueOf(hex.substring(1, 3), 16), Integer.valueOf(hex.substring(3, 5), 16), Integer.valueOf(hex.substring(5, 7), 16));
    }
}


