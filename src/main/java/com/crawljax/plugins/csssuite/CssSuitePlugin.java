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
import com.crawljax.plugins.csssuite.generator.CssWriter;
import com.crawljax.plugins.csssuite.generator.SassWriter;
import com.crawljax.plugins.csssuite.interfaces.ICssTransformer;
import com.crawljax.plugins.csssuite.plugins.*;
import com.crawljax.plugins.csssuite.plugins.EffectivenessPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.ElementSelectorMatcher;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.plugins.merge.NormalizeAndMergePlugin;
import com.crawljax.plugins.csssuite.sass.SassBuilder;
import com.crawljax.plugins.csssuite.sass.SassStatistics;
import com.crawljax.plugins.csssuite.util.FileHelper;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.crawljax.plugins.csssuite.verification.CssOnDomVerifier;

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

	// files that apply per discovered DOM state
	private final Map<StateVertex, LinkedHashMap<String, Integer>> _stateCssFiles;

	// originally discovered CSS files
	private final Map<String, MCssFile> _origMcssFiles;

	// optimized CSS files
	private Map<String, MCssFile> _newMcssFiles;

	// matched elements analysis, crawltime
	private final MatchedElements _matchedElements;

	// additional transformations, post-crawltime
	private final List<ICssTransformer> _postPlugins;

	// generated CSS/SASS files
	private final Map<String, File> _targetCssFiles;
	private final Map<String, File> _targetSassFiles;
	private final Map<String, File> _targetCssFromSassFiles;

	private final File _outputFile = new File("output/csssuite" + String.format("%s", new SimpleDateFormat("ddMMyy-hhmmss").format(new Date())) + ".txt");

	// statistics
	private int _originalCssLOC;
	private int _domstates;
	private final List<SassStatistics> _sassStatistics;

	public CssSuitePlugin(String siteName, String siteIndex)
	{
		_siteName = siteName;
		_siteIndex = siteIndex;

		//DOMConfigurator.configure("log4j.xml");

		LogHandler.info("");
		LogHandler.info("==================================START NEW CSS-SUITE RUN=====================================");
		LogHandler.info("TARGET: %s at URL %s", _siteName, this._siteIndex);

		_originalCssLOC = 0;
		_domstates = 0;

		_stateCssFiles = new HashMap<>();

		_origMcssFiles = new HashMap<>();
		_newMcssFiles = new HashMap<>();

		_matchedElements = new MatchedElements();

		_postPlugins = new ArrayList<>();
		_postPlugins.add(new NormalizeAndSplitPlugin());
		_postPlugins.add(new DetectClonedDeclarationsPlugin());
		_postPlugins.add(new EffectivenessPlugin());
		_postPlugins.add(new DetectUndoingPlugin());
		_postPlugins.add(new ChildCombinatorPlugin());
		_postPlugins.add(new NormalizeAndMergePlugin());

		_targetCssFiles = new HashMap<>();
		_targetSassFiles = new HashMap<>();
		_targetCssFromSassFiles = new HashMap<>();
		_sassStatistics = new ArrayList<>();
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
		_domstates++;

		// if the external CSS files are not parsed yet, do so
		LogHandler.info("[NEW STATE] Parse CSS rules...");
		LinkedHashMap<String, Integer> stateFileOrder = parseCssRulesForState(context, newState);

		try
		{
			ElementSelectorMatcher.MatchElementsToDocument(newState.getName(), newState.getDocument(), _newMcssFiles, stateFileOrder, _matchedElements);
			_stateCssFiles.put(newState, stateFileOrder);
		}
		catch (Exception ex)
		{
			LogHandler.error(ex, "[NEW STATE] [MATCH SELECTORS] Error occurred while matching selectors for state %s", newState.getName());
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
		Map<String, MCssFile> rules = executePostTransformations();
		_newMcssFiles = rules;

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

					_origMcssFiles.put(cssUrl, parseCssRules(cssUrl, cssCode));
					_newMcssFiles.put(cssUrl, parseCssRules(cssUrl, cssCode));
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
		for(ICssTransformer plugin : _postPlugins)
		{
			rules = plugin.transform(rules, _matchedElements);
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
				LogHandler.error(e, "[Generate CSS Code] Error while generating CSS code for file %s", _targetCssFiles.get(fileName));
			}
		}

		if(generateSass)
		{
			// store generated SCSS files
			Map<String, File> scssFiles = new HashMap<>();

			// generate SCSS code from SCSS objects
			SassWriter sassWriter = new SassWriter();
			for (String fileName : mcssFiles.keySet())
			{
				try
				{
					LogHandler.info("[Generate SCSS Code] Generating SCSS code for file %s...", _targetSassFiles.get(fileName));

					SassBuilder sassBuilder = new SassBuilder(mcssFiles.get(fileName), _clonePropsUpperLimit);
					scssFiles.put(fileName, sassWriter.GenerateSassCode(_targetSassFiles.get(fileName), sassBuilder.generateSass()));

					//gather statistics for this file
					_sassStatistics.add(sassBuilder.getStatistics());
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

			generateFileStatistics(builder, "\t");

			for(ICssTransformer plugin : _postPlugins)
			{
				plugin.getStatistics(builder, "\t");
			}

			generateSassStatistics(builder, "\t");

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
	private void generateFileStatistics(SuiteStringBuilder builder, String prefix)
	{
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

		builder.appendLine("%s<LOC>%d</LOC>", prefix, _originalCssLOC);
		builder.appendLine("%s<DOM_states>%d</DOM_states>", prefix, _domstates);

		builder.appendLine("%s<OrS>%d</OrS>", prefix, OrS);
		builder.appendLine("%s<OpS>%d</OpS>", prefix, OpS);

		builder.appendLine("%s<OrD>%d</OrD>", prefix, OrD);
		builder.appendLine("%s<OpD>%d</OpD>", prefix, OpD);
	}


	private int countRuleSelectors(MCssRule mCssRule)
	{
		return (int)mCssRule.GetSelectors().stream().filter(s -> !s.IsIgnored()).count();
	}

	private int countRuleDeclarations(MCssRule mCssRule)
	{
		int count = 0;
		for(MSelector mSelector : mCssRule.GetSelectors())
		{
			if(!mSelector.IsIgnored())
			{
				count += mSelector.GetDeclarations().stream().filter(p -> !p.IsIgnored()).count();
			}
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

	private void generateSassStatistics(SuiteStringBuilder builder, String prefix)
	{
		int V = 0;
		int VT = 0;
		int C = 0;
		int CT = 0;
		int M = 0;
		int MT = 0;

		for(SassStatistics statistics : _sassStatistics)
		{
			V += statistics.variableCount;
			VT += statistics.declarationsTouchedByVars;
			C += statistics.cloneSetCount;
			CT += statistics.declarationsTouchedByClones;
			M += statistics.mergeMixinCount;
			MT += statistics.declarationsTouchedByMerges;
		}

		builder.appendLine("%s<V>%d</V>", prefix, V);
		builder.appendLine("%s<VT>%d</VT>", prefix, VT);
		builder.appendLine("%s<C>%d</C>", prefix, C);
		builder.appendLine("%s<CT>%d</CT>", prefix, CT);
		builder.appendLine("%s<M>%d</M>", prefix, M);
		builder.appendLine("%s<MT>%d</MT>", prefix, MT);
	}
}