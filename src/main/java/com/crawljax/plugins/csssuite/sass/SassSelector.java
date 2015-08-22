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

        _selectorText = original.getSelectorText();
        _declarations = original.getDeclarations();

        _cloneIncludes = new ArrayList<>();
        _otherIncludes = new ArrayList<>();
    }

    public void addCloneInclude(SassCloneMixin sassTemplate)
    {
        _cloneIncludes.add(sassTemplate);
    }

    public void addInclude(String include)
    {
        _otherIncludes.add(include);
    }

    public void printContents(SuiteStringBuilder builder, String prefix)
    {
        _declarations.sort((p1, p2) -> Integer.compare(p1.getOrder(), p2.getOrder()));

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
            if(!mDeclaration.isFaulty())
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

    public String getSelectorText()
    {
        return _selectorText;
    }

    public int getLineNumber()
    {
        return _original.getLineNumber();
    }

    public int getOrder()
    {
        return _original.getOrder();
    }

    public List<MediaQuery> getMediaQueries() { return _original.getMediaQueries();}

    public MCssRuleBase getParent() { return _original.getParent(); }

    public boolean hasEqualDeclarationsByText(SassSelector other)
    {
        List<String> sorted = getSortedDeclarationsText();
        List<String> otherSorted = other.getSortedDeclarationsText();

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

    public List<String> getSortedDeclarationsText()
    {
        List<String> result = _cloneIncludes.stream().sorted((e1, e2) -> Integer.compare(e1.getNumber(), e2.getNumber())).map(e -> e.toString()).collect(Collectors.toList());
        result.addAll(_otherIncludes);
        result.addAll(_declarations.stream().sorted((p1, p2) -> p1.toString().compareTo(p2.toString())).map(p -> p.toString()).collect(Collectors.toList()));
        return result;
    }

    public List<MDeclaration> getDeclarations()
    {
        return _declarations;
    }

    public void removeDeclarations(List<MDeclaration> declarations)
    {
        _declarations.removeAll(declarations);
    }
}
