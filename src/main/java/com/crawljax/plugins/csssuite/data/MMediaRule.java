package com.crawljax.plugins.csssuite.data;

import com.crawljax.plugins.csssuite.LogHandler;
import com.jcabi.w3c.Defect;
import com.steadystate.css.dom.CSSMediaRuleImpl;
import com.steadystate.css.dom.CSSStyleRuleImpl;

import com.steadystate.css.dom.MediaListImpl;
import com.steadystate.css.parser.media.MediaQuery;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by axel on 5/22/2015.
 */
public class MMediaRule
{
    private final CSSMediaRuleImpl _mediaRule;
    private List<MCssRule> _mRules;
    private List<MediaQuery> _queries;

    public MMediaRule(CSSRule mediaRule, Set<Defect> w3cErrors)
    {
        _mediaRule = (CSSMediaRuleImpl)mediaRule;
        _queries = new ArrayList<>();
        _mRules = new ArrayList<>();

        MediaListImpl list = (MediaListImpl)_mediaRule.getMedia();
        for(int i = 0; i < list.getLength(); i++)
        {
            _queries.add(list.mediaQuery(i));
        }

        CSSRuleList ruleList = _mediaRule.getCssRules();
        for (int i = 0; i < ruleList.getLength(); i++)
        {
            try
            {
                CSSRule rule = ruleList.item(i);
                if(rule instanceof CSSStyleRuleImpl)
                {
                    _mRules.add(new MCssRule(rule, w3cErrors, _queries));
                }
                else
                {
                    //@import
                    //@page
                }
            }
            catch (Exception ex)
            {
                LogHandler.error(ex, "Error occurred while parsing CSSRules into MCssRules on rule '%s'", ruleList.item(i).getCssText());
            }
        }
    }


    /**
     *
     * @return
     */
    public List<MCssRule> GetMcssRules()
    {
        return _mRules;
    }
}
