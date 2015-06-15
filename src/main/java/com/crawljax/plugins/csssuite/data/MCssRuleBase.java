package com.crawljax.plugins.csssuite.data;

import com.steadystate.css.dom.AbstractCSSRuleImpl;
import com.steadystate.css.parser.media.MediaQuery;
import com.steadystate.css.userdata.UserDataConstants;
import org.w3c.css.sac.Locator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/15/2015.
 */
public class MCssRuleBase
{
    protected final AbstractCSSRuleImpl _rule;
    protected final List<MediaQuery> _mediaQueries;
    protected final Locator _locator;

    public MCssRuleBase(AbstractCSSRuleImpl rule, List<MediaQuery> mediaQueries)
    {
        _rule = rule;
        _mediaQueries = mediaQueries;
        _locator = (Locator)_rule.getUserData(UserDataConstants.KEY_LOCATOR);
    }

    public MCssRuleBase(AbstractCSSRuleImpl rule)
    {
        this(rule, new ArrayList<>());
    }


    public int GetLineNumber()
    {
        return _locator.getLineNumber();
    }

    public List<MediaQuery> GetMediaQueries()
    {
        return _mediaQueries;
    }

    public boolean IsEmpty()
    {
        return false;
    }

    public String Print()
    {
        return _rule.toString() + "\n\n";
    }
}
