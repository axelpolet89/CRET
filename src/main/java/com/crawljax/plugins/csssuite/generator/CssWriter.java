package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MCssRuleBase;
import com.crawljax.plugins.csssuite.util.FileHelper;
import com.steadystate.css.parser.media.MediaQuery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by axel on 5/17/2015.
 */
public class CssWriter
{
    public void Generate(File file, MCssFile mCssFile) throws IOException, URISyntaxException
    {
        LogHandler.info("Generating CSS code for file '%s'...", file.getPath().replace("%", "-PERC-"));

        List<MCssRuleBase> rules = mCssFile.GetAllRules().stream().filter(r -> !r.IsEmpty()).collect(Collectors.toList());

        Collections.sort(rules, new Comparator<MCssRuleBase>() {
            @Override
            public int compare(MCssRuleBase m1, MCssRuleBase m2) {
                if(m1.GetLineNumber() == m2.GetLineNumber())
                {
                    return Integer.compare(m1.GetColumnNumber(), m2.GetColumnNumber());
                }
                return Integer.compare(m1.GetLineNumber(), m2.GetLineNumber());
            }
        });


        FileWriter writer = new FileWriter(file);
        List<MediaQuery> currentMedia = new ArrayList<>();

        for (MCssRuleBase rule : rules)
        {
            if(rule.GetMediaQueries().size() > 0)
            {
                List<MediaQuery> media = rule.GetMediaQueries();

                if(currentMedia.containsAll(media) && media.containsAll(currentMedia))
                {
                    writer.write(rule.Print());
                }
                else
                {
                    if(!currentMedia.isEmpty())
                        writer.write("\n}\n");

                    currentMedia = new ArrayList<>(media);

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
                    currentMedia = new ArrayList<>();
                }

                writer.write(rule.Print());
            }
        }

        LogHandler.info("[CssWriter] CSS code generation successful!");

        writer.flush();
        writer.close();
    }
}