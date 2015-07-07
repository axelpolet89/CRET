package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.data.properties.MProperty;

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
    protected final List<MProperty> _otherProps;

    /**
     *
     * @param name
     */
    public MergerBase(String name)
    {
        _name = name;
        _otherProps = new ArrayList<>();
    }

    /**
     *
     * @param name
     * @param value
     */
    protected abstract boolean ParseFromSingle(String name, String value);


    /**
     *
     * @return
     */
    protected abstract List<MProperty> MergeProperties();


    /**
     *
     * @return
     */
    public final Boolean IsSet()
    {
        return _isSet;
    }


    /**
     *
     * @return
     */
    public final Boolean IsImportant()
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
    public final void Parse(String name, String value, boolean isImportant, int order) throws CssSuiteException
    {
        if(!_isSet)
        {
            _isImportant = isImportant;
            _order = order;
            _isSet = true;
        }
        else if (_isImportant != isImportant)
        {
            throw new CssSuiteException("Cannot normalize value '%s' for name '%s', because another property was previously parsed " +
                                            "with important=%s and this property has important=%s, while this one is.", value, name, _isImportant, isImportant);
        }

        _order = Math.min(order, _order);
        if(!ParseFromSingle(name, value))
        {
            _otherProps.add(new MProperty(name, value, isImportant, order));
        }
        _isSet = true;
    }

    public final List<MProperty> BuildMProperties()
    {
        _otherProps.addAll(MergeProperties());
        return _otherProps;
    }
}
