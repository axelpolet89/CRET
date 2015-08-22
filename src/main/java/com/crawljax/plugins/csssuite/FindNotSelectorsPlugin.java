package com.crawljax.plugins.csssuite;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.parser.CssParser;
import com.crawljax.plugins.csssuite.util.CSSDOMHelper;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class FindNotSelectorsPlugin implements OnNewStatePlugin, PostCrawlingPlugin
{
	/* Configuration properties */
	public final boolean _enableW3cValidation;

	private final String _siteName;
	private final String _siteIndex;

	private final Map<String, MCssFile> _cssFiles;
	private final Map<String, MCssFile> _embeddedStyles;

	private final Map<String, List<String>> _parserErrors;

	private final FileWriter _outputWriter;
	private final FileWriter _detailWriter;

	public FindNotSelectorsPlugin(String siteName, String siteIndex, FileWriter outputWriter, FileWriter outputDetails)
	{
		_siteName = siteName;
		_siteIndex = siteIndex;
		_outputWriter = outputWriter;
		_detailWriter = outputDetails;

		_enableW3cValidation = false;

		LogHandler.info("");
		LogHandler.info("==================================START NEW FIND-NOT-SELECTORS RUN=====================================");
		LogHandler.info("TARGET: %s at URL %s", _siteName, this._siteIndex);

		_cssFiles = new HashMap<>();
		_embeddedStyles = new HashMap<>();
		_parserErrors = new HashMap<>();
	}

	public void onNewState(CrawlerContext context, StateVertex newState)
	{
		LogHandler.info("[NEW STATE] %s", newState.getUrl());

		// if the external CSS files are not parsed yet, do so
		LogHandler.info("Parse CSS rules...");
		ParseCssRulesForState(context, newState);
	}


	/**
	 *
	 * @param context
	 * @param state
	 * @return
	 */
	private void ParseCssRulesForState(CrawlerContext context, StateVertex state)
	{
		final String url = context.getBrowser().getCurrentUrl();

		try
		{
			final Document dom = state.getDocument();

			for (String relPath : CSSDOMHelper.ExtractCssFileNames(dom))
			{
				String cssUrl = CSSDOMHelper.GetAbsPath(url, relPath);

				if (!_cssFiles.containsKey(cssUrl))
				{
					LogHandler.info("[FOUND NEW CSS FILE] " + cssUrl);

					String cssCode = CSSDOMHelper.GetUrlContent(cssUrl);

					_cssFiles.put(cssUrl, ParseCssRules(cssUrl, cssCode));
				}
			}

			// get all the embedded <STYLE> rules, save per HTML page
			if (!_embeddedStyles.containsKey(url))
			{
				String embeddedCode = CSSDOMHelper.ParseEmbeddedStyles(dom);

				if(!embeddedCode.isEmpty())
				{
					LogHandler.info("[FOUND NEW EMBEDDED RULES] " + url);
				}

				MCssFile embeddedFile = ParseCssRules(url, embeddedCode);

				if(!embeddedAlreadyParsed(embeddedFile))
				{
					_embeddedStyles.put(url, embeddedFile);
				}
			}
		}
		catch (Exception ex)
		{
			LogHandler.error(ex);
		}
	}


	private boolean embeddedAlreadyParsed(MCssFile check)
	{
		boolean result = false;

		for(MCssFile mCssFile : _embeddedStyles.values())
		{
			List<MCssRule> existingRules = mCssFile.GetRules();
			List<MCssRule> checkRules = check.GetRules();

			if(existingRules.size() != checkRules.size())
			{
				continue;
			}

			boolean equals = true;

			for(int i = 0; i < existingRules.size(); i++)
			{
				if(!existingRules.get(i).toString().equalsIgnoreCase(checkRules.get(i).toString()))
				{

					equals = false;
					break;
				}
			}

			if(equals)
			{
				result = true;
				break;
			}
		}

		return result;
	}


	/**
	 *
	 * @param url
	 * @param code
	 */
	private MCssFile ParseCssRules(String url, String code)
	{
		CssParser parser = new CssParser(_enableW3cValidation);

		MCssFile file = parser.ParseCssIntoMCssRules(url, code);

		LogHandler.info("[CssParser] Parsed '%s' -> CSS rules parsed into McssRules: %d", url, file.GetRules().size());

		_parserErrors.put(url, parser.GetParseErrors());

		return file;
	}


	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitReason)
	{
		SuiteStringBuilder builder = new SuiteStringBuilder();
		builder.append("<site>");
		builder.appendLine("\t<site_name>%s</site_name>", _siteName);

		SuiteStringBuilder detailBuilder = new SuiteStringBuilder();
		detailBuilder.append("<site>");
		detailBuilder.appendLine("\t<site_name>%s</site_name>", _siteName);

		int totalForSite = 0;
		int notForSite = 0;

		for(String fileName : _cssFiles.keySet())
		{
			int totalSelectors = 0;
			int notSelectors = 0;

			for(MCssRule mCssRule : _cssFiles.get(fileName).GetRules())
			{
				for(MSelector mSelector : mCssRule.GetSelectors())
				{
					if(mSelector.GetSelectorText().contains(":not"))
					{
						notSelectors++;
					}

					totalSelectors++;
				}
			}

			detailBuilder.appendLine("\t\t<file>");
			detailBuilder.appendLine("\t\t\t<css_file>%s</css_file>", encodeUrl(fileName));
			detailBuilder.appendLine("\t\t\t<embedded_file></embedded_file>");
			detailBuilder.appendLine("\t\t\t<total_selectors>%d</total_selectors>", totalSelectors);
			detailBuilder.appendLine("\t\t\t<not_selectors>%d</not_selectors>", notSelectors);
			detailBuilder.appendLine("\t\t</file>");

			//LogHandler.info("file:\t\t%s\ntotal selectors:\t%d\n:not selectors:\t%d\npercentage:\t%.2f", fileName, totalSelectors, notSelectors, percentage);

			totalForSite += totalSelectors;
			notForSite += notSelectors;
		}

		for(String fileName : _embeddedStyles.keySet())
		{
			int totalSelectors = 0;
			int notSelectors = 0;

			for(MCssRule mCssRule : _embeddedStyles.get(fileName).GetRules())
			{
				for(MSelector mSelector : mCssRule.GetSelectors())
				{
					if(mSelector.GetSelectorText().contains(":not"))
					{
						notSelectors++;
					}

					totalSelectors++;
				}
			}

			detailBuilder.appendLine("\t\t<file>");
			detailBuilder.appendLine("\t\t\t<css_file></css_file>");
			detailBuilder.appendLine("\t\t\t<embedded_file>%s</embedded_file>", encodeUrl(fileName));
			detailBuilder.appendLine("\t\t\t<total_selectors>%d</total_selectors>", totalSelectors);
			detailBuilder.appendLine("\t\t\t<not_selectors>%d</not_selectors>", notSelectors);
			detailBuilder.appendLine("\t\t</file>");

			totalForSite += totalSelectors;
			notForSite += notSelectors;
		}

		builder.appendLine("\t<total_for_site>%d</total_for_site>", totalForSite);
		builder.appendLine("\t<not_for_site>%d</not_for_site>", notForSite);
		builder.appendLine("</site>");

		detailBuilder.appendLine("</site>");

		try
		{
			_outputWriter.append(builder.toString());
			_outputWriter.append("\n\n");
			_outputWriter.flush();

			_detailWriter.append(detailBuilder.toString());
			_detailWriter.append("\n\n");
			_detailWriter.flush();
		}
		catch (IOException e)
		{
			LogHandler.error(e, "Error occurred while writing :not analysis for site %s", _siteName);
		}

		LogHandler.info("\n\n");

		for(String fileName : _parserErrors.keySet())
		{
			List<String> parseErrors = _parserErrors.get(fileName);

			if(parseErrors.size() > 0)
			{
				LogHandler.warn("[PARSE ERRORS] Parse errors occurred for file '%s'", fileName);
				for (String parseError : parseErrors)
				{
					LogHandler.warn("[ERROR DETAILS] ", parseError);
				}
			}
		}
	}

	private static String encodeUrl(String url)
	{
		try
		{
			return URLEncoder.encode(url, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			LogHandler.error(e, "URL encode error on url '%s'", url);
		}

		return url;
	}
}