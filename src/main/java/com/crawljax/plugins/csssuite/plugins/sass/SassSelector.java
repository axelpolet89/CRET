package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/8/2015.
 */
public class SassSelector
{
    private String _selectorText;
    private MSelector _original;

    private SassSelector _parent;
    private SassSelector _child;

    private List<MProperty> _properties;
    private List<SassTemplate> _extends;

    public SassSelector(MSelector original)
    {
        _original = original;

        _selectorText = original.GetSelectorText();
        _properties = original.GetProperties();

        _extends = new ArrayList<>();
    }

    public void AddExtension(SassTemplate sassTemplate)
    {
        _extends.add(sassTemplate);
    }

    public String GetSelectorText()
    {
        return _selectorText;
    }

    public String Print()
    {
        SuiteStringBuilder builder = new SuiteStringBuilder();

        builder.append(_selectorText);
        builder.append("{");

        PrintInner(builder);

        builder.appendLine("}\n\n");

        return builder.toString();
    }

    public void PrintInner(SuiteStringBuilder builder)
    {
        for(SassTemplate sassTemplate : _extends)
        {
            builder.appendLine("\t@extend: %%extend_%d;", sassTemplate.GetNumber());
        }

        if(_extends.size() > 0)
            builder.appendLine("");

        for(MProperty mProperty : _properties)
        {
            builder.appendLine("\t%s", mProperty);
        }
    }

    public int GetRuleNumber()
    {
        return _original.GetRuleNumber();
    }
}
