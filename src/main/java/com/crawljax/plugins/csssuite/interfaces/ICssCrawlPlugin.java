package com.crawljax.plugins.csssuite.interfaces;

import com.crawljax.plugins.csssuite.data.MCssRule;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;

/**
 * Created by axel on 5/18/2015.
 */
public interface ICssCrawlPlugin
{
    public void Transform(String stateName, Document dom, Map<String, List<MCssRule>> cssRules);
}