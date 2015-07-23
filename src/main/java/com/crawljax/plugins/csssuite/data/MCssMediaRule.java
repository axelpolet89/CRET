package com.crawljax.plugins.csssuite.data;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
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

    @Override
    public boolean IsCompatibleWithMediaRule()
    {
        return true;
    }

    public void SetInnerRules(List<MCssRuleBase> innerRules)
    {
        _innerRules = innerRules;
    }

    public List<MCssRuleBase> GetInnerRules(){ return _innerRules; }

    public void Print(SuiteStringBuilder builder, String prefix)
    {
//        builder.append("@media");
//        for(MediaQuery mq : _mediaQueries)
//        {
//            builder.append(" " + mq.toString());
//        }
//        builder.append("{\n\n");
//
//        for(int i = 0; i < _sassRules.size(); i++)
//        {
//            SassRule sr = _sassRules.get(i);
//            sr.Print(builder, "\t");
//
//            if(i < _sassRules.size() -1)
//                builder.append("\n\n");
//        }
//        builder.appendLine("}");
    }
}
