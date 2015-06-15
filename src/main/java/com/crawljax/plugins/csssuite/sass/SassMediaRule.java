package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.List;

/**
 * Created by axel on 6/9/2015.
 */
public class SassMediaRule extends SassRuleBase
{
    private final List<MediaQuery> _mediaQueries;
    private final List<SassRuleBase> _sassRules;

    public SassMediaRule(int lineNumber, List<MediaQuery> queries, List<SassRuleBase> rules)
    {
        super(lineNumber);

        _mediaQueries = queries;
        _sassRules = rules;
    }

    public void Print(SuiteStringBuilder builder, String prefix)
    {
        builder.append("@media");
        for(MediaQuery mq : _mediaQueries)
        {
            builder.append(" " + mq.toString());
        }
        builder.append("{\n\n");

        for(int i = 0; i < _sassRules.size(); i++)
        {
            SassRuleBase sr = _sassRules.get(i);
            sr.Print(builder, "\t");

            if(i < _sassRules.size() -1)
                builder.append("\n\n");
        }
        builder.appendLine("}");
    }
}
