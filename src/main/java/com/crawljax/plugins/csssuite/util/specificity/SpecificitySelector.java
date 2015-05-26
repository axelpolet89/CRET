package com.crawljax.plugins.csssuite.util.specificity;

import com.crawljax.plugins.csssuite.data.MSelector;

/**
 * Wraps a MSelector and an integer, for use in SpecificityHelper only
 * This 'order' attribute is not included in the MSelector class, because it may change on a new state, while the MSelector is parsed only once
 */
public class SpecificitySelector
{
    private final MSelector _mSelector;
    private final int _order;

    public SpecificitySelector(MSelector mSelector, int order)
    {
        _mSelector = mSelector;
        _order = order;
    }

    public int GetSpecificity()
    {
        return _mSelector.GetSpecificity().GetValue();
    }

    public int GetRuleNumber()
    {
        return _mSelector.GetRuleNumber();
    }

    public int GetOrder()
    {
        return _order;
    }

    public MSelector GetSelector()
    {
        return _mSelector;
    }
}
