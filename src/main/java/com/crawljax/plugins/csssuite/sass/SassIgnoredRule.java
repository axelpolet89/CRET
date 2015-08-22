package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.steadystate.css.dom.AbstractCSSRuleImpl;

/**
 * Created by axel on 6/15/2015.
 */
public class SassIgnoredRule extends SassRuleBase
{
    private final AbstractCSSRuleImpl _rule;

    public SassIgnoredRule(int lineNumber, AbstractCSSRuleImpl rule)
    {
        super(lineNumber);

        _rule = rule;
    }

    public void print(SuiteStringBuilder builder, String prefix)
    {
        builder.append("%s%s", prefix, _rule.toString()
                .replace(": ;",": '';").replace(":;", ": '';")                              //paypal
                .replace("content:/", "content:url(/").replace("_V_.png","_V_.png")        //imdb
                .replace("background: /", "background: url(/").replace(".png", ".png)"));   //vk
    }
}
