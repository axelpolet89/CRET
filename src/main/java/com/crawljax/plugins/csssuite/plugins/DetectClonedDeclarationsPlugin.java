package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.interfaces.ICssTransformer;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/12/2015.
 */
public class DetectClonedDeclarationsPlugin implements ICssTransformer
{
    private int _clonedDeclarationsRemoved = 0;

    @Override
    public void getStatistics(SuiteStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<CD>%d</CD>", prefix, _clonedDeclarationsRemoved);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for(String fileName : cssRules.keySet())
        {
            for(MCssRule mCssRule : cssRules.get(fileName).GetRules())
            {
                for(MSelector mSelector : mCssRule.GetSelectors())
                {
                    List<MDeclaration> declarations = mSelector.GetDeclarations();
                    List<MDeclaration> clonedProps = new ArrayList<>();

                    for(int i = 0; i < declarations.size(); i++)
                    {
                        final MDeclaration current = declarations.get(i);

                        for(int j = i+1; j < declarations.size(); j++)
                        {
                            final MDeclaration other = declarations.get(j);

                            if(current.GetName().equals(other.GetName()))
                            {
                                if((!current.IsImportant() || other.IsImportant()) && current.GetValueVendor().isEmpty())
                                {
                                    clonedProps.add(current);
                                    _clonedDeclarationsRemoved++;
                                    LogHandler.debug("[DetectClonedDeclarations] Declaration with '%s' in selector '%s' of file '%s' is a clone of a LATER declared declaration, and considered ineffective, will be removed", current, mSelector, fileName);
                                }
                                else if (current.IsImportant() && !other.IsImportant())
                                {
                                    clonedProps.add(other);
                                    _clonedDeclarationsRemoved++;
                                    LogHandler.debug("[DetectClonedDeclarations] Declaration with '%s' in selector '%s' of file '%s' is a clone of a PREVIOUS declared declaration, and considered ineffective, will be removed", current, mSelector, fileName);
                                }
                            }
                        }
                    }

                    mSelector.RemoveDeclarations(clonedProps);
                }
            }
        }

        return cssRules;
    }
}
