package com.crawljax.plugins.csssuite.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;

import com.crawljax.plugins.csssuite.data.MCssRuleBase;
import com.jcabi.w3c.Defect;
import com.jcabi.w3c.ValidationResponse;

import com.steadystate.css.dom.*;

import com.steadystate.css.parser.media.MediaQuery;
import com.sun.webkit.dom.CSSRuleImpl;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

public class CssParser
{
	private final ParserErrorHandler _errorHandler;
	private final boolean _doW3cValidation;


	/**
	 * Test constructor
	 */
	public CssParser()
	{
		this(false);
	}


	/**
	 * Constructor
	 */
	public CssParser(boolean doW3cValidation)
	{
		_errorHandler = new ParserErrorHandler();
		_doW3cValidation = doW3cValidation;
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
		catch (IOException ex)
		{
			LogHandler.error(ex);
		}

		return rules;
	}


	/**
	 * @param cssCode the css code
	 * @return a list of MCssRule objects, of which each wraps a cssrule
	 */
	public MCssFile ParseCssIntoMCssRules(String url, String cssCode)
	{
		Set<Defect> w3cWarnings = new HashSet<>();
		Set<Defect> w3cErrors = new HashSet<>();

		CSSRuleList ruleList = ParseCssCode(cssCode);

		if(_doW3cValidation && ruleList.getLength() > 0)
		{
			ValidationResponse response = null;
			try
			{
				response = CssValidator.ValidateW3C(cssCode);
				w3cWarnings = response.warnings();
				w3cErrors = response.errors();
			}
			catch (IOException e)
			{
				LogHandler.error(e, "Error occurred while validating the CSS code for file '%s'", url);
			}

			LogHandler.info("[W3C Validator] # errors found: %d", w3cErrors.size());
			LogHandler.info("[W3C Validator] # warnings found: %d", w3cWarnings.size());
		}

		List<MCssRule> mCssRules = new ArrayList<>();
		List<MCssRuleBase> ignoredRules = new ArrayList<>();

		for (int i = 0; i < ruleList.getLength(); i++)
		{
			try
			{
				CSSRule rule = ruleList.item(i);
				if(rule instanceof CSSStyleRuleImpl)
				{
					mCssRules.add(new MCssRule((CSSStyleRuleImpl)rule, w3cErrors));
				}
				else if (rule instanceof CSSMediaRuleImpl)
				{
					mCssRules.addAll(RecursiveParseMediaRules(rule, w3cErrors, ignoredRules));
				}
				else
				{
					ignoredRules.add(new MCssRuleBase((AbstractCSSRuleImpl)rule));
				}
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "Error occurred while parsing CSSRules into MCssRules on rule '%s'", ruleList.item(i).getCssText());
			}
		}


		return new MCssFile(url, mCssRules, ignoredRules);
	}


	/**
	 * Recursively parse a media rule by iterating it's inner rules
	 * @param rule
	 * @param w3cErrors
	 * @param ignoredRules any rule that resides inside the given MediaRule,
	 *                        that is not regular style or another media, should be ignored
	 * @return List of parsed media rules
	 */
	private static List<MCssRule> RecursiveParseMediaRules(CSSRule rule, Set<Defect> w3cErrors, List<MCssRuleBase> ignoredRules)
	{
		List<MCssRule> result = new ArrayList<>();

		CSSMediaRuleImpl mediaRule = (CSSMediaRuleImpl) rule;

		MediaListImpl list = (MediaListImpl)mediaRule.getMedia();
		List<MediaQuery> queries = new ArrayList<>();
		for(int i = 0; i < list.getLength(); i++)
		{
			queries.add(list.mediaQuery(i));
		}

		CSSRuleList ruleList = mediaRule.getCssRules();
		for (int i = 0; i < ruleList.getLength(); i++)
		{
			try
			{
				CSSRule innerRule = ruleList.item(i);
				if(innerRule instanceof CSSStyleRuleImpl)
				{
					result.add(new MCssRule((CSSStyleRuleImpl)innerRule, w3cErrors, queries));
				}
				else if (innerRule instanceof CSSMediaRuleImpl)
				{
					result.addAll(RecursiveParseMediaRules(innerRule, w3cErrors, ignoredRules));
				}
				else
				{
					ignoredRules.add(new MCssRuleBase((AbstractCSSRuleImpl)innerRule, queries));
				}
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "Error occurred while parsing CSSRules into MCssRules on rule '%s'", ruleList.item(i).getCssText());
			}
		}

		return result;
	}
}