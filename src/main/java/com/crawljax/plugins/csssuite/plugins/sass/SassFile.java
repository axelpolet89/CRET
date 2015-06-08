package com.crawljax.plugins.csssuite.plugins.sass;

import java.util.List;

/**
 * Created by axel on 6/8/2015.
 */
public class SassFile
{
    private final List<SassTemplate> _extensions;
    private final List<SassSelector> _selectors;

    public SassFile(List<SassTemplate> sassTemplates, List<SassSelector> sassSelectors)
    {
        _extensions = sassTemplates;
        _selectors = sassSelectors;
    }
}
