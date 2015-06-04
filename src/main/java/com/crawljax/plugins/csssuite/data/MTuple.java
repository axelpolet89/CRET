package com.crawljax.plugins.csssuite.data;

import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for holding selectors that have the same properties
 */
public class MTuple
{
    private final List<MSelector> _selectors;
    private final List<MProperty> _properties;

    public MTuple(MSelector mSelector, List<MProperty> properties)
    {
        _selectors = new ArrayList<>();
        _properties = properties;
        _selectors.add(mSelector);
    }

    public void AddSelector(MSelector selector)
    {
        _selectors.add(selector);
    }

    public List<MSelector> GetSelectors()
    {
        return _selectors;
    }

    public List<MProperty> GetProperties()
    {
        return _properties;
    }
}