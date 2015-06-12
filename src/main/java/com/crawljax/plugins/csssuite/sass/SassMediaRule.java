package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.List;

/**
 * Created by axel on 6/9/2015.
 */
public class SassMediaRule
{
    private final List<MediaQuery> _mediaQueries;
    private final List<SassRule> _sassRules;

    public SassMediaRule(List<MediaQuery> queries, List<SassRule> rules)
    {
        _mediaQueries = queries;
        _sassRules = rules;
    }

    public void Print(SuiteStringBuilder builder)
    {
        builder.append("@media");
        for(MediaQuery mq : _mediaQueries)
        {
            builder.append(" " + mq.toString());
        }
        builder.append("{\n\n");

        for(int i = 0; i < _sassRules.size(); i++)
        {
            SassRule sr = _sassRules.get(i);
            sr.Print(builder, "\t");

            if(i < _sassRules.size() -1)
                builder.append("\n\n");
        }
        builder.appendLine("}");
    }
}
