package com.crawljax.plugins.cilla.analysis;

import java.util.*;

import com.steadystate.css.parser.SelectorListImpl;
import org.w3c.css.sac.Locator;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;

import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.userdata.UserDataConstants;

public class MCssRule {

	private CSSRule rule;
	private List<MSelector> selectors;

	/**
	 * Constructor.
	 * 
	 * @param rule
	 *            the CSS rule.
	 */
	public MCssRule(CSSRule rule) {

		this.rule = rule;
		selectors = new ArrayList<>();

		setSelectors();
	}

	public CSSRule getRule() {
		return rule;
	}

	public List<MSelector> getSelectors() {
		return selectors;
	}

	private void setSelectors() {
		if (this.rule instanceof CSSStyleRuleImpl) {
			CSSStyleRuleImpl styleRuleImpl = (CSSStyleRuleImpl) rule;

			List<MProperty> props = getProperties();

			SelectorListImpl list = (SelectorListImpl)styleRuleImpl.getSelectors();

			for(org.w3c.css.sac.Selector selector : list.getSelectors())
			{
				selectors.add(new MSelector(selector, props, this.getLocator().getLineNumber()));
			}

		}

	}

	public List<MProperty> getProperties() {
		CSSStyleDeclaration styleDeclaration = null;
		List<MProperty> properties = new ArrayList<MProperty>();

		if (this.rule instanceof CSSStyleRule) {
			CSSStyleRule styleRule = (CSSStyleRule) rule;
			styleDeclaration = styleRule.getStyle();

			for (int j = 0; j < styleDeclaration.getLength(); j++) {
				String property = styleDeclaration.item(j);
				String value = styleDeclaration.getPropertyCSSValue(property).getCssText();
				properties.add(new MProperty(property, value));
			}

		}

		return properties;
	}

	/**
	 * @return the CSS Style declaration of this rule.
	 */
	public CSSStyleDeclaration getStyleDeclaration() {
		CSSStyleDeclaration styleDeclaration = null;

		if (this.rule instanceof CSSStyleRule) {
			CSSStyleRule styleRule = (CSSStyleRule) rule;
			styleDeclaration = styleRule.getStyle();

			for (int j = 0; j < styleDeclaration.getLength(); j++) {
				String property = styleDeclaration.item(j);
				System.out.println("property: " + property);
				System.out.println("value: "
				        + styleDeclaration.getPropertyCSSValue(property).getCssText());
			}

		}

		return styleDeclaration;
	}

	public static List<MCssRule> convertToMCssRules(CSSRuleList ruleList) {

		List<MCssRule> mCssRules = new ArrayList<MCssRule>();

		for (int i = 0; i < ruleList.getLength(); i++) {
			mCssRules.add(new MCssRule(ruleList.item(i)));
		}

		return mCssRules;
	}

	@Override
	public String toString() {

		StringBuffer buffer = new StringBuffer();
		Locator locator = getLocator();

		buffer.append("locator: line=" + locator.getLineNumber() + " col="
		        + locator.getColumnNumber() + "\n");
		buffer.append("Rule: " + rule.getCssText() + "\n");

		for (MSelector selector : this.selectors) {
			buffer.append(selector.toString());
		}

		return buffer.toString();
	}

	/**
	 * @return the selectors that are not matched (not associated DOM elements have been detected).
	 */
	public List<MSelector> getUnmatchedSelectors() {
		List<MSelector> unmatched = new ArrayList<MSelector>();

		for (MSelector selector : this.selectors) {
			if (!selector.isMatched() && !selector.isIgnored()) {
				unmatched.add(selector);
			}
		}

		return unmatched;

	}

	/**
	 * @return the selectors that are effective (associated DOM elements have been detected).
	 */
	public List<MSelector> getMatchedSelectors() {
		List<MSelector> effective = new ArrayList<MSelector>();

		for (MSelector selector : this.selectors) {
			if (selector.isMatched() && !selector.isIgnored()) {
				effective.add(selector);
			}
		}

		return effective;

	}

	/**
	 * @return the Locator of this rule (line number, column).
	 */
	public Locator getLocator() {
		if (this.rule instanceof CSSStyleRuleImpl) {
			return (Locator) ((CSSStyleRuleImpl) this.rule)
			        .getUserData(UserDataConstants.KEY_LOCATOR);
		}

		return null;
	}
}
