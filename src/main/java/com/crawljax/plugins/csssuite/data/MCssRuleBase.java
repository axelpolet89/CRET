package com.crawljax.plugins.csssuite.data;

import com.steadystate.css.dom.AbstractCSSRuleImpl;
import com.steadystate.css.parser.media.MediaQuery;
import com.steadystate.css.userdata.UserDataConstants;
import org.w3c.css.sac.Locator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/15/2015.
 *
 * Base class for a CSS rule
 */
public class MCssRuleBase
{
    protected final AbstractCSSRuleImpl _rule;
    protected final List<MediaQuery> _mediaQueries;
    protected final Locator _locator;

    protected final MCssRuleBase _parent;

    public MCssRuleBase(AbstractCSSRuleImpl rule, List<MediaQuery> mediaQueries, MCssRuleBase parent)
    {
        _rule = rule;
        _mediaQueries = mediaQueries;
        _locator = (Locator)_rule.getUserData(UserDataConstants.KEY_LOCATOR);

        _parent = parent;
    }

    public MCssRuleBase(AbstractCSSRuleImpl rule)
    {
        this(rule, new ArrayList<>(), null);
    }

    public AbstractCSSRuleImpl GetAbstractRule() { return _rule; }

    public int GetLineNumber()
    {
        return _locator.getLineNumber();
    }

    public int GetColumnNumber()
    {
        return _locator.getColumnNumber();
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
