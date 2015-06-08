package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public class BackgroundMerger extends MergerBase
{
    private String _color;
    private String _image;
    private String _position;
    private String _repeat;
    private String _attachment;

    public BackgroundMerger(String name)
    {
        super(name);

        _color = "";
        _image = "";
        _position = "";
        _repeat = "";
        _attachment = "";
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
            case "color":
                _color = value;
                break;
            case "image":
                _image = value;
                break;
            case "position":
                _position = value;
                break;
            case "repeat":
                _repeat = value;
                break;
            case "attachment":
                _attachment = value;
                break;
        }

        _isSet = true;
    }


    @Override
    public List<MProperty> BuildMProperties()
    {
        String value = "";

        if(!_color.isEmpty())
            value += _color;

        if(!_image.isEmpty())
            value += " " + _image;

        if(!_position.isEmpty())
            value += " " + _position;

        if(!_repeat.isEmpty())
            value += " " + _repeat;

        if(!_attachment.isEmpty())
            value += " " + _attachment;

        return Arrays.asList(new MProperty(_name, value, _isImportant));
    }
}
