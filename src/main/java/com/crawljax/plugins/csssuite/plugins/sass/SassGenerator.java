package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.generator.CssWriter;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.sass.clonedetection.CloneDetector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            sassFiles.put(fileName, new SassFile(templates, sassSelectors));

            CssWriter cssWriter = new CssWriter();
            try
            {
                cssWriter.GenerateSassFile(fileName, sassSelectors, templates);
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

}
