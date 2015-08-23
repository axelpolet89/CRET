package com.crawljax.plugins.cret.plugins.merge;

import com.crawljax.plugins.cret.CssSuiteException;
import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.interfaces.ICssTransformer;
import com.crawljax.plugins.cret.plugins.matcher.MatchedElements;
import com.crawljax.plugins.cret.util.CretStringBuilder;

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
    public void getStatistics(CretStringBuilder builder, String prefix)
    {
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for (String file : cssRules.keySet())
        {
            LogHandler.info("[NormalizeAndMerge] Start normalization of declarations for file '%s'", file);

            for(MCssRule mRule : cssRules.get(file).getRules())
            {
                for(MSelector mSelector : mRule.getSelectors())
                {
                    _propSelMap.clear();
                    for(MDeclaration mDeclaration : mSelector.getDeclarations())
                    {
                        _propSelMap.put(mDeclaration, mSelector);
                    }

                    mergeDeclarationsToShorthand(mSelector);

                    //sort properties again
                    mSelector.getDeclarations().sort((p1, p2) -> Integer.compare(p1.getOrder(), p2.getOrder()));
                }
            }
        }

        return cssRules;
    }


    /**
     * Split any shorthand margin, padding, border, border-radius, outline and background declaration into parts
     * @param mSelector
     */
    private void mergeDeclarationsToShorthand(MSelector mSelector)
    {
        List<MDeclaration> newDeclarations = new ArrayList<>();
        List<MDeclaration> declarations = mSelector.getDeclarations();

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

        for(int i = 0; i < declarations.size(); i++)
        {
            MDeclaration mDeclaration = declarations.get(i);

            if(mDeclaration.isIgnored())
            {
                newDeclarations.add(mDeclaration);
                continue;
            }

            final String name = mDeclaration.getName();

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
            else if(name.contains("background-"))
            {
                background.add(mDeclaration);
            }
            else
            {
                newDeclarations.add(mDeclaration);
            }
        }

        newDeclarations.addAll(mergeBoxDeclarations(margins, new BoxMerger("margin")));
        newDeclarations.addAll(mergeBoxDeclarations(paddings, new BoxMerger("padding")));
        newDeclarations.addAll(mergeBorderDeclarations(border, new BorderMerger("border")));

        if(borderWidths.size() == (borderTop.size() + borderBottom.size() + borderLeft.size() + borderRight.size()))
        {
            newDeclarations.addAll(mergeBoxDeclarations(new ArrayList<>(borderWidths), new BoxMerger("border-width")));
        }
        else if (borderStyles.size() == (borderTop.size() + borderBottom.size() + borderLeft.size() + borderRight.size()))
        {
            newDeclarations.addAll(mergeBoxDeclarations(new ArrayList<>(borderStyles), new BoxMerger("border-style")));
        }
        else if (borderColors.size() == (borderTop.size() + borderBottom.size() + borderLeft.size() + borderRight.size()))
        {
            newDeclarations.addAll(mergeBoxDeclarations(new ArrayList<>(borderColors), new BoxMerger("border-color")));
        }
        else
        {
            newDeclarations.addAll(mergeBorderDeclarations(borderTop, new BorderSideMerger("border-top")));
            newDeclarations.addAll(mergeBorderDeclarations(borderRight, new BorderSideMerger("border-right")));
            newDeclarations.addAll(mergeBorderDeclarations(borderBottom, new BorderSideMerger("border-bottom")));
            newDeclarations.addAll(mergeBorderDeclarations(borderLeft, new BorderSideMerger("border-left")));
        }

        newDeclarations.addAll(mergeBoxDeclarations(borderRadii, new BorderRadiusMerger("border-radius")));
        newDeclarations.addAll(mergeBorderDeclarations(outline, new OutlineMerger("outline")));
        newDeclarations.addAll(mergeBorderDeclarations(background, new BackgroundMerger("background")));

        mSelector.setNewDeclarations(newDeclarations);
    }


    /**
     *
     * @param declarations
     * @param merger
     * @return
     */
    private List<MDeclaration> mergeBoxDeclarations(List<MDeclaration> declarations, MergerBase merger)
    {
        if(declarations.size() == 0)
        {
            return new ArrayList<>();
        }

        if(declarations.size() == 4)
        {
            List<MDeclaration> result = new ArrayList<>();
            for (MDeclaration mDeclaration : declarations)
            {
                try
                {
                    merger.parse(mDeclaration.getName(), mDeclaration.getValue(), mDeclaration.isImportant(), mDeclaration.getOrder());
                }
                catch (CssSuiteException e)
                {
                    result.add(mDeclaration);
                    LogHandler.debug("[NormalizeAndMerge] Cannot parse single declaration %s in selector %s into its shorthand equivalent -\n%s",
                            mDeclaration, _propSelMap.get(mDeclaration), e.getMessage());
                }
                catch(Exception e)
                {
                    result.add(mDeclaration);
                    LogHandler.error(e, "[NormalizeAndMerge] Cannot parse single declaration %s in selector %s into its shorthand equivalent",
                            mDeclaration, _propSelMap.get(mDeclaration));
                }
            }

            result.addAll(merger.buildMDeclarations());
            return result;
        }

        return declarations;
    }


    /**
     *
     * @param declarations
     * @param merger
     * @return
     */
    private List<MDeclaration> mergeBorderDeclarations(List<MDeclaration> declarations, MergerBase merger)
    {
        if (declarations.size() == 0)
        {
            return new ArrayList<>();
        }

        List<MDeclaration> result = new ArrayList<>();

        for (MDeclaration mDeclaration : declarations)
        {
            try
            {
                merger.parse(mDeclaration.getName(), mDeclaration.getValue(), mDeclaration.isImportant(), mDeclaration.getOrder());
            }
            catch (CssSuiteException e)
            {
                result.add(mDeclaration);
                LogHandler.debug("[NormalizeAndMerge] Cannot parse single declaration %s in selector %s into its shorthand equivalent -\n%s",
                        mDeclaration, _propSelMap.get(mDeclaration), e.getMessage());
            }
            catch(Exception e)
            {
                result.add(mDeclaration);
                LogHandler.error(e, "[NormalizeAndMerge] Cannot parse single declaration %s in selector %s into its shorthand equivalent",
                        mDeclaration, _propSelMap.get(mDeclaration));
            }
        }

        result.addAll(merger.buildMDeclarations());

        return result;
    }
}