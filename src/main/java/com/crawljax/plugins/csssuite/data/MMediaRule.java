package com.crawljax.plugins.csssuite.data;

import com.crawljax.plugins.csssuite.LogHandler;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.sun.webkit.dom.CSSRuleImpl;
import org.w3c.dom.css.CSSMediaRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 5/22/2015.
 */
public class MMediaRule
{
    private final CSSMediaRule _mediaRule;
    private List<MCssRule> _mRules;

    public MMediaRule(CSSRule mediaRule)
    {
        _mediaRule = (CSSMediaRule)mediaRule;
        _mRules = new ArrayList<>();

        CSSRuleList ruleList = _mediaRule.getCssRules();
        for (int i = 0; i < ruleList.getLength(); i++)
        {
            try
            {
                CSSRule rule = ruleList.item(i);
                if(rule instanceof CSSStyleRuleImpl)
                {
                    _mRules.add(new MCssRule(rule));
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
