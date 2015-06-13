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
	private final CSSStyleRuleImpl _rule;
	private final List<MSelector> _selectors;
	private final Locator _locator;


	/**
	 * Constructor for any rule contained in one or more media-queries
	 * @param rule
	 * @param w3cErrors
	 * @param queries
	 */
	public MCssRule(CSSStyleRuleImpl rule, Set<Defect> w3cErrors, List<MediaQuery> queries)
	{
		_rule = rule;
		_selectors = new ArrayList<>();
		_locator = (Locator)_rule.getUserData(UserDataConstants.KEY_LOCATOR);

		SetSelectors(w3cErrors, queries);
	}


	/**
	 * Constructor for regular rules, not contained in media-queries
	 */
	public MCssRule(CSSStyleRuleImpl rule, Set<Defect> w3cErrors)
	{
		this(rule, w3cErrors, new ArrayList<>());
	}


	/**
	 * Parse all selectors from this _rule and add them to the _selectors
	 */
	private void SetSelectors(Set<Defect> w3cErrors, List<MediaQuery> queries)
	{
		_selectors.addAll(((SelectorListImpl) _rule.getSelectors())
							.getSelectors().stream()
							.map(selector -> new MSelector(selector, ParseProperties(_rule, w3cErrors), GetLocator().getLineNumber(), queries))
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
	 * Find out if the given property is related to a W3C validation error
	 * @param property
	 * @param w3cErrors
	 * @return W3C error, if present for given property
	 */
	private static String TryFindW3cError(Property property, Set<Defect> w3cErrors)
	{
		Locator loc = (Locator)property.getUserData(UserDataConstants.KEY_LOCATOR);
		Optional<Defect> match = w3cErrors.stream().filter(error -> error.line() == loc.getLineNumber()
																&& (error.message().contains(property.getName())
																|| error.message().contains(property.getValue().getCssText())))
				                                   .findFirst();
		if(match.isPresent())
		{
			return match.get().message();
		}

		return "";
	}


	/** Getter */
	public CSSStyleRuleImpl GetRule()
	{
		return _rule;
	}

	/** Getter */
	public List<MSelector> GetSelectors()
	{
		return _selectors;
	}

	/** Getter */
	public Locator GetLocator()
	{
		return _locator;
	}


	/**
	 * @return the _selectors that are not matched (no associated DOM elements have been detected)
	 */
	public List<MSelector> GetUnmatchedSelectors()
	{
		return _selectors.stream().filter(selector -> !selector.IsMatched() && !selector.IsIgnored()).collect(Collectors.toList());
	}


	/**
	 * @return the _selectors that are effective (associated DOM elements have been detected)
	 */
	public List<MSelector> GetMatchedSelectors()
	{
		return _selectors.stream().filter(selector -> selector.IsMatched() && !selector.IsIgnored()).collect(Collectors.toList());
	}


	/**
	 * Remove the given list of selectors from the _selectors
	 */
	public void RemoveSelectors(List<MSelector> selectors)
	{
		_selectors.removeAll(selectors);
	}


	/**
	 * Replace selector with another, used in selector transformations
	 */
	public void ReplaceSelector(MSelector oldS, MSelector newS)
	{
		_selectors.remove(oldS);
		_selectors.add(newS);
	}


	@Override
	public String toString() {

		SuiteStringBuilder builder = new SuiteStringBuilder();

		builder.append("[MCssRule] line=%d, col=%d", _locator.getLineNumber(), _locator.getColumnNumber());
		builder.appendLine("rule: %s", _rule.getCssText());

		return builder.toString();
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


	private class MTuple
	{
		private final List<MSelector> _selectors;
		private final List<MProperty> _properties;

		public MTuple(MSelector mSelector, List<MProperty> properties)
		{
			_selectors = Arrays.asList(mSelector);
			_properties = properties;
		}

		public void AddSelector(MSelector selector)
		{
			_selectors.add(selector);
		}

		public List<MSelector> GetSelectors()
		{
			return _selectors;
		}

		public List<MProperty> GetProperties()
		{
			return _properties;
		}
	}
}