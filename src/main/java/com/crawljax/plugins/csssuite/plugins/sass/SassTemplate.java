package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/5/2015.
 */
public class SassTemplate
{
    private List<MProperty> _properties;
    private List<MSelector> _extractedFrom;

    public SassTemplate()
    {
        _properties = new ArrayList<>();
        _extractedFrom = new ArrayList<>();
    }

    public void addProperty(MProperty mProperty)
    {
        _properties.add(mProperty);
    }

    public void addSelector(MSelector mSelector)
    {
        _extractedFrom.add(mSelector);
    }

    public boolean sameSelectors(List<MSelector> selectors)
    {
        return _extractedFrom.size() == selectors.size() && selectors.containsAll(_extractedFrom);
    }

    public List<MProperty> GetProperties()
    {
        return _properties;
    }

    public List<MSelector> GetRelatedSelectors()
    {
        return _extractedFrom;
    }
}
