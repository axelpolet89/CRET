package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;

import java.util.*;


/**
 * Created by axel on 6/8/2015.
 *
 * This class is responsible for merging split-up properties into their shorthand equivalents
 */
public class NormalizeAndMergePlugin implements ICssPostCrawlPlugin
{
    private Map<MProperty, MSelector> _propSelMap = new HashMap<>();

    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[CssMergeNormalizer] Start normalization of properties for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).GetRules())
            {
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    _propSelMap.clear();
                    for(MProperty mProperty : mSelector.GetProperties())
                    {
                        _propSelMap.put(mProperty, mSelector);
                    }

                    MergePropertiesToShorthand(mSelector);

                    //sort properties again
                    mSelector.GetProperties().sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));
                }
            }
        }

        return cssRules;
    }


    /**
     * Split any shorthand margin, padding, border, border-radius, outline and background property into parts
     * @param mSelector
     */
    private void MergePropertiesToShorthand(MSelector mSelector)
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

        Set<MProperty> borderStyles = new HashSet<>();
        Set<MProperty> borderColors = new HashSet<>();
        Set<MProperty> borderWidths = new HashSet<>();

        for(int i = 0; i < properties.size(); i++)
        {
            MProperty mProperty = properties.get(i);

            if(mProperty.IsIgnored())
            {
                newProperties.add(mProperty);
                continue;
            }

            final String name = mProperty.GetName();

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
                    if(name.contains("style"))
                        borderStyles.add(mProperty);
                    else if(name.contains("width"))
                        borderWidths.add(mProperty);
                    else
                        borderColors.add(mProperty);

                    borderTop.add(mProperty);
                }
                else if (name.contains("right"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mProperty);
                    else if(name.contains("width"))
                        borderWidths.add(mProperty);
                    else
                        borderColors.add(mProperty);

                    borderRight.add(mProperty);
                }
                else if (name.contains("bottom"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mProperty);
                    else if(name.contains("width"))
                        borderWidths.add(mProperty);
                    else
                        borderColors.add(mProperty);

                    borderBottom.add(mProperty);
                }
                else if (name.contains("left"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mProperty);
                    else if(name.contains("width"))
                        borderWidths.add(mProperty);
                    else
                        borderColors.add(mProperty);

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

        if(borderWidths.size() == (borderTop.size() + borderBottom.size() + borderLeft.size() + borderRight.size()))
        {
            newProperties.addAll(MergeBoxProperties(new ArrayList<>(borderWidths), new BoxMerger("border-width")));
        }
        else if (borderStyles.size() == (borderTop.size() + borderBottom.size() + borderLeft.size() + borderRight.size()))
        {
            newProperties.addAll(MergeBoxProperties(new ArrayList<>(borderStyles), new BoxMerger("border-style")));
        }
        else if (borderColors.size() == (borderTop.size() + borderBottom.size() + borderLeft.size() + borderRight.size()))
        {
            newProperties.addAll(MergeBoxProperties(new ArrayList<>(borderColors), new BoxMerger("border-color")));
        }
        else
        {
            newProperties.addAll(MergeBorderProperties(borderTop, new BorderSideMerger("border-top")));
            newProperties.addAll(MergeBorderProperties(borderRight, new BorderSideMerger("border-right")));
            newProperties.addAll(MergeBorderProperties(borderBottom, new BorderSideMerger("border-bottom")));
            newProperties.addAll(MergeBorderProperties(borderLeft, new BorderSideMerger("border-left")));
        }

        newProperties.addAll(MergeBoxProperties(borderRadii, new BorderRadiusMerger("border-radius")));
        newProperties.addAll(MergeBorderProperties(outline, new OutlineMerger("outline")));
        newProperties.addAll(MergeBorderProperties(background, new BackgroundMerger("background")));

        mSelector.SetNewProperties(newProperties);
    }


    /**
     *
     * @param properties
     * @param merger
     * @return
     */
    private List<MProperty> MergeBoxProperties(List<MProperty> properties, MergerBase merger)
    {
        if(properties.size() == 0)
        {
            return new ArrayList<>();
        }

        if(properties.size() == 4)
        {
            List<MProperty> result = new ArrayList<>();
            for (MProperty mProperty : properties)
            {
                try
                {
                    merger.Parse(mProperty.GetName(), mProperty.GetValue(), mProperty.IsImportant(), mProperty.GetOrder());
                }
                catch (Exception e)
                {
                    result.add(mProperty);
                    LogHandler.error(e, "[CssMergeNormalizer] Cannot parse single property %s in selector %s into shorthand equivalent, just add it to result", mProperty, _propSelMap.get(mProperty));
                }
            }

            result.addAll(merger.BuildMProperties());
            return result;
        }

        return properties;
    }


    /**
     *
     * @param properties
     * @param merger
     * @return
     */
    private List<MProperty> MergeBorderProperties(List<MProperty> properties, MergerBase merger)
    {
        if (properties.size() == 0)
        {
            return new ArrayList<>();
        }

        List<MProperty> result = new ArrayList<>();

        for (MProperty mProperty : properties)
        {
            try
            {
                merger.Parse(mProperty.GetName(), mProperty.GetValue(), mProperty.IsImportant(), mProperty.GetOrder());
            }
            catch (Exception e)
            {
                result.add(mProperty);
                LogHandler.error(e, "[CssMergeNormalizer] Cannot parse single property %s for selector %s into shorthand equivalent, just add it to result", mProperty, _propSelMap.get(mProperty));
            }
        }

        result.addAll(merger.BuildMProperties());

        return result;
    }
}