package com.crawljax.plugins.cilla.generator;

import com.crawljax.plugins.cilla.data.MCssRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by axel on 5/17/2015.
 */
public class CssWriter
{

    public void Generate(String fileName, List<MCssRule> rules) throws IOException, URISyntaxException
    {
        if(fileName.contains(".html"))
            return;

        URI uri = new URI(fileName);
        String name = new File(uri.getPath()).getName();

        File file = new File("output\\cssfiles\\" + name);

        FileWriter writer = new FileWriter(file);

        Collections.sort(rules, new Comparator<MCssRule>() {
            @Override
            public int compare(MCssRule mCssRule, MCssRule t1) {
                return Integer.compare(mCssRule.GetLocator().getLineNumber(), t1.GetLocator().getLineNumber());
            }
        });

        for (MCssRule rule : rules)
        {
            writer.write(rule.Print());
        }

        writer.flush();
        writer.close();
    }
}
