package com.crawljax.plugins.cret.interfaces;

import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.plugins.analysis.MatchedElements;
import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.Map;

/**
 * Created by axel on 5/18/2015.
 */
public interface ICssTransformer
{
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements);
    public void getStatistics(CretStringBuilder builder, String prefix);
}