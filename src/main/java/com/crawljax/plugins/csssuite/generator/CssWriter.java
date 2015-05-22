package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssRule;

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
                file = CreateFile(fileName, "output\\cssfiles\\", "\\embedded_styles\\");
            }
            else
            {
                file = CreateFile(fileName, "output\\cssfiles\\", "");
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

        for (MCssRule rule : rules)
        {
            writer.write(rule.Print());
        }

        LogHandler.info("[CssWriter] New rules written to output");

        writer.flush();
        writer.close();
    }


    private File CreateFile(String fileName, String root, String special) throws IOException, URISyntaxException
    {
        URI uri = new URI(fileName);
        root = root + uri.getAuthority().replace(uri.getScheme(), "") + special;

        String path = uri.getPath();
        if(path.equals("/"))
            path = "root/";

        File file = new File(root + path);
        File dir = new File((root + path).replace(file.getName(), ""));

        if(!dir.exists())
            dir.mkdirs();

        try
        {
            if (!file.exists())
                file.createNewFile();
        }
        catch(IOException ex)
        {
            LogHandler.error("Error in creating new file '%s'\nwith name '%s'", file, file.getName());
            throw(ex);
        }

        return file;
    }
}