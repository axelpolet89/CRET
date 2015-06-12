package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.util.FileHelper;
import com.steadystate.css.parser.media.MediaQuery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by axel on 5/17/2015.
 */
public class CssWriter
{
    public void Generate(File file, List<MCssRule> rules) throws IOException, URISyntaxException
    {
        LogHandler.info("Generating CSS code for file '%s'...", file.getPath().replace("%", "-PERC-"));

        Collections.sort(rules, new Comparator<MCssRule>() {
            @Override
            public int compare(MCssRule mCssRule, MCssRule t1) {
                return Integer.compare(mCssRule.GetLocator().getLineNumber(), t1.GetLocator().getLineNumber());
            }
        });


        FileWriter writer = new FileWriter(file);
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

        LogHandler.info("[CssWriter] CSS code generation successful!");

        writer.flush();
        writer.close();
    }
}