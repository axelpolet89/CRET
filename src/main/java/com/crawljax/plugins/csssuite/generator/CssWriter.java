package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.util.FileHelper;
import com.steadystate.css.parser.media.MediaQuery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by axel on 5/17/2015.
 */
public class CssWriter
{
    public void Generate(String fileName, List<MCssRule> rules) throws IOException, URISyntaxException
    {
        int totalSelectors = 0;
        final int[] totalProperties = {0};

        for (MCssRule rule : rules)
        {
            totalSelectors += rule.GetSelectors().size();
            rule.GetSelectors().forEach((s) -> totalProperties[0] += s.GetProperties().size());
        }

        LogHandler.info("[CssWriter] File: " + fileName);
        LogHandler.info("[CssWriter] # Selectors: %d", totalSelectors);
        LogHandler.info("[CssWriter] # Properties: %d", totalProperties[0]);

        File file = null;
        try
        {
            if(!fileName.contains(".css"))
            {
                LogHandler.info("[CssWriter] Styles not contained in a CSS file -> written to embedded_styles");
                file = FileHelper.CreateFileAndDirs(fileName, "output\\cssfiles\\", "\\embedded_styles\\");
            }
            else
            {
                file = FileHelper.CreateFileAndDirs(fileName, "output\\cssfiles\\", "");
            }
        }
        catch (IOException ex)
        {
            LogHandler.error(ex, "Error while creating CSS file for url '%s'", fileName.replace("%", "-PERC-"));
            return;
        }


        FileWriter writer = new FileWriter(file);

        Collections.sort(rules, new Comparator<MCssRule>() {
            @Override
            public int compare(MCssRule mCssRule, MCssRule t1) {
                return Integer.compare(mCssRule.GetLocator().getLineNumber(), t1.GetLocator().getLineNumber());
            }
        });

        List<MediaQuery> currentMedia = new ArrayList<>();

        for (MCssRule rule : rules)
        {
            if(rule.GetSelectors().size() > 0 && rule.GetSelectors().stream().allMatch(s -> s.GetMediaQueries().size() > 0))
            {
                List<MediaQuery> media = rule.GetSelectors().get(0).GetMediaQueries();

                if(currentMedia.containsAll(media) && media.containsAll(currentMedia))
                {
                    writer.write(rule.Print());
                }
                else
                {
                    if(!currentMedia.isEmpty())
                        writer.write("\n}\n");

                    currentMedia = media;

                    String mediaQueryText = "@media";

                    for(MediaQuery mq : media)
                    {
                        mediaQueryText += " " + mq.toString();
                    }

                    mediaQueryText += "{\n";

                    writer.write(mediaQueryText);

                    writer.write(rule.Print());
                }
            }
            else
            {
                if(!currentMedia.isEmpty())
                {
                    writer.write("\n}\n");
                    currentMedia.clear();
                }

                writer.write(rule.Print());
            }
        }

        LogHandler.info("[CssWriter] New rules written to output");

        writer.flush();
        writer.close();
    }
}