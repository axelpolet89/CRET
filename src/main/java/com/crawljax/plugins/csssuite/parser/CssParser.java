package com.crawljax.plugins.csssuite.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssRule;

import com.crawljax.plugins.csssuite.data.MMediaRule;
import com.steadystate.css.dom.CSSMediaRuleImpl;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import org.w3c.css.css.CssStyleRule;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

public class CssParser
{
	private final ParserErrorHandler _errorHandler;

	/**
	 * Constructor
	 */
	public CssParser()
	{
		_errorHandler = new ParserErrorHandler();
	}


	/**
	 *
	 * @return
	 */
	public List<String> GetParseErrors()
	{
		return _errorHandler.PrintParseErrors();
	}

	/**
	 * Parse css code using the CSSOMParser and a SACParserCSS3 to support CSS3 rules
	 * @param cssCode
	 * @return a CSSRuleList which contains objects which adhere to the org.w3c.dom.css specification
	 */
	private CSSRuleList ParseCssCode(String cssCode)
	{
		InputSource source = new InputSource(new StringReader(cssCode));
		CSSOMParser cssomParser = new CSSOMParser(new SACParserCSS3());
		cssomParser.setErrorHandler(_errorHandler);

		CSSRuleList rules = null;
		try
		{
			CSSStyleSheet css = cssomParser.parseStyleSheet(source, null, null);
			rules = css.getCssRules();
		}
		catch (Exception ex)
		{
			LogHandler.error(ex);
		}

		return rules;
	}

	/**
	 * @param cssCode the css code
	 * @return a list of MCssRule objects, of which each wraps a cssrule
	 */
	public List<MCssRule> ParseCssIntoMCssRules(String cssCode)
	{
		List<MCssRule> mCssRules = new ArrayList<>();
		List<MMediaRule> mediaRules = new ArrayList<>();

		CSSRuleList ruleList = ParseCssCode(cssCode);

		for (int i = 0; i < ruleList.getLength(); i++)
		{
			try
			{
				CSSRule rule = ruleList.item(i);
				if(rule instanceof CSSStyleRuleImpl)
				{
					mCssRules.add(new MCssRule(rule));
				}
				else if (rule instanceof CSSMediaRuleImpl)
				{
					mediaRules.add(new MMediaRule(rule));
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

		return mCssRules;
	}
}