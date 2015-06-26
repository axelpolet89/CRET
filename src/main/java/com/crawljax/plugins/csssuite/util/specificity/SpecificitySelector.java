package com.crawljax.plugins.csssuite.util.specificity;

import com.crawljax.plugins.csssuite.data.MSelector;

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

    public int GetSpecificity()
    {
        return _mSelector.GetSpecificity().GetValue();
    }

    public int GetLineNumber()
    {
        return _mSelector.GetLineNumber();
    }

    public int GetOrder() { return _mSelector.GetOrder(); }

    public int GetFileOrder()
    {
        return _fileOrder;
    }

    public MSelector GetSelector()
    {
        return _mSelector;
    }
}
