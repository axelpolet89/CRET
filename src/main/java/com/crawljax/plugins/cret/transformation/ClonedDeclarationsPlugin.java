package com.crawljax.plugins.cret.transformation;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.interfaces.ICssTransformer;
import com.crawljax.plugins.cret.transformation.matcher.MatchedElements;
import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 6/12/2015.
 *
 * This class is responsible for removing declarations per selector, when multiple declarations share the same name
 */
public class ClonedDeclarationsPlugin implements ICssTransformer
{
    private int _clonedDeclarationsRemoved = 0;

    @Override
    public void getStatistics(CretStringBuilder builder, String prefix)
    {
        builder.appendLine("%s<CD>%d</CD>", prefix, _clonedDeclarationsRemoved);
    }

    @Override
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
    {
        for(String fileName : cssRules.keySet())
        {
            for(MCssRule mCssRule : cssRules.get(fileName).getRules())
            {
                for(MSelector mSelector : mCssRule.getSelectors())
                {
                    List<MDeclaration> declarations = mSelector.getDeclarations();
                    List<MDeclaration> clonedProps = new ArrayList<>();

                    for(int i = 0; i < declarations.size(); i++)
                    {
                        final MDeclaration current = declarations.get(i);

                        for(int j = i+1; j < declarations.size(); j++)
                        {
                            final MDeclaration other = declarations.get(j);

                            if(current.getName().equals(other.getName()))
                            {
                                if((!current.isImportant() || other.isImportant()) && current.getValueVendor().isEmpty())
                                {
                                    clonedProps.add(current);
                                    _clonedDeclarationsRemoved++;
                                    LogHandler.debug("[ClonedDeclarations] Declaration with '%s' in selector '%s' of file '%s' is a clone of a LATER declared declaration, and considered ineffective, will be removed", current, mSelector, fileName);
                                }
                                else if (current.isImportant() && !other.isImportant())
                                {
                                    clonedProps.add(other);
                                    _clonedDeclarationsRemoved++;
                                    LogHandler.debug("[ClonedDeclarations] Declaration with '%s' in selector '%s' of file '%s' is a clone of a PREVIOUS declared declaration, and considered ineffective, will be removed", current, mSelector, fileName);
                                }
                            }
                        }
                    }

                    mSelector.removeDeclarations(clonedProps);
                }
            }
        }

        return cssRules;
    }
}
