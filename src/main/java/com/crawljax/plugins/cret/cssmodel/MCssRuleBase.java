package com.crawljax.plugins.cret.cssmodel;

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

    public AbstractCSSRuleImpl getAbstractRule() { return _rule; }

    /** Getter */
    public int getLineNumber()
    {
        return _locator.getLineNumber();
    }

    /** Getter */
    public int getColumnNumber()
    {
        return _locator.getColumnNumber();
    }

    /** Getter */
    public List<MediaQuery> getMediaQueries()
    {
        return _mediaQueries;
    }

    /** Getter */
    public boolean isEmpty()
    {
        return false;
    }

    /** Getter */
    public String print()
    {
        return _rule.toString() + "\n\n";
    }
}
