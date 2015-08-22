package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;

import java.util.Arrays;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public class BoxMerger extends MergerBase
{
    private String _top;
    private String _right;
    private String _bottom;
    private String _left;

    public BoxMerger(String name)
    {
        super(name);

        _top = "";
        _right = "";
        _bottom = "";
        _left = "";
    }


    /**
     *
     * @param name
     * @param value
     */
    protected boolean ParseFromSingle(String name, String value)
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
            default:
                return false;
        }

        _isSet = true;
        return true;
    }


    /**
     *
     * @return
     */
    @Override
    protected List<MDeclaration> mergeDeclarations()
    {
        String value;

        if(_top.equals(_right) && _top.equals(_bottom) && _top.equals(_left))
        {
            value = _top;
        }
        else if(_top.equals(_bottom) && _right.equals(_left))
        {
            value = String.format("%s %s", _top, _right);
        }
        else if(_right.equals(_left))
        {
            value = String.format("%s %s %s", _top, _right, _bottom);
        }
        else
        {
            value =  String.format("%s %s %s %s", _top, _right, _bottom, _left);
        }

        return Arrays.asList(new MDeclaration(_name, value, _isImportant, true, _order));
    }
}
