package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;

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
    protected boolean parseFromSingle(String name, String value)
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
            default:
               return false;
        }

        return true;
    }


    @Override
    protected List<MDeclaration> mergeDeclarations()
    {
        List<MDeclaration> result = new ArrayList<>();

        if(_style.isEmpty())
        {
            if(!_width.isEmpty())
            {
                //exception by convention, on border-width: 0, we can just say border: 0
                if(_width.equals("0"))
                {
                    result.add(new MDeclaration(String.format("%s", _name), _width, _isImportant, true, _order));
                }
                else
                {
                    result.add(new MDeclaration(String.format("%s-width", _name), _width, _isImportant, true, _order));
                }
            }

            if(!_color.isEmpty())
            {
                result.add(new MDeclaration(String.format("%s-color", _name), _color, _isImportant, true, _order));
            }
        }
        else
        {
            String value = "";

            if(!_width.isEmpty())
            {
                value += _width;
            }

            value += " " + _style;

            if(!_color.isEmpty())
            {
                value += " " + _color;
            }

            result.add(new MDeclaration(_name, value, _isImportant, true, _order));
        }

        return result;
    }
}
