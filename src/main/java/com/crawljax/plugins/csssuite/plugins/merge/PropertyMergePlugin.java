package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by axel on 6/8/2015.
 *
 * This class is responsible for merging split-up properties into their shorthand equivalents
 */
public class PropertyMergePlugin implements ICssPostCrawlPlugin
{
    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[CssNormalizer] Start normalization of properties for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).GetRules())
            {
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    MergeShortHandDeclarations(mSelector);
                }
            }
        }

        return cssRules;
    }


    /**
     * Split any shorthand margin, padding, border, border-radius, outline and background property into parts
     * @param mSelector
     */
    private static void MergeShortHandDeclarations(MSelector mSelector)
    {
        List<MProperty> newProperties = new ArrayList<>();
        List<MProperty> properties = mSelector.GetProperties();

        List<MProperty> margins = new ArrayList<>();
        List<MProperty> paddings = new ArrayList<>();
        List<MProperty> borderRadii = new ArrayList<>();
        List<MProperty> border = new ArrayList<>();
        List<MProperty> borderTop = new ArrayList<>();
        List<MProperty> borderRight = new ArrayList<>();
        List<MProperty> borderBottom = new ArrayList<>();
        List<MProperty> borderLeft = new ArrayList<>();
        List<MProperty> outline = new ArrayList<>();
        List<MProperty> background = new ArrayList<>();

        for(int i = 0; i < properties.size(); i++)
        {
            MProperty mProperty = properties.get(i);

            if(mProperty.IsIgnored())
                continue;

            final String name = mProperty.GetName();
            final String value = mProperty.GetValue();
            final boolean isImportant = mProperty.IsImportant();

            if(name.contains("margin"))
            {
                margins.add(mProperty);
            }
            else if (name.contains("padding"))
            {
                paddings.add(mProperty);
            }
            else if(name.contains("border"))
            {
                if(name.contains("radius"))
                {
                    borderRadii.add(mProperty);
                }
                else if(name.contains("top"))
                {
                    borderTop.add(mProperty);
                }
                else if (name.contains("right"))
                {
                    borderRight.add(mProperty);
                }
                else if (name.contains("bottom"))
                {
                    borderBottom.add(mProperty);
                }
                else if (name.contains("left"))
                {
                    borderLeft.add(mProperty);
                }
                else
                {
                    border.add(mProperty);
                }
            }
            else if(name.contains("outline"))
            {
                outline.add(mProperty);
            }
            else if(name.contains("background"))
            {
                background.add(mProperty);
            }
            else
            {
                newProperties.add(mProperty);
            }
        }

        newProperties.addAll(MergeBoxProperties(margins, new BoxMerger("margin")));
        newProperties.addAll(MergeBoxProperties(paddings, new BoxMerger("padding")));
        newProperties.addAll(MergeBorderProperties(border, new BorderMerger("border")));
        newProperties.addAll(MergeBorderProperties(borderTop, new BorderMerger("border-top")));
        newProperties.addAll(MergeBorderProperties(borderRight, new BorderMerger("border-right")));
        newProperties.addAll(MergeBorderProperties(borderBottom, new BorderMerger("border-bottom")));
        newProperties.addAll(MergeBorderProperties(borderLeft, new BorderMerger("border-left")));
        newProperties.addAll(MergeBoxProperties(borderRadii, new BorderRadiusMerger("border-radius")));
        newProperties.addAll(MergeBorderProperties(outline, new OutlineMerger("outline")));
        newProperties.addAll(MergeBorderProperties(background, new BackgroundMerger("background")));

        mSelector.ReplaceProperties(newProperties);
    }


    /**
     *
     * @param properties
     * @param merger
     * @return
     */
    private static List<MProperty> MergeBoxProperties(List<MProperty> properties, MergerBase merger)
    {
        if(properties.size() == 0)
        {
            return new ArrayList<>();
        }

        if(properties.size() == 4)
        {
            for (MProperty property : properties)
            {
                try
                {
                    merger.Parse(property.GetName(), property.GetValue(), property.IsImportant());
                }
                catch (CssSuiteException e)
                {
                    LogHandler.error(e, "Cannot parse single property %s into shorthand", property);
                }
            }

            return merger.BuildMProperties();
        }

        return properties;
    }


    /**
     *
     * @param properties
     * @param merger
     * @return
     */
    private static List<MProperty> MergeBorderProperties(List<MProperty> properties, MergerBase merger)
    {
        if (properties.size() == 0)
        {
            return new ArrayList<>();
        }

        if (properties.size() == 1)
        {
            return properties;
        }

        for (MProperty property : properties)
        {
            try
            {
                merger.Parse(property.GetName(), property.GetValue(), property.IsImportant());
            }
            catch (CssSuiteException e)
            {
                LogHandler.error(e, "Cannot parse single property %s into shorthand", property);
            }
        }

        return merger.BuildMProperties();
    }
}