package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

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

    public int GetLineNumber()
    {
        return _lineNumber;
    }

    public abstract void Print(SuiteStringBuilder builder, String prefix);
}
