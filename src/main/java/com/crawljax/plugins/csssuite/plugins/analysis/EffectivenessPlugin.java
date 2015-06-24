package com.crawljax.plugins.csssuite.plugins.analysis;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;

import java.util.*;

public class EffectivenessPlugin implements ICssPostCrawlPlugin
{
	@Override
	public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
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
			result.put(fileName, FilterIneffectiveRules(cssRules.get(fileName)));
		}

		return result;
	}


	/**
	 * Filter all ineffective rules or individual selectors within those rules by their (in)effective properties
	 * @param file
	 * @return
	 */
	private static MCssFile FilterIneffectiveRules(MCssFile file)
	{
		List<MCssRule> newRules = new ArrayList<>();

		for(MCssRule mRule : file.GetRules())
		{
			boolean effective = false;

			List<MSelector> ineffectiveSelectors = new ArrayList<>();

			ineffectiveSelectors.addAll(mRule.GetUnmatchedSelectors());

			for(MSelector mSelector : mRule.GetMatchedSelectors())
			{
				if(mSelector.HasEffectiveProperties())
				{
					effective = true;
					mSelector.RemoveIneffectiveProperties();
				}
				else
				{
					ineffectiveSelectors.add(mSelector);
				}
			}

			if(effective)
			{
				mRule.RemoveSelectors(ineffectiveSelectors);
				newRules.add(mRule);
			}
		}

		return new MCssFile(newRules, file);
	}
}