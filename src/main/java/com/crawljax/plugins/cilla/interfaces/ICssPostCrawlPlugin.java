package com.crawljax.plugins.cilla.interfaces;

import com.crawljax.plugins.cilla.data.MCssRule;

import java.util.List;
import java.util.Map;

/**
 * Created by axel on 5/18/2015.
 */
public interface ICssPostCrawlPlugin
{
    public Map<String, List<MCssRule>> Transform(Map<String, List<MCssRule>> cssRules);
}