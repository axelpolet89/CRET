package com.crawljax.plugins.cret.generation;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRuleBase;
import com.steadystate.css.parser.media.MediaQuery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 5/17/2015.
 */
public class CssWriter
{
    public void generateCssCode(File file, MCssFile mCssFile) throws IOException, URISyntaxException
    {
        LogHandler.info("Generating CSS code for file '%s'...", file.getPath().replace("%", "-PERC-"));

        List<MCssRuleBase> rules = mCssFile.getAllRules().stream().filter(r -> !r.isEmpty()).collect(Collectors.toList());

        Collections.sort(rules, new Comparator<MCssRuleBase>() {
            @Override
            public int compare(MCssRuleBase m1, MCssRuleBase m2) {
                if(m1.getLineNumber() == m2.getLineNumber())
                {
                    return Integer.compare(m1.getColumnNumber(), m2.getColumnNumber());
                }
                return Integer.compare(m1.getLineNumber(), m2.getLineNumber());
            }
        });


        FileWriter writer = new FileWriter(file);
        List<MediaQuery> currentMedia = new ArrayList<>();

        for (MCssRuleBase rule : rules)
        {
            if(rule.getMediaQueries().size() > 0)
            {
                List<MediaQuery> media = rule.getMediaQueries();

                if(currentMedia.containsAll(media) && media.containsAll(currentMedia))
                {
                    writer.write(rule.print());
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

                    writer.write(rule.print());
                }
            }
            else
            {
                if(!currentMedia.isEmpty())
                {
                    writer.write("\n}\n");
                    currentMedia = new ArrayList<>();
                }

                writer.write(rule.print());
            }
        }

        LogHandler.info("[CssWriter] CSS code generation successful!");

        writer.flush();
        writer.close();
    }
}