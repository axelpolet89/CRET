package com.crawljax.plugins.cret.sass;

import com.crawljax.plugins.cret.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.cret.sass.mixins.SassMixinBase;
import com.crawljax.plugins.cret.sass.variables.SassVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/8/2015.
 */
public class SassFile
{
    private final List<SassVariable> _variables;
    private final List<SassCloneMixin> _cloneMixins;
    private final List<SassMixinBase> _mixins;
    private final List<SassRuleBase> _rules;

    public SassFile(List<SassVariable> variables, List<SassCloneMixin> cloneMixins, List<SassMixinBase> mixins, List<SassRuleBase> rules)
    {
        _variables = variables;
        _cloneMixins = cloneMixins;
        _mixins = mixins;

        _rules = new ArrayList<>();
        _rules.addAll(rules);
        _rules.sort((r1, r2 ) -> Integer.compare(r1.getLineNumber(), r2.getLineNumber()));
    }

    /** Getter */
    public List<SassVariable> getVariables() { return _variables; }

    /** Getter */
    public List<SassCloneMixin> getCloneMixins() { return _cloneMixins; }

    /** Getter */
    public List<SassMixinBase> getMixins() { return _mixins; }

    /** Getter */
    public List<SassRuleBase> getRules() { return _rules; }
}