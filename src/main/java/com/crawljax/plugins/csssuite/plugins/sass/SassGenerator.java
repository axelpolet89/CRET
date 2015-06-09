package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.generator.CssWriter;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.sass.clonedetection.CloneDetector;
import com.crawljax.plugins.csssuite.plugins.sass.colors.ColorNameFinder;
import com.crawljax.plugins.csssuite.util.ColorHelper;
import com.steadystate.css.parser.media.MediaQuery;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/8/2015.
 */
public class SassGenerator implements ICssPostCrawlPlugin
{
    public int minPropCount = 3;

    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
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

            List<SassTemplate> templates = cd.GenerateSassTemplates(allSelectors);

            for (SassTemplate t : templates)
            {
                List<MProperty> templateProps = t.GetProperties();

                // restore properties for each template we will NOT include, because they do not adhere to minimum property count
                if (templateProps.size() > 0 && templateProps.size() < minPropCount)
                {
                    for (MSelector templateSel : t.GetRelatedSelectors())
                    {
                        for (MProperty mProperty : templateProps)
                        {
                            templateSel.AddProperty(mProperty);
                        }
                    }
                }
            }

            templates = templates.stream().filter(t -> t.GetProperties().size() >= minPropCount).collect(Collectors.toList());
            LogHandler.debug("Found %d templates that apply to more than %d or more properties", templates.size(), minPropCount);

            for (int i = 0; i < templates.size(); i++)
            {
                templates.get(i).SetNumber(i + 1);
            }

            List<SassSelector> sassSelectors = GenerateSassSelectors(allSelectors, templates);
            List<SassVariable> sassVariables = GenerateVariables(sassSelectors);
            List<SassRule> sassRules = GenerateSassRules(sassSelectors);
            List<SassMediaRule> mediaRules = GenerateMediaRules(sassRules);

            sassFiles.put(fileName, new SassFile(templates, sassRules, mediaRules));
        }

        CssWriter cssWriter = new CssWriter();

        for(String fileName : sassFiles.keySet())
        {
            try
            {
                cssWriter.GenerateSassFile(fileName, sassFiles.get(fileName));
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return cssRules;
    }

    private List<SassSelector> GenerateSassSelectors(List<MSelector> selectors, List<SassTemplate> extensions)
    {
        List<SassSelector> results = new ArrayList<>();

        for(MSelector mSelector : selectors)
        {
            SassSelector ss = new SassSelector(mSelector);

            for(SassTemplate st : extensions)
            {
                for(MSelector related : st.GetRelatedSelectors())
                {
                    if(related == mSelector)
                    {
                        ss.AddExtension(st);
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
            if(!mediaGroups.containsKey(currentMedia))
            {
                mediaGroups.put(currentMedia, new ArrayList<>());
                mediaGroups.get(currentMedia).add(sassRule);
                continue;
            }

            List<MediaQuery> mqs = sassRule.getMediaQueries();
            if(currentMedia.containsAll(mqs) && mqs.containsAll(currentMedia))
            {
                mediaGroups.get(currentMedia).add(sassRule);
            }

            currentMedia = sassRule.getMediaQueries();
        }

        // remove any sass rule contained in a media rule from given list
        // create sass media rules from media groups
        for(List<MediaQuery> mediaQueries : mediaGroups.keySet())
        {
            if(mediaQueries.isEmpty())
                continue;

            mediaGroups.get(mediaQueries).forEach(sassRules::remove);

            mediaRules.add(new SassMediaRule(mediaQueries, mediaGroups.get(mediaQueries)));
        }

        return mediaRules;
    }

    private List<SassVariable> GenerateVariables(List<SassSelector> sassSelectors)
    {
        List<SassVariable> variables = new ArrayList<>();

        ColorNameFinder ctn = new ColorNameFinder();
        if(!ctn.IsInitialized())
            return variables;

        for(SassSelector sassSelector : sassSelectors)
        {
            for(MProperty mProperty : sassSelector.GetProperties())
            {
                String value = mProperty.GetValue();
                if(value.contains("rgba"))
                {

                }
                else if(value.contains("rgb"))
                {
                    value = ColorHelper.TryParseRgb(value);
                    String[] parts = value.replaceFirst("rgb\\(","").replaceFirst("\\)","").split(",");
                    try
                    {
                        String name = ctn.TryGetNameForRgb(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
                    }
                    catch (CssSuiteException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        return variables;
    }
}