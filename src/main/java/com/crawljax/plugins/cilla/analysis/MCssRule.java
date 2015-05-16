package com.crawljax.plugins.cilla.analysis;

import java.util.ArrayList;
import java.util.List;

import com.steadystate.css.parser.SelectorListImpl;
import org.w3c.css.sac.Locator;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;

import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.userdata.UserDataConstants;

public class MCssRule
{

	private CSSRule _rule;
	private List<MSelector> _selectors;

	/**
	 *
	 * @param rule
	 */
	public MCssRule(CSSRule rule) {

		_rule = rule;
		_selectors = new ArrayList<>();

		SetSelectors();
	}

	private void SetSelectors() {
		if (_rule instanceof CSSStyleRuleImpl)
		{
			CSSStyleRuleImpl styleRuleImpl = (CSSStyleRuleImpl) _rule;

			List<MProperty> props = GetProperties();

			SelectorListImpl list = (SelectorListImpl)styleRuleImpl.getSelectors();

			for(org.w3c.css.sac.Selector selector : list.getSelectors())
			{
				_selectors.add(new MSelector(selector, props, GetLocator().getLineNumber()));
			}
		}
	}


	//todo: store properties instead of parsing them on every call?
	public List<MProperty> GetProperties()
	{
		List<MProperty> properties = new ArrayList<>();

		if (_rule instanceof CSSStyleRule)
		{
			CSSStyleRule styleRule = (CSSStyleRule) _rule;
			CSSStyleDeclaration styleDeclaration = styleRule.getStyle();

			for (int j = 0; j < styleDeclaration.getLength(); j++)
			{
				String property = styleDeclaration.item(j);
				String value = styleDeclaration.getPropertyCSSValue(property).getCssText();
				properties.add(new MProperty(property, value));
			}
		}

		return properties;
	}


	/**
	 *
	 * @return
	 */
	public CSSRule GetRule()
	{
		return _rule;
	}


	/**
	 *
	 * @return
	 */
	public List<MSelector> GetSelectors()
	{
		return _selectors;
	}


	/**
	 * @return the _selectors that are not matched (not associated DOM elements have been detected).
	 */
	public List<MSelector> GetUnmatchedSelectors() {
		List<MSelector> unmatched = new ArrayList<>();

		for (MSelector selector : _selectors) {
			if (!selector.isMatched() && !selector.IsIgnored()) {
				unmatched.add(selector);
			}
		}

		return unmatched;

	}

	/**
	 * @return the _selectors that are effective (associated DOM elements have been detected).
	 */
	public List<MSelector> GetMatchedSelectors() {
		List<MSelector> effective = new ArrayList<MSelector>();

		for (MSelector selector : _selectors) {
			if (selector.isMatched() && !selector.IsIgnored()) {
				effective.add(selector);
			}
		}

		return effective;

	}

	/**
	 * @return the Locator of this _rule (line number, column).
	 */
	public Locator GetLocator() {
		if (_rule instanceof CSSStyleRuleImpl) {
			return (Locator) ((CSSStyleRuleImpl) _rule)
			        .getUserData(UserDataConstants.KEY_LOCATOR);
		}

		return null;
	}


	/**
	 *
	 * @param ruleList
	 * @return
	 */
	public static List<MCssRule> ConvertToMCssRules(CSSRuleList ruleList)
	{
		List<MCssRule> mCssRules = new ArrayList<>();

		for (int i = 0; i < ruleList.getLength(); i++) {
			mCssRules.add(new MCssRule(ruleList.item(i)));
		}

		return mCssRules;
	}

	@Override
	public String toString() {

		StringBuffer buffer = new StringBuffer();
		Locator locator = GetLocator();

		buffer.append("locator: line=" + locator.getLineNumber() + " col="
				+ locator.getColumnNumber() + "\n");
		buffer.append("Rule: " + _rule.getCssText() + "\n");

		for (MSelector selector : _selectors) {
			buffer.append(selector.toString());
		}

		return buffer.toString();
	}

}