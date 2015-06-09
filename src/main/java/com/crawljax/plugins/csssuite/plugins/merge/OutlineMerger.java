package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public class OutlineMerger extends BorderMerger
{
    private String _offset;
    public OutlineMerger(String name)
    {
        super(name);

        _offset = "";
    }


    /**
     *
     * @param name
     * @param value
     */
    @Override
    protected void ParseFromSingle(String name, String value)
    {
        if (name.contains("offset"))
        {
            _offset = value;
            _isSet = true;
        }
        else
        {
            super.ParseFromSingle(name, value);
        }
    }


    @Override
    public List<MProperty> BuildMProperties()
    {
        List<MProperty> result = super.BuildMProperties();

        if(!_offset.isEmpty())
            result.add(new MProperty("outline-offset", _offset, _isImportant, true));

        return result;
    }
}
