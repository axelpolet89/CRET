package com.crawljax.plugins.csssuite.data;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.jcabi.w3c.Defect;
import com.steadystate.css.dom.Property;
import com.steadystate.css.parser.media.MediaQuery;
import org.w3c.css.sac.Locator;
import org.w3c.dom.css.CSSRule;

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

	public MCssRule(CSSRule rule, Set<Defect> w3cErrors)
	{
		_rule = rule;
		_selectors = new ArrayList<>();

		SetSelectors(w3cErrors, new ArrayList<>());
	}


	public MCssRule(CSSRule rule, Set<Defect> w3cErrors, List<MediaQuery> queries)
	{
		_rule = rule;
		_selectors = new ArrayList<>();

		SetSelectors(w3cErrors, queries);
	}


	/**
	 *
	 * @param rule
	 */
	public MCssRule(CSSRule rule)
	{
		this(rule, new HashSet<>());
	}


	/**
	 * Parse all selectors from this _rule and add them to the _selectors
	 */
	private void SetSelectors(Set<Defect> w3cErrors, List<MediaQuery> queries)
	{
		CSSStyleRuleImpl styleRule = (CSSStyleRuleImpl) _rule;

		_selectors.addAll(((SelectorListImpl) styleRule.getSelectors())
							.getSelectors().stream()
							.map(selector -> new MSelector(selector, ParseProperties(styleRule, w3cErrors), GetLocator().getLineNumber(), queries))
							.collect(Collectors.toList()));
	}


	/**
	 *
	 * @param styleRule
	 * @param w3cErrors
	 * @return
	 */
	private static List<MProperty> ParseProperties(CSSStyleRuleImpl styleRule, Set<Defect> w3cErrors)
	{
		CSSStyleDeclarationImpl styleDeclaration = (CSSStyleDeclarationImpl)styleRule.getStyle();
		return styleDeclaration.getProperties().stream()
				.map(property -> new MProperty(property.getName(), property.getValue().getCssText(), property.isImportant(), TryFindW3cError(property, w3cErrors)))
				.collect(Collectors.toList());
	}



	/**
	 *
	 * @param property
	 * @param w3cErrors
	 * @return
	 */
	private static String TryFindW3cError(Property property, Set<Defect> w3cErrors)
	{
		Locator loc = (Locator)property.getUserData(UserDataConstants.KEY_LOCATOR);
		Optional<Defect> match = w3cErrors.stream().filter(error -> error.line() == loc.getLineNumber()
																&& (error.message().contains(property.getName())
																|| error.message().contains(property.getValue().getCssText())))
				                                   .findFirst();
		if(match.isPresent())
			return match.get().message();

		return "";
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
	 * Remove the given list of selectors from the _selectors
	 * @param selectors the list of selectors ts to remove
	 */
	public void RemoveSelectors(List<MSelector> selectors)
	{
		_selectors.removeAll(selectors);
	}


	/**
	 *
	 */
	public void ReplaceSelector(MSelector oldS, MSelector newS)
	{
		_selectors.remove(oldS);
		_selectors.add(newS);
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
	 * It is possible that the selectors in this rule have varying properties, due to previous analyses and filters
	 * So we need to find selectors for this rule that have properties in common and output them as a group
	 * @return
	 */
	public String Print()
	{
		Map<String, MTuple> combinations = new HashMap<>();
		for(MSelector mSelector : _selectors)
		{
			List<MProperty> mProps = mSelector.GetProperties();

			final String[] key = {""};
			mProps.forEach(mProp -> key[0] += "|" + mProp.AsKey());

			if(combinations.containsKey(key[0]))
			{
				combinations.get(key[0]).AddSelector(mSelector);
			}
			else
			{
				combinations.put(key[0], new MTuple(mSelector, mProps));
			}
		}

		SuiteStringBuilder builder = new SuiteStringBuilder();
		for(String key : combinations.keySet())
		{
			MTuple mTuple = combinations.get(key);
			List<MSelector> mSelectors = mTuple.GetSelectors();

			int size = mSelectors.size();
			for(int i = 0; i < size; i++)
			{
				builder.append(mSelectors.get(i).GetSelectorText());
				if(i < size - 1)
					builder.append(", ");
			}

			builder.append(" {");
			for(MProperty mProp : mTuple.GetProperties())
			{
				builder.appendLine("\t" + mProp.toString());
			}
			builder.appendLine("}\n\n");
		}

		return builder.toString();
	}


	@Override
	public String toString() {

		SuiteStringBuilder buffer = new SuiteStringBuilder();
		Locator locator = GetLocator();

		buffer.append("[McssRule] line=" + locator.getLineNumber() + " col=" + locator.getColumnNumber());
		buffer.appendLine("rule: " + _rule.getCssText() + "\n");

		return buffer.toString();
	}
}