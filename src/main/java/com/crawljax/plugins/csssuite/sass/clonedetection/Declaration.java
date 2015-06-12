package com.crawljax.plugins.csssuite.sass.clonedetection;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;

/**
 * Created by axel on 6/5/2015.
 */
public class Declaration
{
    private final MProperty _mProperty;
    private final MSelector _mSelector;

    public Declaration(MProperty mProperty, MSelector mSelector)
    {
        _mProperty = mProperty;
        _mSelector = mSelector;
    }

    public MSelector getSelector()
    {
        return _mSelector;
    }

    public MProperty getProperty()
    {
        return _mProperty;
    }
}
