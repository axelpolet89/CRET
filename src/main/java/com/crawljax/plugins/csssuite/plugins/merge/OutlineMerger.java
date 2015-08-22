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
    protected boolean parseFromSingle(String name, String value)
    {
        if (name.contains("offset"))
        {
            _offset = value;
            _isSet = true;
            return true;
        }
        else
        {
            return super.parseFromSingle(name, value);
        }
    }


    @Override
    protected List<MDeclaration> mergeDeclarations()
    {
        List<MDeclaration> result = super.mergeDeclarations();

        if(!_offset.isEmpty())
        {
            result.add(new MDeclaration("outline-offset", _offset, _isImportant, true, _order));
        }

        return result;
    }
}
