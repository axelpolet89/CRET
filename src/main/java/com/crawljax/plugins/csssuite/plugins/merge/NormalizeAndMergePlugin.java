package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.interfaces.ICssTransformer;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.*;


/**
 * Created by axel on 6/8/2015.
 *
 * This class is responsible for merging split-up properties into their shorthand equivalents
 */
public class NormalizeAndMergePlugin implements ICssTransformer
{
    private Map<MDeclaration, MSelector> _propSelMap = new HashMap<>();

    @Override
    public void getStatistics(SuiteStringBuilder builder, String prefix)
    {
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[CssMergeNormalizer] Start normalization of properties for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).GetRules())
            {
                for(MSelector mSelector : mRule.GetSelectors())
                {
                    _propSelMap.clear();
                    for(MDeclaration mDeclaration : mSelector.GetDeclarations())
                    {
                        _propSelMap.put(mDeclaration, mSelector);
                    }

                    MergePropertiesToShorthand(mSelector);

                    //sort properties again
                    mSelector.GetDeclarations().sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));
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
        List<MDeclaration> newProperties = new ArrayList<>();
        List<MDeclaration> properties = mSelector.GetDeclarations();

        List<MDeclaration> margins = new ArrayList<>();
        List<MDeclaration> paddings = new ArrayList<>();
        List<MDeclaration> borderRadii = new ArrayList<>();
        List<MDeclaration> border = new ArrayList<>();
        List<MDeclaration> borderTop = new ArrayList<>();
        List<MDeclaration> borderRight = new ArrayList<>();
        List<MDeclaration> borderBottom = new ArrayList<>();
        List<MDeclaration> borderLeft = new ArrayList<>();
        List<MDeclaration> outline = new ArrayList<>();
        List<MDeclaration> background = new ArrayList<>();

        Set<MDeclaration> borderStyles = new HashSet<>();
        Set<MDeclaration> borderColors = new HashSet<>();
        Set<MDeclaration> borderWidths = new HashSet<>();

        for(int i = 0; i < properties.size(); i++)
        {
            MDeclaration mDeclaration = properties.get(i);

            if(mDeclaration.IsIgnored())
            {
                newProperties.add(mDeclaration);
                continue;
            }

            final String name = mDeclaration.GetName();

            if(name.contains("margin"))
            {
                margins.add(mDeclaration);
            }
            else if (name.contains("padding"))
            {
                paddings.add(mDeclaration);
            }
            else if(name.contains("border"))
            {
                if(name.contains("radius"))
                {
                    borderRadii.add(mDeclaration);
                }
                else if(name.contains("top"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mDeclaration);
                    else if(name.contains("width"))
                        borderWidths.add(mDeclaration);
                    else
                        borderColors.add(mDeclaration);

                    borderTop.add(mDeclaration);
                }
                else if (name.contains("right"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mDeclaration);
                    else if(name.contains("width"))
                        borderWidths.add(mDeclaration);
                    else
                        borderColors.add(mDeclaration);

                    borderRight.add(mDeclaration);
                }
                else if (name.contains("bottom"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mDeclaration);
                    else if(name.contains("width"))
                        borderWidths.add(mDeclaration);
                    else
                        borderColors.add(mDeclaration);

                    borderBottom.add(mDeclaration);
                }
                else if (name.contains("left"))
                {
                    if(name.contains("style"))
                        borderStyles.add(mDeclaration);
                    else if(name.contains("width"))
                        borderWidths.add(mDeclaration);
                    else
                        borderColors.add(mDeclaration);

                    borderLeft.add(mDeclaration);
                }
                else
                {
                    border.add(mDeclaration);
                }
            }
            else if(name.contains("outline"))
            {
                outline.add(mDeclaration);
            }
            else if(name.contains("background"))
            {
                background.add(mDeclaration);
            }
            else
            {
                newProperties.add(mDeclaration);
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

        mSelector.SetNewDeclarations(newProperties);
    }


    /**
     *
     * @param properties
     * @param merger
     * @return
     */
    private List<MDeclaration> MergeBoxProperties(List<MDeclaration> properties, MergerBase merger)
    {
        if(properties.size() == 0)
        {
            return new ArrayList<>();
        }

        if(properties.size() == 4)
        {
            List<MDeclaration> result = new ArrayList<>();
            for (MDeclaration mDeclaration : properties)
            {
                try
                {
                    merger.Parse(mDeclaration.GetName(), mDeclaration.GetValue(), mDeclaration.IsImportant(), mDeclaration.GetOrder());
                }
                catch (Exception e)
                {
                    result.add(mDeclaration);
                    LogHandler.error(e, "[CssMergeNormalizer] Cannot parse single property %s in selector %s into shorthand equivalent, just add it to result", mDeclaration, _propSelMap.get(mDeclaration));
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
    private List<MDeclaration> MergeBorderProperties(List<MDeclaration> properties, MergerBase merger)
    {
        if (properties.size() == 0)
        {
            return new ArrayList<>();
        }

        List<MDeclaration> result = new ArrayList<>();

        for (MDeclaration mDeclaration : properties)
        {
            try
            {
                merger.Parse(mDeclaration.GetName(), mDeclaration.GetValue(), mDeclaration.IsImportant(), mDeclaration.GetOrder());
            }
            catch (Exception e)
            {
                result.add(mDeclaration);
                LogHandler.error(e, "[CssMergeNormalizer] Cannot parse single property %s for selector %s into shorthand equivalent, just add it to result", mDeclaration, _propSelMap.get(mDeclaration));
            }
        }

        result.addAll(merger.BuildMProperties());

        return result;
    }
}