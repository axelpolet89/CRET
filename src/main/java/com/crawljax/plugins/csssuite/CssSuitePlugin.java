package com.crawljax.plugins.csssuite;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import com.cathive.sass.SassContext;
import com.cathive.sass.SassFileContext;
import com.cathive.sass.SassOutputStyle;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.generator.CssWriter;
import com.crawljax.plugins.csssuite.generator.SassWriter;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.*;
import com.crawljax.plugins.csssuite.plugins.analysis.EffectivenessPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.ElementSelectorMatcher;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.plugins.merge.NormalizeAndMergePlugin;
import com.crawljax.plugins.csssuite.sass.SassBuilder;
import com.crawljax.plugins.csssuite.sass.SassFile;
import com.crawljax.plugins.csssuite.util.FileHelper;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.crawljax.plugins.csssuite.verification.CssOnDomVerifier;
import com.steadystate.css.parser.media.MediaQuery;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
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
	/* Configuration properties */
	public boolean _enableW3cValidation = false;
	public boolean _enableSassGeneration = false;
	public boolean _enableVerification = false;

	private final String _siteName;
	private final String _siteIndex;

	private final List<String> _processedCssFiles;
	private int _originalCssLOC;

	private Map<String, MCssFile> _origMcssFiles;
	private Map<String, MCssFile> _mcssFiles;
	private Map<StateVertex, LinkedHashMap<String, Integer>> _stateCssFileMap;

	private final MatchedElements _matchedElements;
	private final List<ICssPostCrawlPlugin> _postPlugins;

	private final Map<String, File> _newCssFiles;

	private final File _outputFile = new File("output/csssuite" + String.format("%s", new SimpleDateFormat("ddMMyy-hhmmss").format(new Date())) + ".txt");

	public CssSuitePlugin(String siteName, String _siteIndex)
	{
		_siteName = siteName;
		this._siteIndex = _siteIndex;

		DOMConfigurator.configure("log4j.xml");

		LogHandler.info("");
		LogHandler.info("==================================START NEW CSS-SUITE RUN=====================================");
		LogHandler.info("TARGET: %s at URL %s", _siteName, this._siteIndex);

		_originalCssLOC = 0;

		_origMcssFiles = new HashMap<>();
		_mcssFiles = new HashMap<>();
		_stateCssFileMap = new HashMap<>();

		_processedCssFiles = new ArrayList<>();

		_postPlugins = new ArrayList<>();
		_matchedElements = new MatchedElements();

		_newCssFiles = new HashMap<>();

		_postPlugins.add(new NormalizeAndSplitPlugin());
		_postPlugins.add(new DetectClonedPropertiesPlugin());
		_postPlugins.add(new EffectivenessPlugin());
		_postPlugins.add(new DetectUndoingPlugin());
		_postPlugins.add(new ChildCombinatorPlugin());
		_postPlugins.add(new NormalizeAndMergePlugin());
	}

	public void EnableDebug()
	{
		LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
	}

	public void onNewState(CrawlerContext context, StateVertex newState)
	{
		LogHandler.info("[NEW STATE] %s", newState.getUrl());

		// if the external CSS files are not parsed yet, do so
		LogHandler.info("Parse CSS rules...");
		LinkedHashMap<String, Integer> stateFileOrder = ParseCssRulesForState(context, newState);

		try
		{
			ElementSelectorMatcher.MatchElementsToDocument(newState.getName(), newState.getDocument(), _mcssFiles, stateFileOrder, _matchedElements);
			_stateCssFileMap.put(newState, stateFileOrder);
		}
		catch (Exception ex)
		{
			LogHandler.error(ex, "[MATCH SELECTORS] Error occurred while matching selectors for state %s", newState.getName());
		}
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

			//_domCssFileNameMap.put(dom, new ArrayList<>());

			int order = 0;
			for (String relPath : CSSDOMHelper.ExtractCssFileNames(dom))
			{
				String cssUrl = CSSDOMHelper.GetAbsPath(url, relPath);

				if (!_mcssFiles.containsKey(cssUrl))
				{
					LogHandler.info("[FOUND NEW CSS FILE] " + cssUrl);

					String cssCode = CSSDOMHelper.GetUrlContent(cssUrl);

					_originalCssLOC += CountLOC(cssCode);

					_origMcssFiles.put(cssUrl,  ParseCssRules(cssUrl, cssCode));
					_mcssFiles.put(cssUrl,  ParseCssRules(cssUrl, cssCode));
					//_domCssFileNameMap.get(dom).add(cssUrl);
				}

				//retain order of css files referenced in DOM
				stateFileOrder.put(cssUrl, order);
				order++;
			}

			// get all the embedded <STYLE> rules, save per HTML page
			if (!_mcssFiles.containsKey(url))
			{
				String embeddedCode = CSSDOMHelper.ParseEmbeddedStyles(dom);

				if(!embeddedCode.isEmpty())
					LogHandler.info("[FOUND NEW EMBEDDED RULES] " + url);

				_originalCssLOC += CountLOC(embeddedCode);

				_origMcssFiles.put(url, ParseCssRules(url, embeddedCode));
				_mcssFiles.put(url, ParseCssRules(url, embeddedCode));
				//_domCssFileNameMap.get(dom).add(url);
			}

			// embedded style sheet has higher order
			order++;
			stateFileOrder.put(url, order);
		}
		catch (Exception ex)
		{
			LogHandler.error(ex);
		}

		return stateFileOrder;
	}


	/**
	 *
	 * @param url
	 * @param code
	 */
	private MCssFile ParseCssRules(String url, String code)
	{
		CssParser parser = new CssParser();

		MCssFile file = parser.ParseCssIntoMCssRules(url, code);

		List<String> parseErrors = parser.GetParseErrors();
		for(String parseError : parseErrors)
		{
			LogHandler.warn("[CssParser] Parse errors occurred while parsing '%s'\n%s", url, parseError);
		}

		LogHandler.info("[CssParser] Parsed '%s' -> CSS rules parsed into McssRules: %d", url, file.GetRules().size());

		return file;
	}


	private Map<String, MCssFile> ExecutePostTransformations()
	{
		LogHandler.info("[CSS SUITE PLUGIN] Execute POST crawl-time transformations...");

		Map<String, MCssFile> rules = _mcssFiles;
		for(ICssPostCrawlPlugin plugin : _postPlugins)
		{
			rules = plugin.Transform(_mcssFiles, _matchedElements);
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
	public void postCrawling(CrawlSession session, ExitStatus exitReason)
	{
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
		_mcssFiles = rules;

		final int[] countOrig = {0};
		final int[] countNew = {0};

		final int[] countOrigProps = {0};
		final int[] countNewProps = {0};

		for(String fileName : _origMcssFiles.keySet())
		{
			_origMcssFiles.get(fileName).GetRules().stream().forEach(r -> countOrig[0] += r.GetSelectors().size());
			_mcssFiles.get(fileName).GetRules().stream().forEach(r -> countNew[0] += r.GetSelectors().size());

			_origMcssFiles.get(fileName).GetRules().stream().forEach(r -> r.GetSelectors().forEach(s -> countOrigProps[0] += s.GetProperties().size()));
			_mcssFiles.get(fileName).GetRules().stream().forEach(r -> r.GetSelectors().forEach(s -> countNewProps[0] += s.GetProperties().size()));
		}


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


		try
		{
			FileUtils.writeStringToFile(_outputFile, output.toString());
		}
		catch (IOException e)
		{
			LogHandler.error(e.getMessage(), e);
		}

		GenerateCssAndSass(rules, _enableSassGeneration);

		if(_enableVerification)
		{
			VerifyGeneratedCss();
		}
	}



	private void GenerateCssAndSass(Map<String, MCssFile> source, boolean generateSass)
	{
		String root = String.format("output\\%s\\", _siteName);
		String cssOutputRoot = String.format("%s\\CSS(def)\\", root);
		String sassOutputRoot = String.format("%s\\SASS\\", root);

		Map<String, File> cssFiles = new HashMap<>();
		Map<String, File> sassFiles = new HashMap<>();

		URI indexUri = null;
		try
		{
			indexUri = new URI(_siteIndex);
		}
		catch (URISyntaxException e)
		{
			return;
		}

		String siteRoot = _siteIndex.replace(indexUri.getPath(), "");

		for(String fileName : source.keySet())
		{
			String cssFile = fileName.replace(siteRoot, "");

			if(cssFile.isEmpty() || cssFile.equals("") || cssFile.equals("/"))
				cssFile += "index/";

			String cssRootDir = cssOutputRoot;
			String sassRootDir = sassOutputRoot;

			if(!cssFile.contains(".css"))
			{
				cssRootDir += "embedded_styles\\";
				sassRootDir += "embedded_styles\\";

				if(cssFile.charAt(cssFile.length() - 1) == '/')
					cssFile = cssFile.substring(0, cssFile.length() - 1).concat(".css");
				else
					cssFile = cssFile.concat(".css");

				LogHandler.info("[CssWriter] Styles not contained in external CSS file, write as embedded styles");
			}

			//replace querystring token, not valid as file name
			if(cssFile.contains("?"))
			{
				int idx = cssFile.indexOf("?");
				cssFile = cssFile.replace(cssFile.substring(idx, cssFile.length()), "");
			}

			try
			{
				cssFiles.put(fileName, FileHelper.CreateFileAndDirs(cssRootDir.concat(cssFile)));
			}
			catch (Exception e)
			{
				LogHandler.error(e, "[GenerateCssAndSass] Error in building File object for CSS file '%s' at root '%s'", cssFile, cssRootDir);
			}

			if(!generateSass)
			{
				continue;
			}

			String sassFile = cssFile.replace(".css", ".scss");

			try
			{
				sassFiles.put(fileName, FileHelper.CreateFileAndDirs(sassRootDir.concat(sassFile)));
			}
			catch (Exception e)
			{
				LogHandler.error(e, "[GenerateCssAndSass] Error in building File object for SASS file '%s' at root '%s'", sassFile, sassRootDir);
			}

			String genCssFile = cssRootDir.concat(cssFile).replace("CSS(def)", "CSS(sass)");

			try
			{
				_newCssFiles.put(fileName, FileHelper.CreateFileAndDirs(genCssFile));
			}
			catch (Exception e)
			{
				LogHandler.error(e, "[GenerateCssAndSass] Error in building File object for SASS generated CSS file at path '%s'", genCssFile);
			}
		}

		// CSS before SASS transformation
		CssWriter writer = new CssWriter();
		for(String fileName : source.keySet())
		{
			try
			{
				writer.Generate(cssFiles.get(fileName), source.get(fileName));
			}
			catch (Exception e)
			{
				LogHandler.error(e, "[Generate CSS] Error while generating CSS code for file %s", fileName);
			}
		}

		if(generateSass)
		{
			// create SCSS objects from CSS files, including clone detection and other SCSS-specific transformations
			SassBuilder sassBuilder = new SassBuilder();
			Map<String, SassFile> sassFileObjects = sassBuilder.CssToSass(source);

			// store generated SCSS files
			Map<String, File> scssFiles = new HashMap<>();

			// generate SCSS code from SCSS objects
			SassWriter sassWriter = new SassWriter();
			for (String fileName : sassFileObjects.keySet())
			{
				try
				{
					scssFiles.put(fileName, sassWriter.GenerateSassCode(sassFiles.get(fileName), sassFileObjects.get(fileName)));
				}
				catch (Exception e)
				{
					LogHandler.error(e, "[Generate SCSS] Error while generating SCSS code for file %s", fileName);
				}
			}


			// generate CSS from SCSS files
			for (String fileName : scssFiles.keySet())
			{
				try
				{
					SassContext ctx = SassFileContext.create(scssFiles.get(fileName).toPath());
					ctx.getOptions().setOutputStyle(SassOutputStyle.NESTED);

					FileOutputStream outputStream = new FileOutputStream(_newCssFiles.get(fileName));
					ctx.compile(outputStream);

					outputStream.flush();
					outputStream.close();
				}
				catch (Exception e)
				{
					LogHandler.error(e, "[Compile CSS from SCSS] Error while compiling SCSS to CSS via java-sass for source '%s' to target '%s'", scssFiles.get(fileName), _newCssFiles.get(fileName));
				}
			}
		}
	}



	private void VerifyGeneratedCss()
	{
		LogHandler.info("[VERIFICATION] Parse the new generated CSS files...");

		CssParser parser = new CssParser();
		Map<String, MCssFile> generatedCssFiles = new HashMap<>();

		for(String fileName : _newCssFiles.keySet())
		{
			try
			{
				String cssCode = new String(Files.readAllBytes(_newCssFiles.get(fileName).toPath()), "UTF-8");
				generatedCssFiles.put(fileName, parser.ParseCssIntoMCssRules(fileName, cssCode));
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[VERIFICATION] Cannot read lines or parse css code from new file %s", _newCssFiles.get(fileName).toPath());
			}
		}

		LogHandler.info("[VERIFICATION] Start verification for all found DOM states with original and new CSS files");
		CssOnDomVerifier verifier = new CssOnDomVerifier();


		try
		{
			verifier.Verify(_stateCssFileMap, _origMcssFiles, generatedCssFiles);

			SuiteStringBuilder builder = new SuiteStringBuilder();
			builder.append("<site>");
			builder.appendLine("\t<site_name>%s</site_name>\n", _siteName);
			verifier.GenerateXml(builder, "\t");
			builder.appendLine("</site>");

			File verificationOutput = FileHelper.CreateFileAndDirs("./output/verification/verification_summary.xml");
			FileWriter writer = new FileWriter(verificationOutput);
			writer.append(builder.toString());
			writer.flush();
			writer.close();
		}
		catch (Exception ex)
		{
			LogHandler.error(ex, "[VERIFICATION] Error occurred in verification process");
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
						buffer.append("CSS rule: " + rule.GetStyleRule().getCssText() + "\n");
						buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");
						buffer.append("   Selector: " + selector.GetSelectorText() + "\n");

						for(MediaQuery query : selector.GetMediaQueries())
						{
							buffer.append("   Media-query: " + query + "\n");
						}

						counterEffectiveSelectors++;

						for (MProperty prop : selector.GetProperties())
						{
							buffer.append("   Property " + prop.Print() + "\n");
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
						buffer.append("CSS rule: " + rule.GetStyleRule().getCssText() + "\n");

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
					buffer.append("CSS rule: " + rule.GetStyleRule().getCssText() + "\n");
					buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");

					for (MSelector selector : selectors)
					{
						buffer.append(selector.Print() + "\n");
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
					buffer.append("CSS rule: " + rule.GetStyleRule().getCssText() + "\n");
					buffer.append("at line: " + rule.GetLocator().getLineNumber() + "\n");

					for (MSelector selector : selectors)
					{
						buffer.append(selector.Print() + "\n");
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
				for(MSelector selector : rule.GetSelectors())
				{
					counter += selector.GetProperties().size();
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
						counter += selector.GetProperties().size();
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
				result += mRule.GetStyleRule().getCssText().trim().replace("{", "").replace("}", "")
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
		return 0;

//		boolean effective;
//		boolean exit = false;
//		int result = 0;
//
//		int counter = 0;
//		for (Map.Entry<String, MCssFile> entry : _mcssFiles.entrySet())
//		{
//			for (MCssRule mRule : entry.getValue().GetRules())
//			{
//				List<MSelector> selector = mRule.GetSelectors();
//				for (int i = 0; i < selector.size(); i++)
//				{
//					if (!selector.get(i).IsIgnored())
//					{
//						exit = true;
//
//						List<MProperty> property = selector.get(i).GetProperties();
//						for (int j = 0; j < property.size(); j++)
//						{
//							if (!property.get(j).IsEffective())
//							{
//								effective = false;
//								for (int k = i + 1; k < selector.size(); k++)
//								{
//									if (!selector.get(k).IsIgnored()) {
//										if (selector.get(k).GetProperties().get(j).IsEffective())
//										{
//											effective = true;
//											break;
//										}
//									}
//								}
//								if (!effective)
//								{
//									counter++;
//									result += property.get(j).ComputeSizeBytes();
//								}
//							}
//						}
//
//					}
//
//					if (exit)
//					{
//						if (counter == selector.get(i).GetProperties().size())
//							result += selector.get(i).ComputeSizeBytes();
//						break;
//					}
//				}
//			}
//		}
//
//		return result;
	}
}