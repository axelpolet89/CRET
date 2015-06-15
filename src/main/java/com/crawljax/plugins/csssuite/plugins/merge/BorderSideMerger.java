package com.crawljax.plugins.csssuite.plugins.merge;

/**
 * Created by axel on 5/27/2015.
 */
public class BorderSideMerger extends BorderMerger
{
    public BorderSideMerger(String name)
    {
        super(name);
    }


    /**
     *
     * @param name
     * @param value
     */
    @Override
    protected void ParseFromSingle(String name, String value)
    {
        switch (name.split("-")[2])
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
}
