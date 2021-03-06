package com.crawljax.plugins.cret.cssmodel;

import com.steadystate.css.dom.CSSMediaRuleImpl;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.List;

/**
 * Created by axel on 6/9/2015.
 *
 * Represents a CSS media rule, which can contain multiple other CSS rules
 */
public class MCssMediaRule extends MCssRuleBase
{
    private List<MCssRuleBase> _innerRules;

    public MCssMediaRule(CSSMediaRuleImpl rule, List<MediaQuery> queries, MCssRuleBase parent)
    {
        super(rule, queries, parent);
    }

    /** Setter */
    public void setInnerRules(List<MCssRuleBase> innerRules)
    {
        _innerRules = innerRules;
    }

    /** Getter */
    public List<MCssRuleBase> getInnerRules(){ return _innerRules; }
}
