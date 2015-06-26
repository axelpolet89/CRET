package com.crawljax.plugins.csssuite.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssMediaRule;
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
	private int _ruleOrder;

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
		_ruleOrder = 1;
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
				w3cWarnings = new HashSet<>(response.warnings());
				w3cErrors = new HashSet<>(response.errors());
			}
			catch (IOException e)
			{
				LogHandler.error(e, "Error occurred while validating the CSS code for file '%s'", url);
			}

			LogHandler.info("[W3C Validator] # errors found: %d", w3cErrors.size());
			LogHandler.info("[W3C Validator] # warnings found: %d", w3cWarnings.size());
		}

		List<MCssRule> styleAndMediaRules = new ArrayList<>();
		List<MCssMediaRule> mediaRules = new ArrayList<>();
		List<MCssRuleBase> ignoredRules = new ArrayList<>();

		for (int i = 0; i < ruleList.getLength(); i++)
		{
			try
			{
				AbstractCSSRuleImpl rule = (AbstractCSSRuleImpl)ruleList.item(i);
				if(rule instanceof CSSStyleRuleImpl)
				{
					styleAndMediaRules.add(new MCssRule((CSSStyleRuleImpl) rule, w3cErrors));
				}
				else if (rule instanceof CSSMediaRuleImpl)
				{
					// pass styleRules and ignoredRules also, to collect any of them within the media-query
					mediaRules.add(RecursiveParseMediaRules(rule, null, styleAndMediaRules, ignoredRules, w3cErrors));
				}
				else
				{
					ignoredRules.add(new MCssRuleBase(rule));
				}
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "Error occurred while parsing CSSRules into MCssRules on rule '%s'", ruleList.item(i).getCssText());
			}

			_ruleOrder++;
		}

		return new MCssFile(url, styleAndMediaRules, mediaRules, ignoredRules);
	}


	/**
	 * Recursively parse a media rule by iterating it's inner rules
	 * @param rule
	 * @param w3cErrors
	 * @param ignoredRules any rule that resides inside the given MediaRule,
	 *                        that is not regular style or another media, should be ignored
	 * @return List of parsed media rules
	 */
	private MCssMediaRule RecursiveParseMediaRules(AbstractCSSRuleImpl rule, MCssMediaRule parent, List<MCssRule> styleRules, List<MCssRuleBase> ignoredRules, Set<Defect> w3cErrors)
	{

		CSSMediaRuleImpl mediaRule = (CSSMediaRuleImpl) rule;

		MediaListImpl list = (MediaListImpl)mediaRule.getMedia();
		List<MediaQuery> queries = new ArrayList<>();

		// in case of nested media-queries, also include other queries to next media-rule
		if(parent != null)
		{
			queries.addAll(parent.GetMediaQueries());
		}

		for(int i = 0; i < list.getLength(); i++)
		{
			queries.add(list.mediaQuery(i));
		}

		MCssMediaRule result = new MCssMediaRule(mediaRule, queries, parent);
		_ruleOrder++;

		List<MCssRuleBase> innerRules = new ArrayList<>();

		CSSRuleList ruleList = mediaRule.getCssRules();
		for (int i = 0; i < ruleList.getLength(); i++)
		{
			try
			{
				AbstractCSSRuleImpl innerRule = (AbstractCSSRuleImpl)ruleList.item(i);
				if(innerRule instanceof CSSStyleRuleImpl)
				{
					MCssRule styleRule = new MCssRule((CSSStyleRuleImpl)innerRule, w3cErrors, queries, result);
					styleRules.add(styleRule);
					innerRules.add(styleRule);
				}
				else if (innerRule instanceof CSSMediaRuleImpl)
				{
					innerRules.add(RecursiveParseMediaRules(innerRule, result, styleRules, ignoredRules, w3cErrors));
				}
				else
				{
					ignoredRules.add(new MCssRuleBase(innerRule, queries, result));
				}
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "Error occurred while parsing CSSRules into MCssRules on rule '%s'", ruleList.item(i).getCssText());
			}

			_ruleOrder++;
		}

		result.SetInnerRules(innerRules);
		return result;
	}
}