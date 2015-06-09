package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

/**
 * Created by axel on 6/9/2015.
 */
public class SassVariable
{
    private final String _prefix;
    private final String _name;
    private final MProperty _relatedProperty;

    public SassVariable(String prefix, String name, MProperty property)
    {
        _prefix = prefix;
        _name = name;
        _relatedProperty = property;
    }

    public void Print(SuiteStringBuilder builder)
    {
        builder.append("$%s-%s: %s", _prefix, _name, _relatedProperty.GetValue());
    }
}
