package com.crawljax.plugins.cret.transformation.merge;

import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;

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

    @Override
    protected boolean parseSingleDeclaration(String name, String value)
    {
        if (name.contains("offset"))
        {
            _offset = value;
            _isSet = true;
            return true;
        }
        else
        {
            return super.parseSingleDeclaration(name, value);
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
