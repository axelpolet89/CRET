package com.crawljax.plugins.csssuite.plugins.sass;

import java.util.List;

/**
 * Created by axel on 6/8/2015.
 */
public class SassFile
{
    private final List<SassVariable> _variables;
    private final List<SassMixin> _extensions;
    private final List<SassRule> _rules;
    private final List<SassMediaRule> _mediaRules;

    public SassFile(List<SassVariable> variables, List<SassMixin> sassTemplates, List<SassRule> rules, List<SassMediaRule> mediaRules)
    {
        _variables = variables;
        _extensions = sassTemplates;
        _rules = rules;
        _mediaRules = mediaRules;
    }

    public List<SassVariable> getVariables() { return _variables; }

    public List<SassMixin> getExtensions()
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
