package com.crawljax.plugins.csssuite.data;

import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSPageRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 5/22/2015.
 */
public class MCssFile
{
    private final String _url;

    private List<CSSImportRule> _importRules;
    private List<CSSPageRule> _pageRules;

    private List<MCssRule> _allRules;

    public MCssFile(String url, List<MCssRule> rules)
    {
        _url = url;
        _allRules = rules;

        _importRules = new ArrayList<>();
        _pageRules = new ArrayList<>();
    }

    public String GetUrl(){ return _url; }
    public List<MCssRule> GetRules()
    {
        return _allRules;
    }

    public void OverwriteAllRules(List<MCssRule> allRules)
    {
        _allRules = allRules;
    }

    public void AddImportRules(List<CSSImportRule> rules){
        _importRules.addAll(rules);
    }
    public void AddPageRules(List<CSSPageRule> rules){
        _pageRules.addAll(rules);
    }
}
