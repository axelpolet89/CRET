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

    private List<MCssRule> _rules;
    private List<MMediaRule> _mediaRules;
    private List<CSSImportRule> _importRules;
    private List<CSSPageRule> _pageRules;

    public MCssFile(String url)
    {
        _url = url;

        _rules = new ArrayList<>();
        _mediaRules = new ArrayList<>();
        _importRules = new ArrayList<>();
        _pageRules = new ArrayList<>();
    }

    public List<MCssRule> GetRules(){
        return _rules;
    }

    public List<MMediaRule> GetMediaRules(){
        return _mediaRules;
    }

    public void SetRules(List<MCssRule> rules){
        _rules = rules;
    }
    public void AddMediaRules(List<MMediaRule> rules){
        _mediaRules.addAll(rules);
    }
    public void AddImportRules(List<CSSImportRule> rules){
        _importRules.addAll(rules);
    }
    public void AddPageRules(List<CSSPageRule> rules){
        _pageRules.addAll(rules);
    }
}
