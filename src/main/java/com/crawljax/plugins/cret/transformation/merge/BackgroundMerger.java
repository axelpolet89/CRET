package com.crawljax.plugins.cret.transformation.merge;

import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public class BackgroundMerger extends MergerBase
{
    private String _color;
    private String _image;
    private String _position;
    private String _size;
    private String _origin;
    private String _clip;
    private String _repeat;
    private String _attachment;

    public BackgroundMerger(String name)
    {
        super(name);

        _color = "";
        _image = "";
        _position = "";
        _size = "";
        _origin = "";
        _clip = "";
        _repeat = "";
        _attachment = "";
    }


    protected boolean parseSingleDeclaration(String name, String value)
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
            case "size":
                _size = value;
                break;
            case "origin":
                _origin = value;
                break;
            case "clip":
                _clip = value;
                break;
            case "repeat":
                _repeat = value;
                break;
            case "attachment":
                _attachment = value;
                break;
            default:
                return false;
        }

        _isSet = true;
        return true;
    }


    @Override
    protected List<MDeclaration> mergeDeclarations()
    {
        List<MDeclaration> result = new ArrayList<>();

        String value = "";

        if(!_color.isEmpty())
            value += _color;

        if(!_image.isEmpty())
            value += " " + _image;

        if(!_position.isEmpty())
        {
            value += " " + _position;

            if(!_size.isEmpty())
            {
                value += "/" + _size;
            }
        }
        else if (!_size.isEmpty())
        {
            result.add(new MDeclaration("background-size", _size, isImportant(), true, _order));
        }

        if(!_origin.isEmpty())
            value += " " + _origin;

        if(!_clip.isEmpty())
             value += " " + _clip;

        if(!_repeat.isEmpty())
            value += " " + _repeat;

        if(!_attachment.isEmpty())
            value += " " + _attachment;

        if(!value.isEmpty())
        {
            result.add(new MDeclaration(_name, value, _isImportant, true, _order));
        }

        return result;
    }
}
