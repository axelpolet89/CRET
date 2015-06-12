package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/8/2015.
 */
public class SassSelector
{
    private String _selectorText;
    private MSelector _original;

    private List<MProperty> _properties;
    private List<SassCloneMixin> _includes;
    private List<String> _otherIncludes;

    public SassSelector(MSelector original)
    {
        _original = original;

        _selectorText = original.GetSelectorText();
        _properties = original.GetProperties();

        _includes = new ArrayList<>();
        _otherIncludes = new ArrayList<>();
    }

    public void AddCloneInclude(SassCloneMixin sassTemplate)
    {
        _includes.add(sassTemplate);
    }

    public void AddInclude(String include)
    {
        _otherIncludes.add(include);
    }

    public void PrintContents(SuiteStringBuilder builder, String prefix)
    {
        for(SassCloneMixin cloneMixin : _includes)
        {
            builder.appendLine("%s\t@include %s;", prefix, cloneMixin);
        }

        for(String otherMixin : _otherIncludes)
        {
            builder.appendLine("%s\t@include %s;", prefix, otherMixin);
        }

        for(MProperty mProperty : _properties)
        {
            builder.appendLine("%s\t%s", prefix, mProperty);
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

    public List<MediaQuery> GetMediaQueries() { return _original.GetMediaQueries();}

    public boolean HasEqualDeclarationsByText(SassSelector other)
    {
        List<String> sorted = GetSortedPropertiesText();
        List<String> otherSorted = other.GetSortedPropertiesText();

        if(sorted.size() != otherSorted.size())
        {
            return false;
        }

        for(int i = 0; i < sorted.size(); i++)
        {
            if(!sorted.get(i).equals(otherSorted.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    public List<String> GetSortedPropertiesText()
    {
        List<String> result = _includes.stream().sorted((e1, e2) -> Integer.compare(e1.GetNumber(), e2.GetNumber())).map(e -> e.toString()).collect(Collectors.toList());
        result.addAll(_properties.stream().sorted((p1, p2) -> p1.toString().compareTo(p2.toString())).map(p -> p.toString()).collect(Collectors.toList()));
        return result;
    }

    public List<MProperty> GetProperties()
    {
        return _properties;
    }

    public void RemoveProperties(List<MProperty> properties)
    {
        _properties.removeAll(properties);
    }
}
