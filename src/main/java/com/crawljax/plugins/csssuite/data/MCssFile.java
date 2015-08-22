package com.crawljax.plugins.csssuite.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by axel on 5/22/2015.
 *
 * Represents a CSS file
 * For example, we will ignore @page and @import rules
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
        this(oldFile.GetName(), newRules, oldFile.GetMediaRules(), oldFile.GetIgnoredRules());
    }

    /** Getter */
    public String GetName()
    {
        return _name;
    }

    /** Getter */
    public List<MCssRule> GetRules()
    {
        return _allRules;
    }

    /** Getter */
    public List<MCssMediaRule> GetMediaRules()
    {
        return _mediaRules;
    }

    /** Getter */
    public List<MCssRuleBase>  GetIgnoredRules()
    {
        return _ignoredRules;
    }

    /** Getter */
    public List<MCssRuleBase> GetAllRules()
    {
        List<MCssRuleBase> result = new ArrayList<>();

        result.addAll(_allRules);
        result.addAll(_ignoredRules);

        return result.stream().sorted((r1, r2) -> Integer.compare(r1.GetLineNumber(), r2.GetLineNumber())).collect(Collectors.toList());
    }
}