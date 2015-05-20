package com.crawljax.plugins.cilla;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import com.crawljax.plugins.cilla.analysis.*;
import com.crawljax.plugins.cilla.data.*;
import com.crawljax.plugins.cilla.generator.CssWriter;
import com.crawljax.plugins.cilla.interfaces.ICssCrawlPlugin;
import com.crawljax.plugins.cilla.interfaces.ICssPostCrawlPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.cilla.util.CSSDOMHelper;
import com.crawljax.plugins.cilla.parser.CssParser;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class CillaPlugin implements OnNewStatePlugin, PostCrawlingPlugin {
	private static final Logger LOGGER = LogManager.getLogger(CillaPlugin.class.getName());

	private final List<String> _processedCssFiles = new ArrayList<>();
	private int _originalCssLOC;
	public int _numberOfStates;

	private final Map<String, CssParser> _cssFiles;
	private final Map<String, List<MCssRule>> _cssRules;
	private final SetMultimap<String, ElementWithClass> _elementsWithNoClassDef;

	private final List<ICssCrawlPlugin> _plugins;
	private final List<ICssPostCrawlPlugin> _postPlugins;

	private final File _outputFile = new File("output/cilla" + String.format("%s", new SimpleDateFormat("ddMMyy-hhmmss").format(new Date())) + ".txt");

	public CillaPlugin()
	{
		_originalCssLOC = 0;
		_numberOfStates = 0;

		_cssFiles = new HashMap<>();
		_cssRules = new HashMap<>();
		_elementsWithNoClassDef = HashMultimap.create();

		_plugins = new ArrayList<>();
		_postPlugins = new ArrayList<>();

		CssAnalyzer analyzer = new CssAnalyzer();

		_plugins.add(analyzer);
		_postPlugins.add(analyzer);
	}

	public void onNewState(CrawlerContext context, StateVertex newState) {
		// if the external CSS files are not parsed yet, do so
		ParseCssRulesForState(context, newState);

		// now we have all the CSS rules neatly parsed in "rules"
		//checkCssOnDom(newState);
		ExecuteCrawlTransformations(newState);

		checkClassDefinitions(newState);

		_numberOfStates++;
	}


	private void ParseCssRulesForState(CrawlerContext context, StateVertex state)
	{
		String url = context.getBrowser().getCurrentUrl();

		try
		{
			final Document dom = state.getDocument();

			for (String relPath : CSSDOMHelper.ExtractCssFileNames(dom))
			{
				String cssUrl = CSSDOMHelper.GetAbsPath(url, relPath);

				if (!_cssRules.containsKey(cssUrl))
				{
					LOGGER.info("CSS URL: " + cssUrl);

					String cssCode = CSSDOMHelper.GetUrlContent(cssUrl);
					_originalCssLOC += CountLOC(cssCode);

					ParseCssRules(url, cssCode);
				}
			}

			// get all the embedded <STYLE> rules, save per HTML page
			if (!_cssRules.containsKey(url))
			{
				String embeddedRules = CSSDOMHelper.ParseEmbeddedStyles(dom);
				_originalCssLOC += CountLOC(embeddedRules);
				ParseCssRules(url, embeddedRules);
			}

		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}


	private void ParseCssRules(String url, String code)
	{
		//store parser for later readout on parse warnings, errors and fatalErrors;
		CssParser parser = new CssParser();
		_cssFiles.put(url, parser);

		List<MCssRule> rules = parser.ParseCssIntoMCssRules(code);
		if (rules != null && rules.size() > 0)
		{
			_cssRules.put(url, rules);
		}
	}


	private void ExecuteCrawlTransformations(StateVertex state)
	{
		Document dom = null;
		try
		{
			dom = state.getDocument();
		}
		catch (IOException ex)
		{
			LOGGER.debug(ex.toString());
		}
		finally
		{
			for(ICssCrawlPlugin plugin : _plugins)
			{
				plugin.Transform(state.getName(), dom, _cssRules);
			}
		}
	}

	private Map<String, List<MCssRule>> ExecutePostTransformations()
	{
		Map<String, List<MCssRule>> rules = _cssRules;
		for(ICssPostCrawlPlugin plugin : _postPlugins)
		{
			rules = plugin.Transform(rules);
		}
		return rules;
	}

	private void checkClassDefinitions(StateVertex state) {
		LOGGER.info("Checking CSS class definitions...");
		try {

			List<ElementWithClass> elementsWithClass =
			        CSSDOMHelper.GetElementWithClass(state.getName(), state.getDocument());

			for (ElementWithClass element : elementsWithClass) {

				for (String classDef : element.GetClassValues()) {
					boolean matchFound = false;

					search: for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
						for (MCssRule rule : entry.getValue()) {
							for (MSelector selector : rule.GetSelectors()) {
								if (selector.GetSelectorText().contains("." + classDef)) {
									// TODO e.g. css: div.news { color: blue} <span><p>
									// if (selector.getSelectorText().startsWith("." + classDef)) {
									matchFound = true;
									break search;
									// }
								}
							}
						}
					}

					if (!matchFound) {
						element.AddUnmatchedClass(classDef);
						_elementsWithNoClassDef.put(element.getStateName(), element);

					}
				}
			}

		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (XPathExpressionException e) {
			LOGGER.error(e.getMessage(), e);
		}

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
				LOGGER.error(e.getMessage(), e);
			}
		}
		return count;
	}

	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitReason) {

		List<CloneDetector> cloneDetectors = new ArrayList<>();

		for(String cssFile : _cssRules.keySet()){
			if(!_processedCssFiles.contains(cssFile))
			{
				_processedCssFiles.add(cssFile);
				CloneDetector cd = new CloneDetector(cssFile);
				cloneDetectors.add(cd);
				cd.Detect(_cssRules.get(cssFile));
			}
		}

		int totalCssRules = 0;
		int totalCssSelectors = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			totalCssRules += entry.getValue().size();
			for (MCssRule mrule : entry.getValue()) {
				totalCssSelectors += mrule.GetSelectors().size();

			}
		}

		StringBuffer output = new StringBuffer();
		StringBuffer bufferUnused = new StringBuffer();
		StringBuffer bufferUsed = new StringBuffer();
		StringBuffer undefinedClasses = new StringBuffer();
		int used = getUsedRules(bufferUsed);
		int unused = getUnmatchedRules(bufferUnused);

		Map<String, List<MCssRule>> rules = ExecutePostTransformations();

		CssWriter writer = new CssWriter();
		for(String file : rules.keySet()) {
			try {
				writer.Generate(file, rules.get(file));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		//determine if matched selectors are effective
//		CssAnalyzer.analyzeCssSelectorEffectiveness();

		int undefClasses = getUndefinedClasses(undefinedClasses);
		StringBuffer effective = new StringBuffer();
		int effectiveInt = getEffectiveSelectorsBasedOnProps(effective);

		StringBuffer ineffectiveBuffer = new StringBuffer();
		int ineffectiveInt = getIneffectiveSelectorsBasedOnProps(ineffectiveBuffer);

		output.append("Analyzed " + session.getConfig().getUrl() + " on "
		        + new SimpleDateFormat("dd/MM/yy-hh:mm:ss").format(new Date()) + "\n");

		output.append("-> Files with CSS code: " + _cssRules.keySet().size() + "\n");
		for (String address : _cssRules.keySet()) {
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
		output.append("-> Total Defined CSS Properties: " + countProperties() + "\n");
		output.append("   -> Ignored Properties: " + countIgnoredProperties() + "\n");
		output.append("   -> Unused Properties: " + countUnusedProps() + "\n");

		// output.append("-> Effective CSS Rules: " + CSSProxyPlugin.cssTraceSet.size() + "\n");

		// output.append("   -> Effective: " + effectiveInt + "\n");
		output.append("->> Undefined Classes: " + undefClasses + "\n");
		// output.append("->> Duplicate Selectors: " + duplicates + "\n\n");
		output.append("By deleting unused rules, css size reduced by: "
		        + Math.ceil((double) NewCssSizeBytes() / OriginalCssSizeBytes() * 100) + " percent"
		        + "\n");

		/*
		 * This is where the com.crawljax.plugins.cilla.visualizer gets called.
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

		try {
			FileUtils.writeStringToFile(_outputFile, output.toString());

		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
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
			builder.append("#" + _processedCssFiles.indexOf(file) + ": " + file + "\n");
		return builder.toString();
	}

	private int countProperties() {
		int counter = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			List<MCssRule> rules = entry.getValue();

			for (MCssRule rule : rules) {
				for (int i = 0; i < rule.GetSelectors().size(); i++)
					counter += rule.ParseProperties().size();
			}
		}

		return counter;

	}

	private int countIgnoredProperties() {
		int counter = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			for (MCssRule rule : entry.getValue()) {
				List<MSelector> selectors = rule.GetSelectors();
				if (selectors.size() > 0) {
					for (MSelector selector : selectors) {
						if (selector.IsIgnored()) {
							counter += rule.ParseProperties().size();
						}

					}

				}
			}
		}
		return counter;

	}

	private int getEffectiveSelectorsBasedOnProps(StringBuffer buffer) {

		int counterEffectiveSelectors = 0;
		buffer.append("\n========== EFFECTIVE CSS SELECTORS ==========\n");
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			List<MCssRule> rules = entry.getValue();

			buffer.append("\n== IN CSS: " + entry.getKey() + "\n");

			for (MCssRule rule : rules) {
				List<MSelector> selectors = rule.GetMatchedSelectors();

				if (selectors.size() > 0) {

					for (MSelector selector : selectors) {
						if (selector.hasEffectiveProperties()) {
							buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");
							buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");
							buffer.append(" Selector: " + selector.GetSelectorText() + "\n");
							counterEffectiveSelectors++;

							// buffer.append(" has effective properties: \n");
							for (MProperty prop : selector.GetProperties()) {
								buffer.append(" Property " + prop + "\n");
							}
						}
						buffer.append("\n");
					}
				}
			}
		}

		return counterEffectiveSelectors;
	}

	private int getIneffectiveSelectorsBasedOnProps(StringBuffer buffer) {

		int counterIneffectiveSelectors = 0;
		buffer.append("========== INEFFECTIVE CSS SELECTORS ==========\n");
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			List<MCssRule> rules = entry.getValue();

			buffer.append("== IN CSS: " + entry.getKey() + "\n");

			for (MCssRule rule : rules) {
				List<MSelector> selectors = rule.GetMatchedSelectors();

				if (selectors.size() > 0) {

					for (MSelector selector : selectors) {
						if (!selector.hasEffectiveProperties() && !selector.IsIgnored()) {
							buffer.append("Ineffective: ");
							buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");

							buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");
							buffer.append(" Selector: " + selector.GetSelectorText() + "\n\n");
							counterIneffectiveSelectors++;
							// ineffectivePropsSize+=selector.getSelectorText().getBytes().length;

						}

					}
				}
			}
		}

		return counterIneffectiveSelectors;
	}

	private int countUnusedProps() {

		int counter = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			List<MCssRule> rules = entry.getValue();

			for (MCssRule rule : rules) {
				List<MSelector> selectors = rule.GetSelectors();
				if (selectors.size() > 0) {
					for (MSelector selector : selectors) {
						if (!selector.IsIgnored()) {
							for (MProperty prop : selector.GetProperties()) {
								if (!prop.IsEffective()) {
									counter++;
									// ineffectivePropsSize+=prop.getsize();
								}

							}
						}

					}
				}
			}
		}

		return counter;
	}

	private int getUndefinedClasses(StringBuffer output) {
		output.append("========== UNDEFINED CSS CLASSES ==========\n");

		Set<String> undefinedClasses = new HashSet<String>();

		// for (ElementWithClass el : elementsWithNoClassDef) {
		for (String key : _elementsWithNoClassDef.keySet()) {
			output.append("State: " + key + "\n");
			Set<ElementWithClass> set = _elementsWithNoClassDef.get(key);
			for (ElementWithClass e : set) {
				for (String unmatched : e.GetUnmatchedClasses()) {
					if (undefinedClasses.add(unmatched)) {

						output.append("Undefined class: ");
						output.append("  " + unmatched + "\n");
					}
				}
			}
			output.append("\n");
		}

		return undefinedClasses.size();
	}

	private int getUnmatchedRules(StringBuffer buffer) {

		LOGGER.info("Reporting Unmatched CSS Rules...");
		buffer.append("========== UNMATCHED CSS RULES ==========\n");
		int counter = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			List<MCssRule> rules = entry.getValue();

			buffer.append("== UNMATCHED RULES IN: " + entry.getKey() + "\n");
			for (MCssRule rule : rules) {
				List<MSelector> selectors = rule.GetUnmatchedSelectors();
				counter += selectors.size();
				if (selectors.size() > 0) {
					buffer.append("Unmatched: ");
					buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");
					buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");

					for (MSelector selector : selectors) {
						// ineffectivePropsSize+=selector.getSize();
						buffer.append(selector.toString() + "\n");
					}
				}
			}
		}

		return counter;
	}

	private int getUsedRules(StringBuffer buffer) {
		LOGGER.info("Reporting Matched CSS Rules...");
		buffer.append("========== MATCHED CSS RULES ==========\n");
		int counter = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			List<MCssRule> rules = entry.getValue();

			buffer.append("== MATCHED RULES IN: " + entry.getKey() + "\n");
			for (MCssRule rule : rules) {
				List<MSelector> selectors = rule.GetMatchedSelectors();
				counter += selectors.size();
				if (selectors.size() > 0) {
					buffer.append("Matched: ");
					buffer.append("CSS rule: " + rule.GetRule().getCssText() + "\n");
					buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");

					for (MSelector selector : selectors) {
						buffer.append(selector.toString() + "\n");
					}
				}
			}

		}
		return counter;
	}

	private int OriginalCssSizeBytes() {
		int result = 0;

		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet()) {
			for (MCssRule mrule : entry.getValue())
			{
				result += mrule.GetRule().getCssText().trim().replace("{", "").replace("}", "")
				                .replace(",", "").replace(" ", "").getBytes().length;

			}
		}

		return result;
	}

	private int NewCssSizeBytes() {
		boolean effective;
		boolean exit = false;
		int result = 0;

		int counter = 0;
		for (Map.Entry<String, List<MCssRule>> entry : _cssRules.entrySet())
		{
			for (MCssRule mRule : entry.getValue())
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
									result += property.get(j).GetSize();
								}
							}
						}

					}

					if (exit)
					{
						if (counter == selector.get(i).GetProperties().size())
							result += selector.get(i).getSize();
						break;
					}
				}
			}
		}

		return result;
	}
}