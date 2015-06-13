package com.crawljax.plugins.csssuite.data;

import com.steadystate.css.dom.CSSRuleListImpl;
import java.util.List;

/**
 * Created by axel on 5/22/2015.
 *
 * Represents a CSS file, including MCssRules that we will analyze and CSSRuleImpl that we will ignore
 * For example, we will ignore @page and @import rules
 */
public class MCssFile
{
    private final String _name;
    private final List<MCssRule> _allRules;
    private final CSSRuleListImpl _ignoredRules;

    public MCssFile(String url, List<MCssRule> rules, CSSRuleListImpl ignored)
    {
        _name = url;
        _allRules = rules;
        _ignoredRules = ignored;
    }

    /** Getter */
    public String GetName()
    {
        return _name;
    }

    /** Getter */
    public List<MCssRule> GetRules()
    {
        return _allRules;
    }

    /** Getter */
    public CSSRuleListImpl GetIgnoredRules()
    {
        return _ignoredRules;
    }
}