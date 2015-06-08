package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public abstract class MergerBase
{
    protected String _name;
    protected boolean _isImportant;
    protected boolean _isSet;

    /**
     *
     * @param name
     */
    public MergerBase(String name)
    {
        _name = name;
    }

    /**
     *
     * @return
     */
    public Boolean IsSet()
    {
        return _isSet;
    }


    /**
     *
     * @return
     */
    public Boolean IsImportant()
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
    public final void Parse(String name, String value, boolean isImportant) throws CssSuiteException
    {
        if(!_isSet)
        {
            _isImportant = isImportant;
        }
        else if (_isSet && _isImportant != isImportant)
        {
            throw new CssSuiteException("Cannot normalize value '%s' for name '%s', because another property was previously parsed " +
                                            "with important=%s and this property has important=%s, while this one is.", value, name, _isImportant, isImportant);
        }

        ParseFromSingle(name, value);
    }


    /**
     *
     * @return
     */
    public abstract List<MProperty> BuildMProperties();


    /**
     *
     * @param name
     * @param value
     */
    protected abstract void ParseFromSingle(String name, String value);
}
