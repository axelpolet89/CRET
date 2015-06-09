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

    private List<MProperty> _properties;
    private List<SassTemplate> _extensions;

    public SassSelector(MSelector original)
    {
        _original = original;

        _selectorText = original.GetSelectorText();
        _properties = original.GetProperties();

        _extensions = new ArrayList<>();
    }

    public void AddExtension(SassTemplate sassTemplate)
    {
        _extensions.add(sassTemplate);
    }

    public void PrintContents(SuiteStringBuilder builder)
    {
        for(SassTemplate sassTemplate : _extensions)
        {
            builder.appendLine("\t@extend: %%extend_%d;", sassTemplate.GetNumber());
        }

//        if(_extensions.size() > 0 && _properties.size() > 0)
//        {
//            builder.appendLine("");
//        }

        for(MProperty mProperty : _properties)
        {
            builder.appendLine("\t%s", mProperty);
        }
    }

    public String GetSelectorText()
    {
        return _selectorText;
    }

    public int GetRuleNumber()
    {
        return _original.GetRuleNumber();
    }
}
