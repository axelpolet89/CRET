package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public class BorderMerger extends MergerBase
{
    protected String _width;
    protected String _style;
    protected String _color;

    public BorderMerger(String name)
    {
        super(name);

        _width = "";
        _style = "";
        _color = "";
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
            case "width":
                _width = value;
                break;
            case "style":
                _style = value;
                break;
            case "color":
                _color = value;
                break;
        }

        _isSet = true;
    }


    @Override
    public List<MProperty> BuildMProperties()
    {
        List<MProperty> result = new ArrayList<>();

        if(_style.isEmpty())
        {
            if(!_width.isEmpty())
                result.add(new MProperty(String.format("%s-width", _name), _width, _isImportant, true));

            if(!_color.isEmpty())
                result.add(new MProperty(String.format("%s-color", _name), _color, _isImportant, true));
        }
        else
        {
            String value = "";

            if(!_width.isEmpty())
                value += _width;

            value += " " + _style;

            if(!_color.isEmpty())
                value += " " + _color;

            result.add(new MProperty(_name, value, _isImportant, true));
        }

        return result;
    }
}
