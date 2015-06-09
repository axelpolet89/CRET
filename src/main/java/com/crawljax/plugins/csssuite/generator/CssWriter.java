package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.plugins.sass.SassSelector;
import com.crawljax.plugins.csssuite.plugins.sass.SassTemplate;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

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


    public void GenerateSassFile(String fileName, List<SassSelector> selectors, List<SassTemplate> templates) throws URISyntaxException, IOException
    {
        File file = null;
        try
        {
            if (!fileName.contains(".css"))
            {
                LogHandler.info("[CssWriter] Styles not contained in a CSS file -> written to embedded_styles");
                file = CreateFile(fileName, "output\\sassfiles\\", "\\embedded_styles\\");
            }
            else
            {
                file = CreateFile(fileName, "output\\sassfiles\\", "");
            }
        }
        catch (IOException ex)
        {
            LogHandler.error(ex, "Error while creating CSS file for url '%s'", fileName.replace("%", "-PERC-"));
            return;
        }


        FileWriter writer = new FileWriter(file);

//        Collections.sort(rules, new Comparator<MCssRule>() {
//            @Override
//            public int compare(MCssRule mCssRule, MCssRule t1) {
//                return Integer.compare(mCssRule.GetLocator().getLineNumber(), t1.GetLocator().getLineNumber());
//            }
//        });

        for (int i = 0; i < templates.size(); i++)
        {
            writer.write(templates.get(i).Print());
        }

        Map<Integer, List<SassSelector>> rules = new LinkedHashMap<>();

        selectors.stream()
                .sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber()))
                .forEach((s) -> {
                    int ruleNumber = s.GetRuleNumber();
                    if (!rules.containsKey(ruleNumber))
                    {
                        rules.put(ruleNumber, new ArrayList<>());
                    }

                    rules.get(ruleNumber).add(s);
                });

        for (int rule : rules.keySet())
        {
            SuiteStringBuilder builder = new SuiteStringBuilder();
            String selectorGroup = "";

            List<SassSelector> ruleSelectors = rules.get(rule);
            for(int i = 0; i < ruleSelectors.size(); i++)
            {
                SassSelector sassSelector = ruleSelectors.get(i);
                selectorGroup += sassSelector.GetSelectorText();
                if(i < ruleSelectors.size() - 1)
                    selectorGroup += ", ";
            }
            builder.append(selectorGroup);
            builder.append("{");
            ruleSelectors.get(0).PrintContents(builder);
            builder.appendLine("}\n\n");

            writer.write(builder.toString());
        }

        LogHandler.info("[CssWriter] New rules written to output");

        writer.flush();
        writer.close();
    }


    private File CreateFile(String fileName, String root, String special) throws IOException, URISyntaxException
    {
        URI uri = new URI(fileName);
        if(uri.getAuthority() != null && uri.getScheme() != null)
        {
            root = root + uri.getAuthority().replace(uri.getScheme(), "");
        }
        root += special;

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