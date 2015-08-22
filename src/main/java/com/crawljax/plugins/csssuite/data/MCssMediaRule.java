package com.crawljax.plugins.csssuite.data;

import com.steadystate.css.dom.CSSMediaRuleImpl;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.List;

/**
 * Created by axel on 6/9/2015.
 */
public class MCssMediaRule extends MCssRuleBase
{
    private List<MCssRuleBase> _innerRules;

    public MCssMediaRule(CSSMediaRuleImpl rule, List<MediaQuery> queries, MCssRuleBase parent)
    {
        super(rule, queries, parent);
    }

    public void SetInnerRules(List<MCssRuleBase> innerRules)
    {
        _innerRules = innerRules;
    }

    public List<MCssRuleBase> GetInnerRules(){ return _innerRules; }
}
