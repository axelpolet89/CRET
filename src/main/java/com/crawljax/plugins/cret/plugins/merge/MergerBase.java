package com.crawljax.plugins.cret.plugins.merge;

import com.crawljax.plugins.cret.CssSuiteException;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 *
 * Base class responsible for merging one or more separate declarations into a shorthand
 * If not all declarations are merge-able, then they are returned as-is
 */
public abstract class MergerBase
{
    protected final String _name;
    protected boolean _isImportant;
    protected int _order;
    protected boolean _isSet;
    protected final List<MDeclaration> _otherDeclarations;

    public MergerBase(String name)
    {
        _name = name;
        _otherDeclarations = new ArrayList<>();
    }

    /**
     * Parse single declaration in for this merge instance
     * @return false if parse was unsuccesful
     */
    protected abstract boolean parseSingleDeclaration(String name, String value);

    /**
     * Merge some or all parsed declarations into a shorthand declaration
     * @return merged and unmerged declarations
     */
    protected abstract List<MDeclaration> mergeDeclarations();


    /** Getter */
    public final Boolean isImportant()
    {
        return _isImportant;
    }


    /**
     * parse given name, value, !important and order if possible
     * @throws CssSuiteException
     */
    public final void parse(String name, String value, boolean isImportant, int order) throws CssSuiteException
    {
        if(!_isSet)
        {
            _isImportant = isImportant;
            _order = order;
            _isSet = true;
        }
        else if (_isImportant != isImportant)
        {
            throw new CssSuiteException("Cannot normalize value '%s' for name '%s', because another declaration was previously parsed " +
                                            "with important=%s and this declaration has important=%s, while this one is.", value, name, _isImportant, isImportant);
        }

        _order = Math.min(order, _order);
        if(!parseSingleDeclaration(name, value))
        {
            _otherDeclarations.add(new MDeclaration(name, value, isImportant, order));
        }
        _isSet = true;
    }

    /**
     * @return parsed declarations in their merged or ummerged form
     */
    public final List<MDeclaration> buildMDeclarations()
    {
        _otherDeclarations.addAll(mergeDeclarations());
        return _otherDeclarations;
    }
}
