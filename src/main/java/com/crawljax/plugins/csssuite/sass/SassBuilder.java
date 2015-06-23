package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.colors.BrowserColorParser;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.sass.clonedetection.CloneDetector;
import com.crawljax.plugins.csssuite.colors.ColorNameFinder;
import com.crawljax.plugins.csssuite.sass.mixins.SassBoxMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassMixinBase;
import com.crawljax.plugins.csssuite.sass.variables.SassVarType;
import com.crawljax.plugins.csssuite.sass.variables.SassVariable;
import com.crawljax.plugins.csssuite.util.ValueFinderHelper;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/8/2015.
 */
public class SassBuilder
{
    private final int minPropCount = 2;

    public Map<String, SassFile> CssToSass(Map<String, MCssFile> cssFiles)
    {
        Map<String, SassFile> sassFiles = new HashMap<>();
        CloneDetector cd = new CloneDetector();

        for(String fileName : cssFiles.keySet())
        {
            List<MCssRule> cssRules = cssFiles.get(fileName).GetRules();

            // copy all MSelectors, so we won't affect the original rules
            List<MSelector> allSelectors = new ArrayList<>();
            for(MCssRule rule : cssRules)
            {
                allSelectors.addAll(rule.GetSelectors().stream().map(selector -> new MSelector((selector))).collect(Collectors.toList()));
            }

            LogHandler.debug("[SassGenerator] Generate SASS for ignored CSS rules...");


            LogHandler.debug("[SassGenerator] Generate SASS variables...");
            List<SassVariable> sassVariables = GenerateVariables(allSelectors);

            LogHandler.debug("[SassGeneratpr] Generate SASS mixins...");
            List<SassCloneMixin> validMixins = ProcessAndFilterClones(cd.GenerateMixins(allSelectors));
            LogHandler.debug("[SassGenerator] Found %d templates that apply to %d or more properties and are efficient for file %s", validMixins.size(), minPropCount, fileName);

            LogHandler.debug("[SassGenerator] Generate SASS selectors...");
            List<SassSelector> sassSelectors = GenerateSassSelectors(allSelectors, validMixins);

            LogHandler.debug("[SassGenerator] Generate SASS convenience mixins...");
            List<SassMixinBase> sassMixins = GenerateConvenienceMixins(sassSelectors);

            LogHandler.debug("[SassGenerator] Generate SASS rules...");
            List<SassRuleBase> sassRules = GenerateSassRules(sassSelectors, cssFiles.get(fileName).GetMediaRules(), cssFiles.get(fileName).GetIgnoredRules());

            sassFiles.put(fileName, new SassFile(sassVariables, validMixins, sassMixins, sassRules));
        }

        return sassFiles;
    }


    private List<SassCloneMixin> ProcessAndFilterClones(List<SassCloneMixin> mixins)
    {
        List<SassCloneMixin> validMixins = new ArrayList<>();

        for (SassCloneMixin mixin : mixins)
        {
            SortMixinValues(mixin);

            List<MProperty> templateProps = mixin.GetProperties();
            int numberOfProps = templateProps.size();

            // if the total number of properties exceeds minimum property threshold, continue
            if (numberOfProps >= minPropCount)
            {
                // number of lines that mixin will add to output file
                int mixinSize = templateProps.size();

                int count = 0;
                int lineNumber = 0;
                for(MSelector mSelector : mixin.GetRelatedSelectors())
                {
                    if(mSelector.GetRuleNumber() != lineNumber)
                    {
                        count += numberOfProps - 1;
                        lineNumber = mSelector.GetRuleNumber();
                    }
                }

                // if the total number of properties saved exceeds the number of properties added to the mixin, include the mixin
                if(count >= mixinSize)
                {
                    validMixins.add(mixin);
                }
                else
                {
                    RestoreCloneMixin(mixin);
                }
            }
            else
            {
                RestoreCloneMixin(mixin);
            }
        }

        for (int i = 0; i < validMixins.size(); i++)
        {
            validMixins.get(i).SetNumber(i + 1);
        }

        return validMixins;
    }


    private void SortMixinValues(SassCloneMixin mixin)
    {
        mixin.GetRelatedSelectors().sort((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber()));
        mixin.GetProperties().sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));
    }


    private void RestoreCloneMixin(SassCloneMixin mixin)
    {
        List<MProperty> mixinProps = mixin.GetProperties();

        for (MSelector mSelector : mixin.GetRelatedSelectors())
        {
            mixinProps.forEach(mixinProp -> mSelector.AddProperty(new MProperty(mixinProp.GetName(), mixinProp.GetValue(),
                                                                    mixinProp.IsImportant(), mixinProp.IsEffective(),
                                                                    mixin.getPropertyOrderForSelector(mSelector, mixinProp))));
        }
    }


    private List<SassMixinBase> GenerateConvenienceMixins(List<SassSelector> allSelectors)
    {
        List<SassMixinBase> mixins = new ArrayList<>();

        SassBoxMixin padding = new SassBoxMixin("padding");
        SassBoxMixin margin = new SassBoxMixin("margin");

        boolean pUsed = false;
        boolean mUsed = false;

        for(SassSelector sassSelector : allSelectors)
        {
            List<MProperty> paddings = sassSelector.GetProperties().stream().filter(ms -> ms.GetName().contains("padding-")).collect(Collectors.toList());
            List<MProperty> margins = sassSelector.GetProperties().stream().filter(ms -> ms.GetName().contains("margin-")).collect(Collectors.toList());

            if(paddings.size() > 1)
            {
                sassSelector.AddInclude(padding.CreateMixinCall(paddings));
                sassSelector.RemoveProperties(paddings);

                pUsed = true;
            }

            if(margins.size() > 1)
            {
                sassSelector.AddInclude(margin.CreateMixinCall(margins));
                sassSelector.RemoveProperties(margins);

                mUsed = true;
            }
        }

        if(pUsed)
        {
            mixins.add(padding);
        }

        if(mUsed)
        {
            mixins.add(margin);
        }

        return mixins;
    }


    private List<SassSelector> GenerateSassSelectors(List<MSelector> selectors, List<SassCloneMixin> extensions)
    {
        List<SassSelector> results = new ArrayList<>();

        for(MSelector mSelector : selectors)
        {
            SassSelector ss = new SassSelector(mSelector);

            for(SassCloneMixin st : extensions)
            {
                for(MSelector related : st.GetRelatedSelectors())
                {
                    if(related == mSelector)
                    {
                        ss.AddCloneInclude(st);
                        break;
                    }
                }
            }

            results.add(ss);
        }

        return results;
    }

    private List<SassRuleBase> GenerateSassRules(List<SassSelector> sassSelectors, List<MCssMediaRule> mediaRules, List<MCssRuleBase> ignoredRules)
    {
        // group selectors by their line number, maintain order by using LinkedHashMap
        Map<Integer, List<SassSelector>> lineNoSelectorMap = new LinkedHashMap<>();
        sassSelectors.stream().filter(s -> s.GetMediaQueries().isEmpty())
                .sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber()))
                .forEach((s) -> {
                    int lineNumber = s.GetRuleNumber();
                    if (!lineNoSelectorMap.containsKey(lineNumber))
                    {
                        lineNoSelectorMap.put(lineNumber, new ArrayList<>());
                    }

                    lineNoSelectorMap.get(lineNumber).add(s);
                });

        List<SassRuleBase> sassRules = new ArrayList<>();

        // final merge, verify that properties previously contained in the same rule (e.g. equal line number)
        // do not have the same declarations (e.g. all sass declarations and regular properties
        // if they do, merge them into 1 SassRule
        for(int lineNumber : lineNoSelectorMap.keySet())
        {
            List<SassSelector> innerSelectors = lineNoSelectorMap.get(lineNumber);
            List<Integer> processedIdx = new ArrayList<>();
            for(int i = 0; i < innerSelectors.size(); i++)
            {
                if(processedIdx.contains(i))
                    continue;

                List<SassSelector> selectorsForRule = new ArrayList<>();
                SassSelector current = innerSelectors.get(i);
                processedIdx.add(i);
                selectorsForRule.add(current);

                for(int j = i + 1; j < innerSelectors.size(); j++)
                {
                    if(processedIdx.contains(j))
                        continue;

                    SassSelector other = innerSelectors.get(j);
                    if(current.HasEqualDeclarationsByText(other))
                    {
                        selectorsForRule.add(other);
                        processedIdx.add(j);
                    }
                }

                sassRules.add(new SassRule(lineNumber, selectorsForRule));
            }
        }

        for(MCssRuleBase ignoredRule : ignoredRules.stream().filter(r -> r.GetMediaQueries().isEmpty()).collect(Collectors.toList()))
        {
            sassRules.add(new SassIgnoredRule(ignoredRule.GetLineNumber(), ignoredRule.GetAbstractRule()));
        }

        // now process selectors held in media-queries
        List<SassSelector> mediaSelectors = sassSelectors.stream().filter(s -> s.GetMediaQueries().size() > 0).collect(Collectors.toList());

        for(MCssMediaRule mediaRule : mediaRules)
        {
            SassMediaRule sassMediaRule = RecursiveGenerateMediaRules(mediaRule, mediaSelectors, ignoredRules);
            if(sassMediaRule != null)
            {
                sassRules.add(sassMediaRule);
            }
        }

        return sassRules;
    }


    private SassMediaRule RecursiveGenerateMediaRules(MCssMediaRule mediaRule, List<SassSelector> selectors, List<MCssRuleBase> ignoredRules)
    {
        List<SassRuleBase> innerRules = new ArrayList<>();

        for(MCssRuleBase mRule : mediaRule.GetInnerRules())
        {
            if(mRule instanceof MCssMediaRule)
            {
                innerRules.add(RecursiveGenerateMediaRules((MCssMediaRule)mRule, selectors, ignoredRules));
            }
            else if (mRule instanceof MCssRule)
            {
                List<SassSelector> relatedSelectors = selectors.stream().filter(s -> s.GetParent().equals(mRule)).collect(Collectors.toList());
                if(!relatedSelectors.isEmpty())
                {
                    innerRules.add(new SassRule(mRule.GetLineNumber(), relatedSelectors));
                }
            }
            else
            {
                innerRules.add(new SassIgnoredRule(mRule.GetLineNumber(), mRule.GetAbstractRule()));
            }
        }

        if(innerRules.isEmpty())
        {
            return null;
        }

        return new SassMediaRule(mediaRule.GetLineNumber(), mediaRule.GetMediaQueries(), innerRules);
    }

    private List<SassMediaRule> GenerateMediaRules(List<SassRule> sassRules)
    {
        List<SassMediaRule> mediaRules = new ArrayList<>();
        Map<List<MediaQuery>, List<SassRuleBase>> mediaGroups = new LinkedHashMap<>(); // preserve order
        List<MediaQuery> currentMedia = new ArrayList<>();

        // find all sass rules that are contained inside one or more media-queries
        for(SassRule sassRule : sassRules)
        {
            currentMedia = sassRule.getMediaQueries();

            if(!mediaGroups.containsKey(currentMedia))
            {
                mediaGroups.put(currentMedia, new ArrayList<>());
            }

            mediaGroups.get(currentMedia).add(sassRule);
        }

        // remove any sass rule contained in a media rule from given list
        // create sass media rules from media groups
        for(List<MediaQuery> mediaQueries : mediaGroups.keySet())
        {
            if(mediaQueries.isEmpty())
                continue;

            List<SassRuleBase> rulesInMedia = mediaGroups.get(mediaQueries);

            // set media-query line number to be the first rule's line no minus 1
            int mediaLineNo = rulesInMedia.get(0).GetLineNumber() - 1;

            mediaRules.add(new SassMediaRule(mediaLineNo, mediaQueries, rulesInMedia));

            rulesInMedia.forEach(sassRules::remove);
        }

        return mediaRules;
    }

    private List<SassVariable> GenerateVariables(List<MSelector> sassSelectors)
    {
        List<SassVariable> variables = new ArrayList<>();
        Map<String, String> alreadyDefinedVars = new HashMap<>();

        ColorNameFinder ctn = new ColorNameFinder();
        BrowserColorParser bcp = new BrowserColorParser();

        // for identification of browser colors in shorthand properties
        Set browserColors = bcp.GetBrowserColors();

        for(MSelector sassSelector : sassSelectors)
        {
            for(MProperty mProperty : sassSelector.GetProperties())
            {
                SassVarType varType = null;
                String varName = "";
                String varValue = "";

                String origName = mProperty.GetName();
                String origValue = mProperty.GetValue();

                try
                {
                    if (origName.equals("font-family"))
                    {
                        varType = SassVarType.FONT;
                        varName = "font-stack";
                        varValue = origValue;
                    }
                    else if (origValue.contains("url"))
                    {
                        varType = SassVarType.URL;
                        varName = "url";
                        varValue = ValueFinderHelper.TryFindUrl(origValue);
                    }
                    else if (origValue.contains("rgba"))
                    {
                        String value = ValueFinderHelper.TryFindRgb(origValue);
                        String[] rgbParts = value.replaceFirst("rgba\\(", "").replaceFirst("\\)", "").split(",");

                        varType = SassVarType.ALPHA_COLOR;
                        varName = String.format("%s_%s", "alpha_color", ctn.TryGetNameForRgb(Integer.parseInt(rgbParts[0].trim()), Integer.parseInt(rgbParts[1].trim()), Integer.parseInt(rgbParts[2].trim())));
                        varValue = value;
                    }
                    else if (origValue.contains("rgb"))
                    {
                        String value = ValueFinderHelper.TryFindRgb(origValue);
                        String[] rgbParts = value.replaceFirst("rgb\\(", "").replaceFirst("\\)", "").split(",");

                        varType = SassVarType.COLOR;
                        varName = String.format("%s_%s", "color", ctn.TryGetNameForRgb(Integer.parseInt(rgbParts[0].trim()), Integer.parseInt(rgbParts[1].trim()), Integer.parseInt(rgbParts[2].trim())));
                        varValue = value;
                    }
                    else if (origName.contains("color"))
                    {
                        varType = SassVarType.COLOR;
                        varName = String.format("%s_%s", "color", origValue);
                        varValue = bcp.TryParseToRgb(origValue);

                        // update original value with it's rgb representation, so that final replace (with variable reference) will work
                        origValue = varValue;
                    }
                    else if (origName.equals("border") || origName.equals("background") || origName.equals("outline") || origName.equals("box-shadow") || origName.equals("text-shadow"))
                    {
                        String[] parts = origValue.split(" ");

                        for (String part : parts)
                        {
                            part = part.trim();
                            if (browserColors.contains(part))
                            {
                                varType = SassVarType.COLOR;
                                varName = String.format("%s_%s", "color", part);
                                varValue = bcp.TryParseToRgb(part);

                                // update original value with it's rgb representation, so that final replace (with variable reference) will work
                                origValue = origValue.replace(part, varValue);
                                break;
                            }
                        }
                    }

                    if (!varName.isEmpty() && !varValue.isEmpty())
                    {
                        // if varName already used, extend varName with an id
                        if (alreadyDefinedVars.containsKey(varName) && !alreadyDefinedVars.get(varName).equals(varValue))
                        {
                            int id = 1;
                            while (true)
                            {
                                String replace = String.format("%s_%d", varName, id);
                                if (!alreadyDefinedVars.containsKey(replace) || (alreadyDefinedVars.containsKey(replace) && alreadyDefinedVars.get(replace).equals(varValue)))
                                {
                                    varName = replace;
                                    break;
                                }
                                id++;
                            }
                        }

                        if(!alreadyDefinedVars.containsKey(varName))
                        {
                            SassVariable sv = new SassVariable(varType, varName, varValue, mProperty);
                            variables.add(sv);
                            alreadyDefinedVars.put(varName, varValue);
                        }

                        String escapedValue = varValue.replaceFirst("\\(", "\\\\(").replaceFirst("\\)", "\\\\)");
                        String escapedName = String.format("\\$%s", varName);
                        mProperty.SetNormalizedValue(origValue.replaceFirst(escapedValue, escapedName));
                    }
                }
                catch (Exception e)
                {
                    LogHandler.error(e, "[SassGenerator] Error occurred while creating SassVariable for property '%s' with value '%s' for selector '%s'",
                                        origName, origValue, sassSelector.GetSelectorText());
                }
            }
        }

        return variables;
    }
}