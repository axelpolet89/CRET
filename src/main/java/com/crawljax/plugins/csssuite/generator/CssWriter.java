package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.plugins.sass.*;
import com.crawljax.plugins.csssuite.plugins.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.plugins.sass.mixins.SassMixinBase;
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


    public void GenerateSassFile(String fileName, SassFile sassFile) throws URISyntaxException, IOException
    {
        File file = null;
        try
        {
            if (!fileName.contains(".css"))
            {
                LogHandler.info("[CssWriter] Styles not contained in a SCSS file -> written to embedded_styles");
                file = CreateFile(fileName, "output\\sassfiles\\", "\\embedded_styles\\");
            }
            else
            {
                file = CreateFile(fileName, "output\\sassfiles\\", "");
            }
        }
        catch (IOException ex)
        {
            LogHandler.error(ex, "Error while creating SCSS file for url '%s'", fileName.replace("%", "-PERC-"));
            return;
        }


        FileWriter writer = new FileWriter(file);

        List<SassVariable> colors = new ArrayList<>();
        List<SassVariable> alphaColors = new ArrayList<>();
        List<SassVariable> urls = new ArrayList<>();
        List<SassVariable> fonts = new ArrayList<>();

        for (SassVariable sv : sassFile.getVariables())
        {
            switch (sv.getVarType())
            {
                case COLOR:
                    colors.add(sv);
                    break;
                case ALPHA_COLOR:
                    alphaColors.add(sv);
                    break;
                case URL:
                    urls.add(sv);
                    break;
                case FONT:
                    fonts.add(sv);
                    break;
            }
        }

        SuiteStringBuilder builder = new SuiteStringBuilder();

        boolean otherVarsSet = false;
        if(colors.size() > 0)
        {
            otherVarsSet = true;
            builder.append("//colors\n");

            for(SassVariable sv : colors)
            {
                sv.Print(builder);
                builder.append("\n");
            }
        }

        if(alphaColors.size() > 0)
        {
            if(otherVarsSet)
            {
                builder.append("\n");
            }

            otherVarsSet = true;

            builder.append("//alpha colors\n");
            for(SassVariable sv : alphaColors)
            {
                sv.Print(builder);
                builder.append("\n");
            }
        }

        if(urls.size() > 0)
        {
            if(otherVarsSet)
            {
                builder.append("\n");
            }
            otherVarsSet = true;


            builder.append("//urls\n");
            for(SassVariable sv : urls)
            {
                sv.Print(builder);
                builder.append("\n");
            }
        }

        if(fonts.size() > 0)
        {
            if(otherVarsSet)
            {
                builder.append("\n");
            }

            builder.append("//fonts\n");
            for(SassVariable sv : fonts)
            {
                sv.Print(builder);
                builder.append("\n");
            }
        }

        if(otherVarsSet)
        {
            builder.append("\n\n");
        }

        for (SassCloneMixin cloneMixin : sassFile.getCloneMixins())
        {
            cloneMixin.Print(builder);
            builder.append("\n\n");
        }

        for (SassMixinBase mixin : sassFile.getMixins())
        {
            mixin.Print(builder);
            builder.append("\n\n");
        }


        List<SassRule> sassRules = sassFile.getRules();
        for (int i = 0; i < sassRules.size(); i++)
        {
            SassRule sr = sassRules.get(i);
            sr.Print(builder, "");

            if(i < sassRules.size() - 1)
                builder.append("\n\n");
        }

        List<SassMediaRule> mediaRules = sassFile.getMediaRules();

        if(mediaRules.size() > 0)
        {
            builder.append("\n\n");

            for (int i = 0; i < mediaRules.size(); i++)
            {
                SassMediaRule smr = mediaRules.get(i);
                smr.Print(builder);

                if (i < mediaRules.size() - 1)
                    builder.append("\n\n");
            }
        }

        writer.write(builder.toString());

        LogHandler.info("[CssWriter] New SCSS rules written!");

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