package com.crawljax.plugins.cret.sass;

import com.crawljax.plugins.cret.util.CretStringBuilder;

/**
 * Created by axel on 6/15/2015.
 */
public abstract class SassRuleBase
{
    protected final int _lineNumber;

    public SassRuleBase(int lineNumber)
    {
        _lineNumber = lineNumber;
    }

    public int getLineNumber()
    {
        return _lineNumber;
    }

    public abstract void print(CretStringBuilder builder, String prefix);
}
