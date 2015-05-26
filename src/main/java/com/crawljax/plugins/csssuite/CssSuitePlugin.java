package com.crawljax.plugins.csssuite;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.crawljax.plugins.csssuite.analysis.*;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.generator.CssWriter;
import com.crawljax.plugins.csssuite.interfaces.ICssCrawlPlugin;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.util.CSSDOMHelper;
import com.crawljax.plugins.csssuite.parser.CssParser;

public class CssSuitePlugin implements OnNewStatePlugin, PostCrawlingPlugin
{
	private final List<String> _processedCssFiles = new ArrayList<>();
	private int _originalCssLOC;

	private final Map<String, MCssFile> _mcssFiles;

	private final List<ICssCrawlPlugin> _plugins;
	private final List<ICssPostCrawlPlugin> _postPlugins;

	private final File _outputFile = new File("output/csssuite" + String.format("%s", new SimpleDateFormat("ddMMyy-hhmmss").format(new Date())) + ".txt");

	public CssSuitePlugin()
	{
		DOMConfigurator.configure("log4j.xml");
		LogHandler.info("");
		LogHandler.info("==================================START NEW CSS-SUITE RUN=====================================");

		_originalCssLOC = 0;

		_mcssFiles = new HashMap<>();

		_plugins = new ArrayList<>();
		_postPlugins = new ArrayList<>();

		CssAnalyzer analyzer = new CssAnalyzer();

		_plugins.add(analyzer);
		_postPlugins.add(analyzer);
	}

	public void onNewState(CrawlerContext context, StateVertex newState)
	{
		LogHandler.info("[NEW STATE] %s", newState.getUrl());

		// if the external CSS files are not parsed yet, do so
		LogHandler.info("Parse CSS rules...");
		LinkedHashMap<String, Integer> stateFileOrder = ParseCssRulesForState(context, newState);

		// now we have all the CSS rules neatly parsed in "rules"
		//checkCssOnDom(newState);
		LogHandler.info("Execute crawl-time transformations...");
		ExecuteCrawlTransformations(newState, stateFileOrder);
	}


	/**
	 *
	 * @param context
	 * @param state
	 * @return
	 */
	private LinkedHashMap<String, Integer> ParseCssRulesForState(CrawlerContext context, StateVertex state)
	{
		final String url = context.getBrowser().getCurrentUrl();
		final LinkedHashMap<String, Integer> stateFileOrder = new LinkedHashMap<>();

		try
		{
			final Document dom = state.getDocument();

			int order = 0;
			for (String relPath : CSSDOMHelper.ExtractCssFileNames(dom))
			{
				String cssUrl = CSSDOMHelper.GetAbsPath(url, relPath);

				if (!_mcssFiles.containsKey(cssUrl))
				{
					LogHandler.info("[NEW CSS FILE] " + cssUrl);

					String cssCode = CSSDOMHelper.GetUrlContent(cssUrl);
					_originalCssLOC += CountLOC(cssCode);

					ParseCssRules(cssUrl, cssCode);
				}

				//retain order of css files referenced in DOM
				stateFileOrder.put(cssUrl, order);
				order++;
			}

			// get all the embedded <STYLE> rules, save per HTML page
			if (!_mcssFiles.containsKey(url))
			{
				String embeddedRules = CSSDOMHelper.ParseEmbeddedStyles(dom);

				if(!embeddedRules.isEmpty())
					LogHandler.info("[NEW EMBEDDED RULES] " + url);

				_originalCssLOC += CountLOC(embeddedRules);
				ParseCssRules(url, embeddedRules);
			}

			// embedded style sheet has higher order
			order++;
			stateFileOrder.put(url, order);
		}
		catch (IOException e)
		{
			LogHandler.error(e.getMessage(), e);
		}

		return stateFileOrder;
	}


	/**
	 *
	 * @param url
	 * @param code
	 */
	private void ParseCssRules(String url, String code)
	{
		CssParser parser = new CssParser();
		MCssFile file = new MCssFile(url);

		int numberOfRules = parser.ParseCssIntoMCssRules(code, file);
		if (numberOfRules > 0)
		{
			_mcssFiles.put(url, file);
		}

		List<String> parseErrors = parser.GetParseErrors();
		if(parseErrors.size() > 0)
		{
			for(String parseError : parseErrors)
			{
				LogHandler.warn("[CSSPARSER] Parse errors occurred while parsing '%s'\n%s", url, parseError);
			}
		}

		LogHandler.info("Number of CSS rules parsed into McssRules: %d", numberOfRules);
	}


	/**
	 *
	 * @param state
	 */
	private void ExecuteCrawlTransformations(StateVertex state, LinkedHashMap<String, Integer> stateFileOrder)
	{
		Document dom;
		try
		{
			dom = state.getDocument();
		}
		catch (Exception ex)
		{
			LogHandler.error(ex.toString());
			return;
		}

		for(ICssCrawlPlugin plugin : _plugins)
		{
			try
			{
				plugin.Transform(state.getName(), dom, _mcssFiles, stateFileOrder);
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[CRAWL PLUG-IN] Error occurred in plugin %s", plugin);
			}
		}
	}

	private Map<String, MCssFile> ExecutePostTransformations()
	{
		Map<String, MCssFile> rules = _mcssFiles;
		for(ICssPostCrawlPlugin plugin : _postPlugins)
		{
			rules = plugin.Transform(_mcssFiles);
		}
		return rules;
	}


	private int CountLOC(String cssText) {
		int count = 0;
		cssText = cssText.replaceAll("\\{", "{\n");
		cssText = cssText.replaceAll("\\}", "}\n");
		cssText = cssText.replaceAll("\\}", "}\n");
		cssText = cssText.replaceAll("\\;", ";\n");

		if (cssText != null && !cssText.equals("")) {
			LineNumberReader ln = new LineNumberReader(new StringReader(cssText));
			try {
				while (ln.readLine() != null) {
					count++;
				}
			} catch (IOException e) {
				LogHandler.error(e.getMessage(), e);
			}
		}
		return count;
	}

	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitReason) {

		List<CloneDetector> cloneDetectors = new ArrayList<>();

		//todo: clonedetector
//		for(String cssFile : _cssFiles.keySet())
//		{
//			if(!_processedCssFiles.contains(cssFile))
//			{
//				_processedCssFiles.add(cssFile);
//				CloneDetector cd = new CloneDetector(cssFile);
//				cloneDetectors.add(cd);
//				cd.Detect(_cssRules.get(cssFile));
//			}
//		}

		int totalCssRules = 0;
		int totalCssSelectors = 0;
		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			totalCssRules += entry.getValue().GetRules().size();
			for (MCssRule mrule : entry.getValue().GetRules())
			{
				totalCssSelectors += mrule.GetSelectors().size();

			}
		}

		StringBuffer output = new StringBuffer();
		StringBuffer bufferUnused = new StringBuffer();
		StringBuffer bufferUsed = new StringBuffer();
		StringBuffer undefinedClasses = new StringBuffer();
		StringBuffer effective = new StringBuffer();
		StringBuffer ineffectiveBuffer = new StringBuffer();

		int used = PrintMatchedRules(bufferUsed);
		int unused = PrintUnmatchedRules(bufferUnused);

		Map<String, MCssFile> rules = ExecutePostTransformations();

		int effectiveInt = PrintEffectiveSelectors(effective);
		int ineffectiveInt = PrintIneffectiveSelectors(ineffectiveBuffer);

		output.append("Analyzed " + session.getConfig().getUrl() + " on "
		        + new SimpleDateFormat("dd/MM/yy-hh:mm:ss").format(new Date()) + "\n");

		output.append("-> Files with CSS code: " + _mcssFiles.keySet().size() + "\n");
		for (String address : _mcssFiles.keySet())
		{
			output.append("    Address: " + address + "\n");
		}
		// output.append("Total CSS Size: " + getTotalCssRulesSize() + " bytes" + "\n");

		output.append("-> LOC (CSS): " + _originalCssLOC + "\n");
		output.append("-> Total Defined CSS rules: " + totalCssRules + "\n");
		output.append("-> Total Defined CSS selectors: " + totalCssSelectors + " from which: \n");
		int ignored = totalCssSelectors - (unused + used);
		output.append("   -> Ignored (:link, :hover, etc):   " + ignored + "\n");
		output.append("   -> Unmatched: " + unused + "\n");
		output.append("   -> Matched:   " + used + "\n");
		output.append("   -> Ineffective: " + ineffectiveInt + "\n");
		output.append("   -> Effective: " + effectiveInt + "\n");
		output.append("-> Total Defined CSS Properties: " + CountAllProperties() + "\n");
		output.append("   -> Ignored Properties: " + CountIgnoredProperties() + "\n");
		output.append("   -> Unused Properties: " + CountUnusedProperties() + "\n");

		// output.append("-> Effective CSS Rules: " + CSSProxyPlugin.cssTraceSet.size() + "\n");

		// output.append("   -> Effective: " + effectiveInt + "\n");

		// output.append("->> Duplicate Selectors: " + duplicates + "\n\n");
		output.append("By deleting unused rules, css size reduced by: "
		        + Math.ceil((double) NewCssSizeBytes() / OriginalCssSizeBytes() * 100) + " percent"
		        + "\n");

		/*
		 * This is where the com.crawljax.plugins.csssuite.visualizer gets called.
		 */
		String tmpStr = new String();
		tmpStr = output.toString().replace("\n", "<br>");
		String url = session.getConfig().getUrl().toString();

		/* This is where the Visualizer plug-in is invoked */
/*		CillaVisualizer cv = new CillaVisualizer();
		cv.openVisualizer(url, tmpStr, cssRules, elementsWithNoClassDef);*/

		output.append(ineffectiveBuffer.toString());
		output.append(effective.toString());
		output.append(bufferUnused.toString());
		// output.append(bufferUsed.toString());
		output.append(undefinedClasses);
		// output.append(duplicateSelectors);

		StringBuffer clones = new StringBuffer();
		clones.append("CloneDetector checked " + _processedCssFiles.size() + " files.\n");
		clones.append(PrintFiles());

		for(CloneDetector cd : cloneDetectors)
		{
			clones.append("\n\n Clone Report for file " + cd.GetFile());
			clones.append("-> Found " + cd.CountClones() + " duplicate properties.\n");
			clones.append(cd.PrintClones());
		}
		output.append(clones.toString());

		try
		{
			FileUtils.writeStringToFile(_outputFile, output.toString());
		}
		catch (IOException e)
		{
			LogHandler.error(e.getMessage(), e);
		}


		CssWriter writer = new CssWriter();
		for(String file : rules.keySet())
		{
			try
			{
				writer.Generate(file, rules.get(file).GetRules());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
		}
	}


	/**
	 *
	 * @return
	 */
	private String PrintFiles()
	{
		StringBuilder builder = new StringBuilder();
		for(String file : _processedCssFiles)
		{
			builder.append("#" + _processedCssFiles.indexOf(file) + ": " + file + "\n");
		}
		return builder.toString();
	}


	/**
	 *
	 * @param buffer
	 * @return
	 */
	private int PrintEffectiveSelectors(StringBuffer buffer) {

		int counterEffectiveSelectors = 0;
		buffer.append("\n========== EFFECTIVE CSS SELECTORS ==========\n");

		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			List<MCssRule> rules = entry.getValue().GetRules();

			buffer.append("\n== IN CSS: " + entry.getKey() + "\n");

			for (MCssRule rule : rules)
			{
				List<MSelector> selectors = rule.GetMatchedSelectors();

				for (MSelector selector : selectors)
				{
					if (selector.HasEffectiveProperties())
					{
						buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");
						buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");
						buffer.append(" Selector: " + selector.GetSelectorText() + "\n");

						counterEffectiveSelectors++;

						for (MProperty prop : selector.GetProperties())
						{
							buffer.append(" Property " + prop + "\n");
						}
					}

					buffer.append("\n");
				}
			}
		}

		return counterEffectiveSelectors;
	}


	/**
	 *
	 * @param buffer
	 * @return
	 */
	private int PrintIneffectiveSelectors(StringBuffer buffer) {

		int counterIneffectiveSelectors = 0;
		buffer.append("========== INEFFECTIVE CSS SELECTORS ==========\n");

		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			List<MCssRule> rules = entry.getValue().GetRules();

			buffer.append("== IN CSS: " + entry.getKey() + "\n");

			for (MCssRule rule : rules)
			{
				for (MSelector selector : rule.GetMatchedSelectors())
				{
					if (!selector.HasEffectiveProperties() && !selector.IsIgnored())
					{
						buffer.append("Ineffective: ");
						buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");

						buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");
						buffer.append(" Selector: " + selector.GetSelectorText() + "\n\n");

						counterIneffectiveSelectors++;
					}
				}
			}
		}

		return counterIneffectiveSelectors;
	}


	/**
	 *
	 * @param buffer
	 * @return
	 */
	private int PrintUnmatchedRules(StringBuffer buffer)
	{
		buffer.append("========== UNMATCHED CSS RULES ==========\n");
		int counter = 0;

		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			List<MCssRule> rules = entry.getValue().GetRules();

			buffer.append("== UNMATCHED RULES IN: " + entry.getKey() + "\n");
			for (MCssRule rule : rules)
			{
				List<MSelector> selectors = rule.GetUnmatchedSelectors();
				counter += selectors.size();

				if (selectors.size() > 0)
				{
					buffer.append("Unmatched: ");
					buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");
					buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");

					for (MSelector selector : selectors)
					{
						buffer.append(selector.toString() + "\n");
					}
				}
			}
		}

		return counter;
	}


	/**
	 *
	 * @param buffer
	 * @return
	 */
	private int PrintMatchedRules(StringBuffer buffer)
	{
		buffer.append("========== MATCHED CSS RULES ==========\n");

		int counter = 0;
		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			List<MCssRule> rules = entry.getValue().GetRules();

			buffer.append("== MATCHED RULES IN: " + entry.getKey() + "\n");
			for (MCssRule rule : rules)
			{
				List<MSelector> selectors = rule.GetMatchedSelectors();
				counter += selectors.size();

				if (selectors.size() > 0)
				{
					buffer.append("Matched: ");
					buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");
					buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");

					for (MSelector selector : selectors)
					{
						buffer.append(selector.toString() + "\n");
					}
				}
			}

		}

		return counter;
	}


	/**
	 *
	 * @return
	 */
	private int CountAllProperties()
	{
		int counter = 0;
		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			for (MCssRule rule : entry.getValue().GetRules())
			{
				for (int i = 0; i < rule.GetSelectors().size(); i++)
				{
					counter += rule.ParseProperties().size();
				}
			}
		}

		return counter;
	}


	/**
	 *
	 * @return
	 */
	private int CountUnusedProperties() {

		int counter = 0;

		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			List<MCssRule> rules = entry.getValue().GetRules();

			for (MCssRule rule : rules)
			{
				List<MSelector> selectors = rule.GetSelectors();

				for (MSelector selector : selectors)
				{
					if (!selector.IsIgnored())
					{
						for (MProperty prop : selector.GetProperties())
						{
							if (!prop.IsEffective()) {
								counter++;
							}
						}
					}
				}
			}
		}

		return counter;
	}

	/**
	 *
	 * @return
	 */
	private int CountIgnoredProperties()
	{
		int counter = 0;
		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			for (MCssRule rule : entry.getValue().GetRules())
			{
				List<MSelector> selectors = rule.GetSelectors();
				for (MSelector selector : selectors)
				{
					if (selector.IsIgnored())
					{
						counter += rule.ParseProperties().size();
					}
				}
			}
		}

		return counter;
	}


	/**
	 *
	 * @return
	 */
	private int OriginalCssSizeBytes()
	{
		int result = 0;

		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			for (MCssRule mRule : entry.getValue().GetRules())
			{
				result += mRule.GetRule().getCssText().trim().replace("{", "").replace("}", "")
				                .replace(",", "").replace(" ", "").getBytes().length;

			}
		}

		return result;
	}


	/**
	 *
	 * @return
	 */
	private int NewCssSizeBytes()
	{
		boolean effective;
		boolean exit = false;
		int result = 0;

		int counter = 0;
		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
		{
			for (MCssRule mRule : entry.getValue().GetRules())
			{
				List<MSelector> selector = mRule.GetSelectors();
				for (int i = 0; i < selector.size(); i++)
				{
					if (!selector.get(i).IsIgnored())
					{
						exit = true;

						List<MProperty> property = selector.get(i).GetProperties();
						for (int j = 0; j < property.size(); j++)
						{
							if (!property.get(j).IsEffective())
							{
								effective = false;
								for (int k = i + 1; k < selector.size(); k++)
								{
									if (!selector.get(k).IsIgnored()) {
										if (selector.get(k).GetProperties().get(j).IsEffective())
										{
											effective = true;
											break;
										}
									}
								}
								if (!effective)
								{
									counter++;
									result += property.get(j).ComputeSizeBytes();
								}
							}
						}

					}

					if (exit)
					{
						if (counter == selector.get(i).GetProperties().size())
							result += selector.get(i).ComputeSizeBytes();
						break;
					}
				}
			}
		}

		return result;
	}
}