package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.data.MCssRuleBase;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/8/2015.
 */
public class SassSelector
{
    private String _selectorText;
    private MSelector _original;

    private List<MDeclaration> _declarations;
    private List<SassCloneMixin> _cloneIncludes;
    private List<String> _otherIncludes;

    public SassSelector(MSelector original)
    {
        _original = original;

        _selectorText = original.GetSelectorText();
        _declarations = original.GetDeclarations();

        _cloneIncludes = new ArrayList<>();
        _otherIncludes = new ArrayList<>();
    }

    public void AddCloneInclude(SassCloneMixin sassTemplate)
    {
        _cloneIncludes.add(sassTemplate);
    }

    public void AddInclude(String include)
    {
        _otherIncludes.add(include);
    }

    public void PrintContents(SuiteStringBuilder builder, String prefix)
    {
        _declarations.sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));

        for(SassCloneMixin cloneMixin : _cloneIncludes)
        {
            builder.appendLine("%s\t@include %s;", prefix, cloneMixin);
        }

        for(String otherMixin : _otherIncludes)
        {
            builder.appendLine("%s\t@include %s;", prefix, otherMixin);
        }

        for(MDeclaration mDeclaration : _declarations)
        {
            if(!mDeclaration.IsFaulty())
            {
                builder.appendLine("%s\t%s", prefix, mDeclaration);
            }
        }
    }

    public List<SassCloneMixin> getIncludes()
    {
        return _cloneIncludes;
    }

    public List<String> getOtherIncludes()
    {
        return _otherIncludes;
    }

    public String GetSelectorText()
    {
        return _selectorText;
    }

    public int GetLineNumber()
    {
        return _original.GetLineNumber();
    }

    public int GetOrder()
    {
        return _original.GetOrder();
    }

    public List<MediaQuery> GetMediaQueries() { return _original.GetMediaQueries();}

    public MCssRuleBase GetParent() { return _original.GetParent(); }

    public boolean HasEqualDeclarationsByText(SassSelector other)
    {
        List<String> sorted = GetSortedDeclarationsText();
        List<String> otherSorted = other.GetSortedDeclarationsText();

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

    public List<String> GetSortedDeclarationsText()
    {
        List<String> result = _cloneIncludes.stream().sorted((e1, e2) -> Integer.compare(e1.GetNumber(), e2.GetNumber())).map(e -> e.toString()).collect(Collectors.toList());
        result.addAll(_otherIncludes);
        result.addAll(_declarations.stream().sorted((p1, p2) -> p1.toString().compareTo(p2.toString())).map(p -> p.toString()).collect(Collectors.toList()));
        return result;
    }

    public List<MDeclaration> GetProperties()
    {
        return _declarations;
    }

    public void RemoveDeclarations(List<MDeclaration> declarations)
    {
        _declarations.removeAll(declarations);
    }
}
