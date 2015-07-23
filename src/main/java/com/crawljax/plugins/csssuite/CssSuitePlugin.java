package com.crawljax.plugins.csssuite;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

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
	public boolean _enableStatistics = false;
	public int _clonePropsUpperLimit = 999;

	/* fields */
	private final String _siteName;
	private final String _siteIndex;
	private int _originalCssLOC;

	// files that apply per discovered DOM state
	private final Map<StateVertex, LinkedHashMap<String, Integer>> _stateCssFiles;

	// originally discovered CSS files
	private final Map<String, MCssFile> _origMcssFiles;

	// optimized CSS files
	private Map<String, MCssFile> _newMcssFiles;

	// matched elements analysis, crawltime
	private final MatchedElements _matchedElements;

	// additional transformations, post-crawltime
	private final List<ICssPostCrawlPlugin> _postPlugins;

	// generated CSS/SASS files
	private final Map<String, File> _targetCssFiles;
	private final Map<String, File> _targetSassFiles;
	private final Map<String, File> _targetCssFromSassFiles;

	private final File _outputFile = new File("output/csssuite" + String.format("%s", new SimpleDateFormat("ddMMyy-hhmmss").format(new Date())) + ".txt");

	public CssSuitePlugin(String siteName, String siteIndex)
	{
		_siteName = siteName;
		_siteIndex = siteIndex;

		//DOMConfigurator.configure("log4j.xml");

		LogHandler.info("");
		LogHandler.info("==================================START NEW CSS-SUITE RUN=====================================");
		LogHandler.info("TARGET: %s at URL %s", _siteName, this._siteIndex);

		_originalCssLOC = 0;

		_stateCssFiles = new HashMap<>();

		_origMcssFiles = new HashMap<>();
		_newMcssFiles = new HashMap<>();

		_matchedElements = new MatchedElements();

		_postPlugins = new ArrayList<>();
		_postPlugins.add(new NormalizeAndSplitPlugin());
		_postPlugins.add(new DetectClonedPropertiesPlugin());
		_postPlugins.add(new EffectivenessPlugin());
		_postPlugins.add(new DetectUndoingPlugin());
		_postPlugins.add(new ChildCombinatorPlugin());
		_postPlugins.add(new NormalizeAndMergePlugin());

		_targetCssFiles = new HashMap<>();
		_targetSassFiles = new HashMap<>();
		_targetCssFromSassFiles = new HashMap<>();
	}

	/**
	 *
	 */
	public void enableDebug()
	{
		LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
	}


	/**
	 *
	 * @param context
	 * @param newState
	 */
	@Override
	public void onNewState(CrawlerContext context, StateVertex newState)
	{
		LogHandler.info("[NEW STATE] %s", newState.getUrl());

		// if the external CSS files are not parsed yet, do so
		LogHandler.info("Parse CSS rules...");
		LinkedHashMap<String, Integer> stateFileOrder = parseCssRulesForState(context, newState);

		try
		{
			ElementSelectorMatcher.MatchElementsToDocument(newState.getName(), newState.getDocument(), _newMcssFiles, stateFileOrder, _matchedElements);
			_stateCssFiles.put(newState, stateFileOrder);
		}
		catch (Exception ex)
		{
			LogHandler.error(ex, "[MATCH SELECTORS] Error occurred while matching selectors for state %s", newState.getName());
		}
	}


	/**
	 * Process discovered CSS files, after crawling complete
	 * @param session
	 * @param exitReason
	 */
	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitReason)
	{
		int totalCssRules = 0;
		int totalCssSelectors = 0;
		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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

		Map<String, MCssFile> rules = executePostTransformations();
		_newMcssFiles = rules;

		final int[] countOrig = {0};
		final int[] countNew = {0};

		final int[] countOrigProps = {0};
		final int[] countNewProps = {0};

		for(String fileName : _origMcssFiles.keySet())
		{
			_origMcssFiles.get(fileName).GetRules().stream().forEach(r -> countOrig[0] += r.GetSelectors().size());
			_newMcssFiles.get(fileName).GetRules().stream().forEach(r -> countNew[0] += r.GetSelectors().size());

			_origMcssFiles.get(fileName).GetRules().stream().forEach(r -> r.GetSelectors().forEach(s -> countOrigProps[0] += s.GetProperties().size()));
			_newMcssFiles.get(fileName).GetRules().stream().forEach(r -> r.GetSelectors().forEach(s -> countNewProps[0] += s.GetProperties().size()));
		}


		int effectiveInt = PrintEffectiveSelectors(effective);
		int ineffectiveInt = PrintIneffectiveSelectors(ineffectiveBuffer);

		output.append("Analyzed " + session.getConfig().getUrl() + " on "
				+ new SimpleDateFormat("dd/MM/yy-hh:mm:ss").format(new Date()) + "\n");

		output.append("-> Files with CSS code: " + _newMcssFiles.keySet().size() + "\n");
		for (String address : _newMcssFiles.keySet())
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

		if(generateTargetFiles(rules))
		{
			boolean codeErrors = generateCssAndSass(rules, _enableSassGeneration);

			if(!codeErrors)
			{
				if (_enableVerification)
				{
					verifyGeneratedCss();
				}

				if(_enableStatistics)
				{
					generateStatistics();
				}
			}
		}
	}


	/**
	 * @param context
	 * @param state
	 * @return a mapping relating a discovered CSS file to the order in which it is used in a browser
	 */
	private LinkedHashMap<String, Integer> parseCssRulesForState(CrawlerContext context, StateVertex state)
	{
		final String url = context.getBrowser().getCurrentUrl();
		final LinkedHashMap<String, Integer> stateFileOrder = new LinkedHashMap<>();

		try
		{
			final Document dom = state.getDocument();

			int order = 0;
			for (String relPath : CSSDOMHelper.ExtractCssFileNames(dom))
			{
				String cssUrl = relPath;

				if(relPath.startsWith("//"))
				{
					URI uri = new URI(url);
					cssUrl = String.format("%s:%s", uri.getScheme(), cssUrl);
				}
				else
				{
					cssUrl = CSSDOMHelper.GetAbsPath(url, relPath);
				}

				if (!_newMcssFiles.containsKey(cssUrl))
				{
					LogHandler.info("[FOUND NEW CSS FILE] " + cssUrl);

					String cssCode = CSSDOMHelper.GetUrlContent(cssUrl);

					_originalCssLOC += countLOC(cssCode);

					_origMcssFiles.put(cssUrl,  parseCssRules(cssUrl, cssCode));
					_newMcssFiles.put(cssUrl,  parseCssRules(cssUrl, cssCode));
				}

				//retain order of css files referenced in DOM
				stateFileOrder.put(cssUrl, order);
				order++;
			}

			// get all the embedded <STYLE> rules, save per HTML page
			if (!_newMcssFiles.containsKey(url))
			{
				String embeddedCode = CSSDOMHelper.ParseEmbeddedStyles(dom);

				if(!embeddedCode.isEmpty())
				{
					LogHandler.info("[FOUND NEW EMBEDDED RULES] " + url);
				}

				_originalCssLOC += countLOC(embeddedCode);

				_origMcssFiles.put(url, parseCssRules(url, embeddedCode));
				_newMcssFiles.put(url, parseCssRules(url, embeddedCode));
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
	private MCssFile parseCssRules(String url, String code)
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


	/**
	 * Execute all transformations meant to be performed after discovering all CSS files
	 * @return the set of transformed McssFiles
	 */
	private Map<String, MCssFile> executePostTransformations()
	{
		LogHandler.info("[CSS SUITE PLUGIN] Execute POST crawl-time transformations...");

		Map<String, MCssFile> rules = _newMcssFiles;
		for(ICssPostCrawlPlugin plugin : _postPlugins)
		{
			rules = plugin.transform(_newMcssFiles, _matchedElements);
		}
		return rules;
	}


	/**
	 * Count lines of code in a given CSS string
	 * @param cssText
	 * @return number of lines of code
	 */
	private int countLOC(String cssText)
	{
		int count = 0;

		cssText = cssText.replaceAll("\\{", "{\n");
		cssText = cssText.replaceAll("\\}", "}\n");
		cssText = cssText.replaceAll("\\}", "}\n");
		cssText = cssText.replaceAll("\\;", ";\n");

		if (cssText != null && !cssText.equals(""))
		{
			LineNumberReader ln = new LineNumberReader(new StringReader(cssText));
			try
			{
				while (ln.readLine() != null)
				{
					count++;
				}
			} catch (IOException e) {
				LogHandler.error(e.getMessage(), e);
			}
		}

		return count;
	}


	private boolean generateTargetFiles(Map<String, MCssFile> source)
	{
		LogHandler.info("[GenerateCssAndSass] START CODE GENERATION...");

		String root = String.format("output\\%s\\", _siteName);
		String cssOutputRoot = String.format("%s\\CSS(def)\\", root);
		String sassOutputRoot = String.format("%s\\SASS\\", root);

		Map<String, String> embeddedMapping = new LinkedHashMap<>();
		int embeddedIdx = 1;

		Map<String, String> externalMapping = new LinkedHashMap<>();
		int externalIdx = 1;

		boolean filesInError = false;

		for(String fileName : source.keySet())
		{
			LogHandler.info("[GenerateCssAndSass] Generate output file objects for filename '%s'", fileName);

			boolean inError = false;

			String cssFile = "";
			String cssRootDir = cssOutputRoot;

			if(fileName.contains(".css"))
			{
				cssRootDir += "external_styles\\";
				cssFile = String.format("external_%d.css", externalIdx);

				externalMapping.put(fileName, cssFile);

				LogHandler.info("[GenerateCssAndSass] Styles contained in external CSS file, write as external css file '%s'", cssFile);
				externalIdx ++;
			}
			else
			{
				cssRootDir += "embedded_styles\\";
				cssFile = String.format("embedded_%d.css", embeddedIdx);

				embeddedMapping.put(fileName, cssFile);

				LogHandler.info("[GenerateCssAndSass] Styles not contained in external CSS file, write to embedded style file '%s'", cssFile);
				embeddedIdx ++;
			}

			try
			{
				_targetCssFiles.put(fileName, FileHelper.CreateFileAndDirs(cssRootDir.concat(cssFile)));
			}
			catch (Exception e)
			{
				filesInError = true;
				inError = true;
				LogHandler.error(e, "[GenerateCssAndSass] Error in building File object for CSS file '%s' at root '%s'", cssFile, cssRootDir);
			}

			if(inError)
			{
				continue;
			}

			String sassRootDir = cssRootDir.replace(cssOutputRoot, sassOutputRoot);
			String sassFile = cssFile.replace(".css", ".scss");

			try
			{
				_targetSassFiles.put(fileName, FileHelper.CreateFileAndDirs(sassRootDir.concat(sassFile)));
			}
			catch (Exception e)
			{
				filesInError = true;
				inError = true;
				LogHandler.error(e, "[GenerateCssAndSass] Error in building File object for SASS file '%s' at root '%s'", sassFile, sassRootDir);
			}

			if(inError)
			{
				continue;
			}

			String cssFromSassFile = cssRootDir.concat(cssFile).replace("CSS(def)", "CSS(sass)");
			try
			{
				_targetCssFromSassFiles.put(fileName, FileHelper.CreateFileAndDirs(cssFromSassFile));
			}
			catch (Exception e)
			{
				filesInError = true;
				LogHandler.error(e, "[GenerateCssAndSass] Error in building File object for SASS-to-CSS file at path '%s'", cssFromSassFile);
			}
		}


		if(filesInError)
		{
			LogHandler.info("[GenerateCssAndSass] Errors occurred while building File objects, do not continue code generation");
			return false;
		}

		if(!externalMapping.isEmpty())
		{
			try
			{
				FileWriter esWriter = new FileWriter(FileHelper.CreateFileAndDirs(root.concat("external_files_mapping.txt")));
				esWriter.write("external files are mapped as follows:\n");
				for (String fileName : externalMapping.keySet())
				{
					esWriter.append(String.format("%s : %s\n", externalMapping.get(fileName), fileName));
				}
				esWriter.flush();
				esWriter.close();
			}
			catch (IOException e)
			{
				LogHandler.error(e, "[GenerateCssAndSass] Error in generating external file mapping");
			}
		}

		if(!embeddedMapping.isEmpty())
		{
			try
			{
				FileWriter esWriter = new FileWriter(FileHelper.CreateFileAndDirs(root.concat("embedded_styles_mapping.txt")));
				esWriter.write("embedded files are mapped as follows:\n");
				for (String fileName : embeddedMapping.keySet())
				{
					esWriter.append(String.format("%s : %s\n", embeddedMapping.get(fileName), fileName));
				}
				esWriter.flush();
				esWriter.close();
			}
			catch (IOException e)
			{
				LogHandler.error(e, "[GenerateCssAndSass] Error in generating embedded file mapping");
			}
		}

		return true;
	}


	/**
	 * Main method that generates CSS from a given set of MCssFiles
	 * Optionally, it will also generate SASS code including
	 * @param mcssFiles
	 * @param generateSass
	 * @return
	 */
	private boolean generateCssAndSass(Map<String, MCssFile> mcssFiles, boolean generateSass)
	{
		boolean cssInError = false;
		boolean sassInError = false;
		boolean sassToCssInError = false;

		// CSS before SASS transformation
		CssWriter writer = new CssWriter();
		for(String fileName : mcssFiles.keySet())
		{
			try
			{
				writer.Generate(_targetCssFiles.get(fileName), mcssFiles.get(fileName));
			}
			catch (Exception e)
			{
				cssInError = true;
				LogHandler.error(e, "[Generate CSS Code] Error while generating CSS code for file %s", fileName);
			}
		}

		if(generateSass)
		{
			Map<String, SassFile> sassFileObjects = new HashMap<>();
			// create SCSS objects from CSS files, including clone detection and other SCSS-specific transformations
			for(String fileName : mcssFiles.keySet())
			{
				LogHandler.info("[Generate SCSS Code] Start building SASS objects for file %s...", fileName);

				SassBuilder sassBuilder = new SassBuilder(mcssFiles.get(fileName), _clonePropsUpperLimit);
				sassFileObjects.put(fileName, sassBuilder.CssToSass());
			}

			// store generated SCSS files
			Map<String, File> scssFiles = new HashMap<>();

			// generate SCSS code from SCSS objects
			SassWriter sassWriter = new SassWriter();
			for (String fileName : sassFileObjects.keySet())
			{
				try
				{
					LogHandler.info("[Generate SCSS Code] Generating SCSS code for file %s...", fileName);
					scssFiles.put(fileName, sassWriter.GenerateSassCode(_targetSassFiles.get(fileName), sassFileObjects.get(fileName)));
				}
				catch (Exception e)
				{
					sassInError = true;
					LogHandler.error(e, "[Generate SCSS Code] Error while generating SCSS code for file %s", fileName);
				}
			}

			if(!sassInError)
			{
				// generate CSS from SCSS files
				for (String fileName : scssFiles.keySet())
				{
					LogHandler.info("[Compile SCSS from CSS] Start compiling SCSS code for file %s...", fileName);
					try
					{
						SassContext ctx = SassFileContext.create(scssFiles.get(fileName).toPath());
						ctx.getOptions().setOutputStyle(SassOutputStyle.NESTED);

						FileOutputStream outputStream = new FileOutputStream(_targetCssFromSassFiles.get(fileName));
						ctx.compile(outputStream);

						outputStream.flush();
						outputStream.close();
					}
					catch (Exception e)
					{
						sassToCssInError = true;
						LogHandler.error(e, "[Compile CSS from SCSS] Error while compiling SCSS to CSS via java-sass for source '%s' to target '%s'", scssFiles.get(fileName), _targetCssFiles.get(fileName));
					}
				}
			}
		}

		return cssInError || sassInError || sassToCssInError;
	}


	/**
	 *
	 */
	private void verifyGeneratedCss()
	{
		LogHandler.info("[VERIFICATION] Start verification for all found DOM states with original and new CSS files");
		CssOnDomVerifier verifier = new CssOnDomVerifier();

		try
		{
			verifier.Verify(_stateCssFiles, _origMcssFiles, parseGeneratedCss());

			SuiteStringBuilder builder = new SuiteStringBuilder();
			builder.append("<site>");
			builder.appendLine("\t<site_name>%s</site_name>\n", _siteName);
			verifier.GenerateXml(builder, "\t");
			builder.appendLine("</site>");

			File verificationOutput = FileHelper.CreateFileAndDirs("./output/verification/verification_summary.xml");
			FileWriter writer = new FileWriter(verificationOutput, true);
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
	private Map<String, MCssFile> parseGeneratedCss()
	{
		LogHandler.info("[GENERATED CSS] Parse generated CSS files...");

		CssParser parser = new CssParser();
		Map<String, MCssFile> generatedCssFiles = new HashMap<>();

		for(String fileName : _targetCssFiles.keySet())
		{
			try
			{
				String cssCode = new String(Files.readAllBytes(_targetCssFiles.get(fileName).toPath()), "UTF-8");
				generatedCssFiles.put(fileName, parser.ParseCssIntoMCssRules(fileName, cssCode));
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[GENERATED CSS] Cannot read lines or parse css code from new file %s", _targetCssFiles.get(fileName).toPath());
			}
		}

		return  generatedCssFiles;
	}


	private void generateStatistics()
	{
		LogHandler.info("[STATISTICS] Start verification for all found DOM states with original and new CSS files");

		try
		{
			SuiteStringBuilder builder = new SuiteStringBuilder();
			builder.append("<site>");
			builder.appendLine("\t<site_name>%s</site_name>\n", _siteName);

			generateFileStatistics(builder);

			for(ICssPostCrawlPlugin plugin : _postPlugins)
			{
				plugin.getStatistics(builder, "\t");
			}

			builder.appendLine("</site>");

			File verificationOutput = FileHelper.CreateFileAndDirs("./output/statistics/statistics_summary.xml");
			FileWriter writer = new FileWriter(verificationOutput, true);
			writer.append(builder.toString());
			writer.flush();
			writer.close();
		}
		catch (Exception ex)
		{
			LogHandler.error(ex, "[STATISTICS] Error occurred in verification process");
		}
	}


	/**
	 *
	 * @param builder
	 */
	private void generateFileStatistics(SuiteStringBuilder builder)
	{
		String prefix = "\t";

		int OrS = 0;
		int OrD = 0;

		int OpS = 0;
		int OpD = 0;

		for(MCssFile mCssFile : _origMcssFiles.values())
		{
			OrS += getStatistics(mCssFile, this::countRuleSelectors);
			OrD += getStatistics(mCssFile, this::countRuleDeclarations);
		}

		for(MCssFile mCssFile : _newMcssFiles.values())
		{
			OpS += getStatistics(mCssFile, this::countRuleSelectors);
			OpD += getStatistics(mCssFile, this::countRuleDeclarations);
		}

		builder.appendLine("%s<OrS>%d</OrS>", prefix, OrS);
		builder.appendLine("%s<OrD>%d</OrD>", prefix, OrD);
		builder.appendLine("%s<OpS>%d</OpS>", prefix, OpS);
		builder.appendLine("%s<OpD>%d</OpD>", prefix, OpD);
	}


	private int countRuleSelectors(MCssRule mCssRule)
	{
		return mCssRule.GetSelectors().size();
	}

	private int countRuleDeclarations(MCssRule mCssRule)
	{
		int count = 0;
		for(MSelector mSelector : mCssRule.GetSelectors())
		{
			count += mSelector.GetProperties().size();
		}
		return count;
	}

	private int getStatistics(MCssFile mCssFile, Function<MCssRule, Integer> statisticsFunction)
	{
		int count = 0;

		for(MCssRule mCssRule : mCssFile.GetRules())
		{
			count += statisticsFunction.apply(mCssRule);
		}

		return count;
	}


//	private int recursiveCountMediaSelectors(MCssMediaRule mediaRule, Function<MCssRule, Integer> statisticsFunction)
//	{
//		int count = 0;
//
//		for(MCssRuleBase ruleBase : mediaRule.GetInnerRules())
//		{
//			if(ruleBase.IsCompatibleWithRule())
//			{
//				count += statisticsFunction.apply((MCssRule)ruleBase);
//			}
//
//			if(ruleBase.IsCompatibleWithMediaRule())
//			{
//				count += recursiveCountMediaSelectors((MCssMediaRule)ruleBase, statisticsFunction);
//			}
//		}
//
//		return count;
//	}


	/**
	 *
	 * @param buffer
	 * @return
	 */
	private int PrintEffectiveSelectors(StringBuffer buffer) {

		int counterEffectiveSelectors = 0;
		buffer.append("\n========== EFFECTIVE CSS SELECTORS ==========\n");

		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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

		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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

		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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
		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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
		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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

		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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
		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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

		for (Map.Entry<String, MCssFile> entry : _newMcssFiles.entrySet())
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