package com.crawljax.plugins.cret.plugins.merge;

import com.crawljax.plugins.cret.CssSuiteException;
import com.crawljax.plugins.cret.data.declarations.MDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public abstract class MergerBase
{
    protected final String _name;
    protected boolean _isImportant;
    protected int _order;
    protected boolean _isSet;
    protected final List<MDeclaration> _otherDeclarations;

    /**
     *
     * @param name
     */
    public MergerBase(String name)
    {
        _name = name;
        _otherDeclarations = new ArrayList<>();
    }

    /**
     *
     * @param name
     * @param value
     */
    protected abstract boolean parseFromSingle(String name, String value);


    /**
     *
     * @return
     */
    protected abstract List<MDeclaration> mergeDeclarations();


    /**
     *
     * @return
     */
    public final Boolean isImportant()
    {
        return _isImportant;
    }


    /**
     *
     * @param name
     * @param value
     * @param isImportant
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
        if(!parseFromSingle(name, value))
        {
            _otherDeclarations.add(new MDeclaration(name, value, isImportant, order));
        }
        _isSet = true;
    }

    public final List<MDeclaration> buildMDeclarations()
    {
        _otherDeclarations.addAll(mergeDeclarations());
        return _otherDeclarations;
    }
}
