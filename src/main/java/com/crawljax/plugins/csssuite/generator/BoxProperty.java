package com.crawljax.plugins.csssuite.generator;

import com.crawljax.plugins.csssuite.CssSuiteException;

/**
 * Created by axel on 5/27/2015.
 */
public class BoxProperty extends ShortHandProperty
{
    private String _top;
    private String _right;
    private String _bottom;
    private String _left;
    private String _all;

    public BoxProperty()
    {
        _top = "0";
        _right = "0";
        _bottom = "0";
        _left = "0";
    }


    /**
     *
     * @param name
     * @param value
     */
    protected void ParseFromSingle(String name, String value)
    {
        switch (name.split("-")[1])
        {
            case "top":
                _top = value;
                break;
            case "right":
                _right = value;
                break;
            case "bottom":
                _bottom = value;
                break;
            case "left":
                _left = value;
                break;
        }

        _isSet = true;
    }


    /**
     *
     * @param value
     * @throws CssSuiteException
     */
    protected void ParseFromShortHand(String value) throws CssSuiteException
    {
        String[] parts = value.split("\\s");

        switch (parts.length)
        {
            case 1:
                _all = parts[0];
                break;
            case 2:
                _top = _bottom = parts[0];
                _right = _left = parts[1];
                break;
            case 3:
                _top = parts[0];
                _right = parts[1];
                _bottom = parts[2];
                _left = _right;
                break;
            case 4:
                _top = parts[0];
                _right = parts[1];
                _bottom = parts[2];
                _left = parts[3];
                break;
            default:
                throw new CssSuiteException("Cannot normalize value '%s', because number of parts larger than 4 or smaller than 1", value);
        }

        _isSet = true;
    }


    @Override
    public String toString()
    {
        if(_all != null && !_all.isEmpty())
            return _all;

        return String.format("%s %s %s %s", _top, _right, _bottom, _left);
    }
}
