package com.crawljax.plugins.csssuite.plugins.sass;

import com.steadystate.css.parser.media.MediaQuery;

import java.util.List;

/**
 * Created by axel on 6/8/2015.
 */
public class SassFile
{
    private final List<SassTemplate> _extensions;
    private final List<SassRule> _rules;
    private final List<SassMediaRule> _mediaRules;

    public SassFile(List<SassTemplate> sassTemplates, List<SassRule> rules, List<SassMediaRule> mediaRules)
    {
        _extensions = sassTemplates;
        _rules = rules;
        _mediaRules = mediaRules;
    }

    public List<SassTemplate> getExtensions()
    {
        return _extensions;
    }

    public List<SassRule> getRules()
    {
        return _rules;
    }

    public List<SassMediaRule> getMediaRules()
    {
        return _mediaRules;
    }
}
