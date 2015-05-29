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

import com.crawljax.plugins.csssuite.data.MMediaRule;
import com.jcabi.w3c.Defect;
import com.jcabi.w3c.ValidationResponse;

import com.steadystate.css.dom.CSSMediaRuleImpl;
import com.steadystate.css.dom.CSSStyleRuleImpl;

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
		MCssFile mCssFile = new MCssFile(url);
		List<MCssRule> mCssRules = new ArrayList<>();
		List<MMediaRule> mediaRules = new ArrayList<>();

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

		for (int i = 0; i < ruleList.getLength(); i++)
		{
			try
			{
				CSSRule rule = ruleList.item(i);
				if(rule instanceof CSSStyleRuleImpl)
				{
					mCssRules.add(new MCssRule(rule, w3cErrors));
				}
				else if (rule instanceof CSSMediaRuleImpl)
				{
					mediaRules.add(new MMediaRule(rule, w3cErrors));
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

		mCssFile.SetRegularRules(mCssRules);
		mCssFile.SetMediaRules(mediaRules);

		return mCssFile;
	}
}