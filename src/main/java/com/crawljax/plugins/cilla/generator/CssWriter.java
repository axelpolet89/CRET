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
        if(!fileName.contains(".css"))
            return;

        URI uri = new URI(fileName);

        String root = "output\\cssfiles\\" + uri.getAuthority().replace(uri.getScheme(), "");
        File file = new File(root + uri.getPath());
        File dir = new File((root + uri.getPath()).replace(file.getName(), ""));

        if(!dir.exists())
            dir.mkdirs();

        if(!file.exists())
            file.createNewFile();

        FileWriter writer = new FileWriter(file);

        Collections.sort(rules, new Comparator<MCssRule>() {
            @Override
            public int compare(MCssRule mCssRule, MCssRule t1) {
                return Integer.compare(mCssRule.GetLocator().getLineNumber(), t1.GetLocator().getLineNumber());
            }
        });

        int totalSelectors = 0;
        final int[] totalProperties = {0};

        for (MCssRule rule : rules)
        {
            totalSelectors += rule.GetSelectors().size();
             rule.GetSelectors().forEach((s) -> totalProperties[0] += s.GetProperties().size());
            writer.write(rule.Print());
        }

        System.out.println("File: " + fileName);
        System.out.println(String.format("# Selectors: %d", totalSelectors));
        System.out.println(String.format("# Properties: %d", totalProperties[0]));

        writer.flush();
        writer.close();
    }
}
