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

    private List<MCssRule> _regularRules;
    private List<MMediaRule> _mediaRules;
    private List<CSSImportRule> _importRules;
    private List<CSSPageRule> _pageRules;

    private List<MCssRule> _allRules;

    public MCssFile(String url)
    {
        _url = url;

        _regularRules = new ArrayList<>();
        _mediaRules = new ArrayList<>();
        _importRules = new ArrayList<>();
        _pageRules = new ArrayList<>();

        _allRules = new ArrayList<>();
    }

    public List<MCssRule> GetRules()
    {
        return _allRules;
    }

    public List<MMediaRule> GetMediaRules(){
        return _mediaRules;
    }

    public List<MCssRule> GetRegularRules()
    {
        return _regularRules;
    }

    public void SetRegularRules(List<MCssRule> rules)
    {
        _regularRules = rules;
        _allRules.addAll(rules);
    }

    public void SetMediaRules(List<MMediaRule> mediaRules)
    {
        _mediaRules = mediaRules;
        mediaRules.stream().forEach(m -> _allRules.addAll(m.GetMcssRules()));
    }

    public void SetAllRules(List<MCssRule> allRules)
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
