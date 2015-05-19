package com.crawljax.plugins.cilla.data;

import com.steadystate.css.dom.Property;
import org.w3c.css.sac.Locator;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleRule;

import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.userdata.UserDataConstants;
import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.parser.SelectorListImpl;

import java.util.*;

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


	/**
	 *
	 */
	private void SetSelectors() {
		if (_rule instanceof CSSStyleRuleImpl)
		{
			CSSStyleRuleImpl styleRuleImpl = (CSSStyleRuleImpl) _rule;

			List<MProperty> props = ParseProperties();

			SelectorListImpl list = (SelectorListImpl)styleRuleImpl.getSelectors();

			for(org.w3c.css.sac.Selector selector : list.getSelectors())
			{
				_selectors.add(new MSelector(selector, props, GetLocator().getLineNumber()));
			}
		}
	}


	/**
	 *
	 * @param selectors
	 */
	public void RemoveSelectors(List<MSelector> selectors)
	{
		_selectors.removeAll(selectors);
	}


	//todo: store properties instead of parsing them on every call?
	/**
	 *
	 * @return
	 */
	public List<MProperty> ParseProperties()
	{
		List<MProperty> properties = new ArrayList<>();

		if (_rule instanceof CSSStyleRule)
		{
			CSSStyleRule styleRule = (CSSStyleRule) _rule;
			CSSStyleDeclarationImpl styleDeclaration = (CSSStyleDeclarationImpl)styleRule.getStyle();

			for (Property property : styleDeclaration.getProperties())
			{
				properties.add(new MProperty(property.getName(), property.getValue().getCssText(), property.isImportant()));
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
			if (!selector.IsMatched() && !selector.IsIgnored()) {
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
			if (selector.IsMatched() && !selector.IsIgnored()) {
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


	public String Print()
	{
		Map<List<MProperty>, List<MSelector>> combinations = new HashMap<>();
		for(MSelector mSelector : _selectors)
		{
			List<MProperty> mProps = mSelector.GetProperties();

			if(combinations.containsKey(mProps))
			{
				combinations.get(mProps).add(mSelector);
			}
			else
			{
				combinations.put(mProps, new ArrayList<>(Arrays.asList(mSelector)));
			}
		}

		StringBuilder builder = new StringBuilder();
		for(List<MProperty> mProps : combinations.keySet())
		{
			List<MSelector> mSelectors = combinations.get(mProps);
			int size = mSelectors.size();
			for(int i = 0; i < size; i++)
			{
				builder.append(mSelectors.get(i).GetSelectorText());
				if(i < size - 1)
					builder.append(", ");
			}

			builder.append(" {");
			for(MProperty mProp : mProps)
			{
				builder.append("\n");
				builder.append("\t" + mProp.Print());
			}
			builder.append("\n}\n\n");
		}

		return builder.toString();
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