package com.crawljax.plugins.csssuite.interfaces;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.Map;

/**
 * Created by axel on 5/18/2015.
 */
public interface ICssTransformer
{
    public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements);
    public void getStatistics(SuiteStringBuilder builder, String prefix);
}