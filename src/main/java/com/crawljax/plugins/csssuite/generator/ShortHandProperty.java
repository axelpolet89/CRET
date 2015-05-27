package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.CssSuiteException;

/**
 * Created by axel on 5/27/2015.
 */
public abstract class ShortHandProperty
{
    protected boolean _isImportant;
    protected boolean _isSet;

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

        if(name.contains("-"))
        {
            ParseFromSingle(name, value);
        }
        else
        {
            ParseFromShortHand(value);
        }
    }


    /**
     *
     * @param name
     * @param value
     */
    protected abstract void ParseFromSingle(String name, String value);


    /**
     *
     * @param value
     * @throws CssSuiteException
     */
    protected abstract void ParseFromShortHand(String value) throws CssSuiteException;
}
