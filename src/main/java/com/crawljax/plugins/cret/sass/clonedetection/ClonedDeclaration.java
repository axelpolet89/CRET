package com.crawljax.plugins.cret.sass.clonedetection;

import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;

/**
 * Created by axel on 6/5/2015.
 */
public class ClonedDeclaration
{
    private final MDeclaration _mDeclaration;
    private final MSelector _mSelector;

    public ClonedDeclaration(MDeclaration mDeclaration, MSelector mSelector)
    {
        _mDeclaration = mDeclaration;
        _mSelector = mSelector;
    }

    public MSelector getSelector()
    {
        return _mSelector;
    }

    public MDeclaration getProperty()
    {
        return _mDeclaration;
    }
}
