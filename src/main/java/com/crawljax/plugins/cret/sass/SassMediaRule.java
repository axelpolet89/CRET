package com.crawljax.plugins.cret.sass;

import com.crawljax.plugins.cret.util.CretStringBuilder;
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

    public void print(CretStringBuilder builder, String prefix)
    {
        builder.append("@media");
        boolean mediaSet = false;
        for(MediaQuery mq : _mediaQueries)
        {
            String text = mq.toString();
            if(text.charAt(0) == (char)65533)
            {
                text = text.substring(1, text.length());
            }

            if(!mediaSet)
            {
                builder.append(" " + text);
                mediaSet = true;
            }
            else
            {
                builder.append(", " + text);
            }
        }
        builder.append("{\n\n");

        for(int i = 0; i < _sassRules.size(); i++)
        {
            SassRuleBase sr = _sassRules.get(i);
            sr.print(builder, "\t");

            if(i < _sassRules.size() -1)
                builder.append("\n\n");
        }
        builder.appendLine("}");
    }
}
