package com.crawljax.plugins.cret.sass.clonedetection;

import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;

/**
 * Created by axel on 6/5/2015.
 *
 * Wrapper used to identify a cloned declaration, including the selector in which it is contained
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

    /** Getter */
    public MSelector getSelector()
    {
        return _mSelector;
    }

    /** Getter */
    public MDeclaration getProperty()
    {
        return _mDeclaration;
    }
}
