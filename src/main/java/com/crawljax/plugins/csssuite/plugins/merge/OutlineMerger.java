package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;

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
    protected boolean ParseFromSingle(String name, String value)
    {
        if (name.contains("offset"))
        {
            _offset = value;
            _isSet = true;
            return true;
        }
        else
        {
            return super.ParseFromSingle(name, value);
        }
    }


    @Override
    protected List<MDeclaration> MergeProperties()
    {
        List<MDeclaration> result = super.MergeProperties();

        if(!_offset.isEmpty())
        {
            result.add(new MDeclaration("outline-offset", _offset, _isImportant, true, _order));
        }

        return result;
    }
}
