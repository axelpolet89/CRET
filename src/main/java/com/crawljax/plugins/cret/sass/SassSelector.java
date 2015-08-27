package com.crawljax.plugins.cret.sass;

import com.crawljax.plugins.cret.cssmodel.MCssRuleBase;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.cret.util.CretStringBuilder;
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

    /**
     * Add clone mixin, transform into @include to this selector
     */
    public void addCloneInclude(SassCloneMixin sassMixin)
    {
        _cloneIncludes.add(sassMixin);
    }

    /**
     * Add @include, as simple string
     */
    public void addInclude(String include)
    {
        _otherIncludes.add(include);
    }


    public void printContents(CretStringBuilder builder, String prefix)
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

    /** Getter */
    public List<SassCloneMixin> getIncludes()
    {
        return _cloneIncludes;
    }

    /** Getter */
    public List<String> getOtherIncludes()
    {
        return _otherIncludes;
    }

    /** Getter */
    public String getSelectorText()
    {
        return _selectorText;
    }

    /** Getter */
    public int getLineNumber()
    {
        return _original.getLineNumber();
    }

    /** Getter */
    public int getOrder()
    {
        return _original.getOrder();
    }

    /** Getter */
    public List<MediaQuery> getMediaQueries()
    {
        return _original.getMediaQueries();
    }

    /** Getter */
    public MCssRuleBase getParent()
    {
        return _original.getParent();
    }

    /** Getter */
    public List<MDeclaration> getDeclarations()
    {
        return _declarations;
    }


    /**
     * Compare given selector with this selector on their declarations
     * @return true if they contain equal declarations
     */
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


    /**
     * @return list of declarations containing clone includes, simple includes and declarations sorted by their order
     */
    public List<String> getSortedDeclarationsText()
    {
        List<String> result = _cloneIncludes.stream().sorted((e1, e2) -> Integer.compare(e1.getNumber(), e2.getNumber())).map(e -> e.toString()).collect(Collectors.toList());
        result.addAll(_otherIncludes);
        result.addAll(_declarations.stream().sorted((p1, p2) -> p1.toString().compareTo(p2.toString())).map(p -> p.toString()).collect(Collectors.toList()));
        return result;
    }


    /**
     * Remove given set of declarations from this selector
     */
    public void removeDeclarations(List<MDeclaration> declarations)
    {
        _declarations.removeAll(declarations);
    }
}
