package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.sass.*;
import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassMixinBase;
import com.crawljax.plugins.csssuite.sass.variables.SassVariable;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by axel on 5/17/2015.
 */
public class SassWriter
{
    public File generateSassCode(File file, SassFile sassFile) throws URISyntaxException, IOException
    {
        LogHandler.info("Generating SCSS code for file '%s'...", file.getPath().replace("%", "-PERC-"));

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
                sv.print(builder);
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
                sv.print(builder);
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
                sv.print(builder);
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
                sv.print(builder);
                builder.append("\n");
            }
        }

        if(otherVarsSet)
        {
            builder.append("\n\n");
        }

        for (SassCloneMixin cloneMixin : sassFile.getCloneMixins())
        {
            cloneMixin.print(builder);
            builder.append("\n\n");
        }

        for (SassMixinBase mixin : sassFile.getMixins())
        {
            mixin.print(builder);
            builder.append("\n\n");
        }


        List<SassRuleBase> sassRules = sassFile.getRules().stream().sorted((r1, r2) -> Integer.compare(r1.getLineNumber(), r2.getLineNumber())).collect(Collectors.toList());
        for (int i = 0; i < sassRules.size(); i++)
        {
            SassRuleBase sr = sassRules.get(i);
            sr.print(builder, "");

            if(i < sassRules.size() - 1)
                builder.append("\n\n");
        }

        writer.write(builder.toString());

        LogHandler.info("[SassWriter] SCSS code generation successful!");

        writer.flush();
        writer.close();

        return file;
    }
}