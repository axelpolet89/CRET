package com.crawljax.plugins.cret.util.specificity;

import com.crawljax.plugins.cret.data.MSelector;

/**
 * Wraps a MSelector and an integer, for use in SpecificityHelper only
 * This 'fileOrder' attribute is not included in the MSelector class, because it may change on a new state, while the MSelector is parsed only once
 * Furthermore, the fileOrder attribute is required for sorting when selectors originate from different files.
 * For instance an embedded file, will always have a higher order than a external file
 */
public class SpecificitySelector
{
    private final MSelector _mSelector;
    private final int _fileOrder;

    public SpecificitySelector(MSelector mSelector, int order)
    {
        _mSelector = mSelector;
        _fileOrder = order;
    }

    public int getSpecificity()
    {
        return _mSelector.getSpecificity().getValue();
    }

    public int getLineNumber()
    {
        return _mSelector.getLineNumber();
    }

    public int getOrder() { return _mSelector.getOrder(); }

    public int getFileOrder()
    {
        return _fileOrder;
    }

    public MSelector getSelector()
    {
        return _mSelector;
    }
}
