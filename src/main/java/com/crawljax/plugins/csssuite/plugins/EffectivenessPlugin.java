package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.EffectivenessAnalysis;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;

import java.util.*;

public class EffectivenessPlugin implements ICssPostCrawlPlugin
{
	private int _unmatchedSelectors = 0;
	private int _ineffectiveSelectors = 0;
	private int _ineffectiveDeclarations = 0;

	@Override
	public void getStatistics(SuiteStringBuilder builder, String prefix)
	{
		builder.appendLine("%s<US>%d</US>", prefix, _unmatchedSelectors);
		builder.appendLine("%s<IS>%d</IS>", prefix, _ineffectiveSelectors);
		builder.appendLine("%s<ID>%d</ID>", prefix, _ineffectiveDeclarations);
	}


	@Override
	public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
	{
		Random random = new Random();

		LogHandler.info("[CssAnalyzer] Performing effectiveness analysis on matched CSS selectors...");

		// performance
		Set<Set<MSelector>> processedSets = new HashSet<>();

		for (String keyElement : matchedElements.GetMatchedElements())
		{
			List<MSelector> matchedSelectors = matchedElements.SortSelectorsForMatchedElem(keyElement);

			// performance
			if(processedSets.contains(new HashSet<>(matchedSelectors)))
			{
				LogHandler.debug("[CssAnalyzer] Set of matched selectors for element '%s' already processed", keyElement);
				continue;
			}

			String overridden = "overridden-" + random.nextInt();

			EffectivenessAnalysis.ComputeEffectiveness(matchedSelectors, overridden);

			// performance
			processedSets.add(new HashSet<>(matchedSelectors));
		}


		Map<String, MCssFile> result = new HashMap<>();

		for(String fileName : cssRules.keySet())
		{
			result.put(fileName, filterIneffectiveRules(cssRules.get(fileName)));
		}

		return result;
	}


	/**
	 * Filter all ineffective rules or individual selectors within those rules by their (in)effective properties
	 * @param file
	 * @return
	 */
	private MCssFile filterIneffectiveRules(MCssFile file)
	{
		List<MCssRule> newRules = new ArrayList<>();

		for(MCssRule mRule : file.GetRules())
		{
			List<MSelector> ineffectiveSelectors = new ArrayList<>();

			ineffectiveSelectors.addAll(mRule.GetUnmatchedSelectors());
			_unmatchedSelectors += ineffectiveSelectors.size();

			boolean effective = false;

			for(MSelector mSelector : mRule.GetMatchedSelectors())
			{
				if(mSelector.HasEffectiveProperties())
				{
					effective = true;
					_ineffectiveDeclarations += mSelector.GetProperties().stream().filter(p -> !p.IsIgnored() && !p.IsEffective()).count();
					mSelector.RemoveIneffectiveProperties();
				}
				else
				{
					ineffectiveSelectors.add(mSelector);
					_ineffectiveSelectors++;
				}
			}

			// only delete individual selectors if there is at least 1 effective selector in this rule
			// otherwise rule is not effective at all and will not be added to result.
			if(effective)
			{
				mRule.RemoveSelectors(ineffectiveSelectors);
				newRules.add(mRule);
			}
		}

		return new MCssFile(newRules, file);
	}
}