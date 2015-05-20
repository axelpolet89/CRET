package com.crawljax.plugins.cilla.data;

import com.crawljax.plugins.cilla.util.SuiteStringBuilder;
import org.w3c.css.sac.Locator;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSStyleRule;

import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.userdata.UserDataConstants;
import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.parser.SelectorListImpl;

import java.util.*;
import java.util.stream.Collectors;

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
	 * Parse all selectors from this _rule and add them to the _selectors
	 */
	private void SetSelectors()
	{
		if (_rule instanceof CSSStyleRuleImpl)
		{
			CSSStyleRuleImpl styleRuleImpl = (CSSStyleRuleImpl) _rule;

			_selectors.addAll(((SelectorListImpl) styleRuleImpl.getSelectors())
					.getSelectors().stream()
					.map(selector -> new MSelector(selector, ParseProperties(), GetLocator().getLineNumber()))
					.collect(Collectors.toList()));
		}
	}


	/** Getter */
	public CSSRule GetRule()
	{
		return _rule;
	}

	/** Getter */
	public List<MSelector> GetSelectors()
	{
		return _selectors;
	}


	/**
	 * Remove the given list of selectors from the _selectors
	 * @param selectors the list of selectors ts to remove
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

			properties.addAll(styleDeclaration.getProperties().stream().map(property -> new MProperty(property.getName(), property.getValue().getCssText(), property.isImportant())).collect(Collectors.toList()));
		}

		return properties;
	}


	/**
	 * @return the _selectors that are not matched (not associated DOM elements have been detected).
	 */
	public List<MSelector> GetUnmatchedSelectors()
	{
		return _selectors.stream().filter(selector -> !selector.IsMatched() && !selector.IsIgnored()).collect(Collectors.toList());
	}


	/**
	 * @return the _selectors that are effective (associated DOM elements have been detected).
	 */
	public List<MSelector> GetMatchedSelectors()
	{
		return _selectors.stream().filter(selector -> selector.IsMatched() && !selector.IsIgnored()).collect(Collectors.toList());
	}


	/**
	 * @return the Locator of this _rule (line number, column).
	 */
	public Locator GetLocator()
	{
		if (_rule instanceof CSSStyleRuleImpl)
		{
			return (Locator) ((CSSStyleRuleImpl) _rule).getUserData(UserDataConstants.KEY_LOCATOR);
		}

		return null;
	}


	/**
	 * Transform the current rule into valid CSS syntax
	 * @return
	 */
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

		SuiteStringBuilder builder = new SuiteStringBuilder();
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
				builder.appendLine("\t" + mProp.Print());
			}
			builder.appendLine("}\n\n");
		}

		return builder.toString();
	}


	@Override
	public String toString() {

		SuiteStringBuilder buffer = new SuiteStringBuilder();
		Locator locator = GetLocator();

		buffer.append("locator: line=" + locator.getLineNumber() + " col=" + locator.getColumnNumber());
		buffer.appendLine("Rule: " + _rule.getCssText() + "\n");

		for (MSelector selector : _selectors) {
			buffer.append(selector.toString());
		}

		return buffer.toString();
	}
}