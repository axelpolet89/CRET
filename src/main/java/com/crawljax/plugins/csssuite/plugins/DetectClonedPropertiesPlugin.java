package com.crawljax.plugins.csssuite.plugins;

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
 * Created by axel on 6/12/2015.
 */
public class DetectClonedPropertiesPlugin implements ICssPostCrawlPlugin
{
    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {
        for(String fileName : cssRules.keySet())
        {
            for(MCssRule mCssRule : cssRules.get(fileName).GetRules())
            {
                for(MSelector mSelector : mCssRule.GetSelectors())
                {
                    List<MProperty> mProperties = mSelector.GetProperties();
                    List<MProperty> clonedProps = new ArrayList<>();

                    for(int i = 0; i < mProperties.size(); i++)
                    {
                        final MProperty current = mProperties.get(i);

                        for(int j = i+1; j < mProperties.size(); j++)
                        {
                            final MProperty other = mProperties.get(j);

                            if(current.GetName().equals(other.GetName()) && (!current.IsImportant() || other.IsImportant()) && current.GetValueVendor().isEmpty())
                            {
                                clonedProps.add(current);
                                LogHandler.debug("[DetectClonedProperties] Property with '%s' in selector '%s' of file '%s' is a clone of a later declared property, and considered ineffective, will be removed", current, mSelector, fileName);
                                break;
                            }
                        }
                    }

                    mSelector.RemoveProperties(clonedProps);
                }
            }
        }

        return cssRules;
    }
}
