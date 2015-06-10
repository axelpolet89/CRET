package com.crawljax.plugins.csssuite.plugins.sass;

import com.crawljax.plugins.csssuite.plugins.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.plugins.sass.mixins.SassMixinBase;

import java.util.List;

/**
 * Created by axel on 6/8/2015.
 */
public class SassFile
{
    private final List<SassVariable> _variables;
    private final List<SassCloneMixin> _cloneMixins;
    private final List<SassMixinBase> _mixins;
    private final List<SassRule> _rules;
    private final List<SassMediaRule> _mediaRules;

    public SassFile(List<SassVariable> variables, List<SassCloneMixin> cloneMixins, List<SassMixinBase> mixins, List<SassRule> rules, List<SassMediaRule> mediaRules)
    {
        _variables = variables;
        _cloneMixins = cloneMixins;
        _mixins = mixins;
        _rules = rules;
        _mediaRules = mediaRules;
    }

    public List<SassVariable> getVariables() { return _variables; }

    public List<SassCloneMixin> getCloneMixins() { return _cloneMixins; }

    public List<SassMixinBase> getMixins() { return _mixins; }

    public List<SassRule> getRules()
    {
        return _rules;
    }

    public List<SassMediaRule> getMediaRules()
    {
        return _mediaRules;
    }
}