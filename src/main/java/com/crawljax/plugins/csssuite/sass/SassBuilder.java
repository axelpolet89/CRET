package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.sass.clonedetection.CloneDetector;
import com.crawljax.plugins.csssuite.sass.colors.ColorNameFinder;
import com.crawljax.plugins.csssuite.sass.mixins.SassBoxMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassMixinBase;
import com.crawljax.plugins.csssuite.sass.variables.SassVarType;
import com.crawljax.plugins.csssuite.sass.variables.SassVariable;
import com.crawljax.plugins.csssuite.util.ColorHelper;
import com.steadystate.css.parser.media.MediaQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/8/2015.
 */
public class SassBuilder
{
    private final int minPropCount = 2;

    public Map<String, SassFile> CssToSass(Map<String, MCssFile> cssRules)
    {
        Map<String, SassFile> sassFiles = new HashMap<>();
        CloneDetector cd = new CloneDetector();

        for(String fileName : cssRules.keySet())
        {
            List<MCssRule> rules = cssRules.get(fileName).GetRules();

            // copy all MSelectors, so we won't affect the original rules
            List<MSelector> allSelectors = new ArrayList<>();
            for(MCssRule rule : rules)
            {
                allSelectors.addAll(rule.GetSelectors().stream().map(selector -> new MSelector((selector))).collect(Collectors.toList()));
            }

            LogHandler.debug("[SassGenerator] Generate SASS variables...");
            List<SassVariable> sassVariables = GenerateVariables(allSelectors);

            LogHandler.debug("[SassGeneratpr] Generate SASS mixins...");
            List<SassCloneMixin> validMixins = FilterValidCloneMixins(cd.GenerateMixins(allSelectors));

            LogHandler.debug("[SassGenerator] Found %d templates that apply to %d or more properties and are efficient for file %s", validMixins.size(), minPropCount, fileName);

            for (int i = 0; i < validMixins.size(); i++)
            {
                validMixins.get(i).SetNumber(i + 1);
            }

            LogHandler.debug("[SassGenerator] Generate SASS selectors...");
            List<SassSelector> sassSelectors = GenerateSassSelectors(allSelectors, validMixins);

            LogHandler.debug("[SassGenerator] Generate SASS convenience mixins...");
            List<SassMixinBase> sassMixins = GenerateConvenienceMixins(sassSelectors);

            LogHandler.debug("[SassGenerator] Generate SASS rules...");
            List<SassRule> sassRules = GenerateSassRules(sassSelectors);

            LogHandler.debug("[SassGenerator] Generate SASS media rules...");
            List<SassMediaRule> mediaRules = GenerateMediaRules(sassRules);

            sassFiles.put(fileName, new SassFile(sassVariables, validMixins, sassMixins, sassRules, mediaRules));
        }

        return sassFiles;
    }


    private List<SassCloneMixin> FilterValidCloneMixins(List<SassCloneMixin> mixins)
    {
        List<SassCloneMixin> validMixins = new ArrayList<>();

        for (SassCloneMixin mixin : mixins)
        {
            List<MProperty> templateProps = mixin.GetProperties();
            int numberOfProps = templateProps.size();

            // if the total number of properties exceeds minimum property threshold, continue
            if (numberOfProps >= minPropCount)
            {
                // number of lines that mixin will add to output file
                int mixinSize = templateProps.size();

                int count = 0;
                int lineNumber = 0;
                for(MSelector mSelector : mixin.GetRelatedSelectors().stream()
                        .sorted((ms1, ms2) -> Integer.compare(ms1.GetRuleNumber(), ms2.GetRuleNumber()))
                        .collect(Collectors.toList()))
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

        return validMixins;
    }


    private void RestoreCloneMixin(SassCloneMixin mixin)
    {
        List<MProperty> mixinProps = mixin.GetProperties();

        for (MSelector mSelector : mixin.GetRelatedSelectors())
        {
            mixinProps.forEach(mSelector::AddProperty);
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

    private List<SassRule> GenerateSassRules(List<SassSelector> sassSelectors)
    {
        // group selectors by their line number, maintain order by using LinkedHashMap
        Map<Integer, List<SassSelector>> lineNoSelectorMap = new LinkedHashMap<>();
        sassSelectors.stream()
                .sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber()))
                .forEach((s) -> {
                    int lineNumber = s.GetRuleNumber();
                    if (!lineNoSelectorMap.containsKey(lineNumber))
                    {
                        lineNoSelectorMap.put(lineNumber, new ArrayList<>());
                    }

                    lineNoSelectorMap.get(lineNumber).add(s);
                });

        List<SassRule> sassRules = new ArrayList<>();

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

        return sassRules;
    }

    private List<SassMediaRule> GenerateMediaRules(List<SassRule> sassRules)
    {
        List<SassMediaRule> mediaRules = new ArrayList<>();
        Map<List<MediaQuery>, List<SassRule>> mediaGroups = new LinkedHashMap<>();
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

            List<SassRule> rulesInMedia = mediaGroups.get(mediaQueries);

            mediaRules.add(new SassMediaRule(mediaQueries, rulesInMedia));

            rulesInMedia.forEach(sassRules::remove);
        }

        return mediaRules;
    }

    private List<SassVariable> GenerateVariables(List<MSelector> sassSelectors)
    {
        List<SassVariable> variables = new ArrayList<>();

        ColorNameFinder ctn = new ColorNameFinder();
        if(!ctn.IsInitialized())
            return variables;

        List<String> browserColors = ParseBrowserColors();

        Map<String, String> definedVars = new HashMap<>();

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
                        varValue = ColorHelper.TryParseUrl(origValue);
                    }
                    else if (origValue.contains("rgba"))
                    {
                        String value = ColorHelper.TryParseRgb(origValue);
                        String[] parts = value.replaceFirst("rgba\\(", "").replaceFirst("\\)", "").split(",");

                        varType = SassVarType.ALPHA_COLOR;
                        varName = String.format("%s_%s", "alpha_color", ctn.TryGetNameForRgb(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
                        varValue = value;
                    }
                    else if (origValue.contains("rgb"))
                    {
                        String value = ColorHelper.TryParseRgb(origValue);
                        String[] parts = value.replaceFirst("rgb\\(", "").replaceFirst("\\)", "").split(",");

                        varType = SassVarType.COLOR;
                        varName = String.format("%s_%s", "color", ctn.TryGetNameForRgb(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
                        varValue = value;
                    }
                    else if (origName.contains("color"))
                    {
                        varType = SassVarType.COLOR;
                        varName = String.format("%s_%s", "color", origValue);
                        varValue = origValue;
                    }
                    else if (origName.equals("border") || origName.equals("background") || origName.equals("outline") || origName.equals("box-shadow"))
                    {
                        String[] parts = origValue.split(" ");

                        for (String part : parts)
                        {
                            if (browserColors.contains(part.trim()))
                            {
                                varType = SassVarType.COLOR;
                                varName = String.format("%s_%s", "color", part);
                                varValue = part;
                                break;
                            }
                        }
                    }

                    if (!varName.isEmpty() && !varValue.isEmpty())
                    {
                        // if varName already used, extend varName with an id
                        if (definedVars.containsKey(varName) && !definedVars.get(varName).equals(varValue))
                        {
                            int id = 1;
                            while (true)
                            {
                                String replace = String.format("%s_%d", varName, id);
                                if (!definedVars.containsKey(replace) || (definedVars.containsKey(replace) && definedVars.get(replace).equals(varValue)))
                                {
                                    varName = replace;
                                    break;
                                }
                                id++;
                            }
                        }

                        if(!definedVars.containsKey(varName))
                        {
                            SassVariable sv = new SassVariable(varType, varName, varValue, mProperty);
                            variables.add(sv);
                            definedVars.put(varName, varValue);
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


    private List<String> ParseBrowserColors()
    {
        try
        {
            return Files.readAllLines(new File("./src/main/java/com/crawljax/plugins/csssuite/sass/colors/color_names_browser.txt").toPath());
        }
        catch (IOException e)
        {
            LogHandler.error(e, "[SassGenerator] Cannot load color_names_browser.txt!");
        }

        return new ArrayList<>();
    }
}