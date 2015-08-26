package com.crawljax.plugins.cret.cssmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by axel on 5/22/2015.
 *
 * Represents a CSS file, containing regular rules, media rules and ignored rules (such as @import)
 */
public class MCssFile
{
    private final String _name;
    private final List<MCssRule> _allRules;
    private final List<MCssMediaRule> _mediaRules;
    private final List<MCssRuleBase> _ignoredRules;

    public MCssFile(String url, List<MCssRule> styleAndMediaRules, List<MCssMediaRule> mediaRules, List<MCssRuleBase> ignored)
    {
        _name = url;
        _allRules = styleAndMediaRules;
        _mediaRules = mediaRules;
        _ignoredRules = ignored;
    }

    /**
     * Convenience constructor for a new set of style rules on a previously defined file
     */
    public MCssFile(List<MCssRule> newRules, MCssFile oldFile)
    {
        this(oldFile.getName(), newRules, oldFile.getMediaRules(), oldFile.getIgnoredRules());
    }

    /** Getter */
    public String getName()
    {
        return _name;
    }

    /** Getter */
    public List<MCssRule> getRules()
    {
        return _allRules;
    }

    /** Getter */
    public List<MCssMediaRule> getMediaRules()
    {
        return _mediaRules;
    }

    /** Getter */
    public List<MCssRuleBase> getIgnoredRules()
    {
        return _ignoredRules;
    }

    /** Getter */
    public List<MCssRuleBase> getAllRules()
    {
        List<MCssRuleBase> result = new ArrayList<>();

        result.addAll(_allRules);
        result.addAll(_ignoredRules);

        return result.stream().sorted((r1, r2) -> Integer.compare(r1.getLineNumber(), r2.getLineNumber())).collect(Collectors.toList());
    }
}