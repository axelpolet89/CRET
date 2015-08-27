package com.crawljax.plugins.cret.transformation.effectiveness;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.interfaces.ICssTransformer;
import com.crawljax.plugins.cret.transformation.matcher.MatchedElements;
import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.*;

public class EffectivenessPlugin implements ICssTransformer
{
	private int _unmatchedSelectors = 0;
	private int _ineffectiveSelectors = 0;
	private int _ineffectiveDeclarations = 0;

	@Override
	public void getStatistics(CretStringBuilder builder, String prefix)
	{
		builder.appendLine("%s<US>%d</US>", prefix, _unmatchedSelectors);
		builder.appendLine("%s<IS>%d</IS>", prefix, _ineffectiveSelectors);
		builder.appendLine("%s<ID>%d</ID>", prefix, _ineffectiveDeclarations);
	}


	@Override
	public Map<String, MCssFile> transform(Map<String, MCssFile> cssRules, MatchedElements matchedElements)
	{
		Random random = new Random();

		LogHandler.info("[Effectiveness] Performing effectiveness analysis on matched CSS selectors...");

		// performance
		Set<Set<MSelector>> processedSets = new HashSet<>();

		for (String keyElement : matchedElements.getMatchedElements())
		{
			List<MSelector> matchedSelectors = matchedElements.sortSelectorsForMatchedElem(keyElement);

			// performance
			if(processedSets.contains(new HashSet<>(matchedSelectors)))
			{
				LogHandler.debug("[Effectiveness] Set of matched selectors for element '%s' already processed", keyElement);
				continue;
			}

			String overridden = "overridden-" + random.nextInt();

			EffectivenessAnalysis.computeEffectiveness(matchedSelectors, overridden);

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
	 * Filter all ineffective rules or individual selectors within those rules by their (in)effective declarations
	 */
	private MCssFile filterIneffectiveRules(MCssFile file)
	{
		List<MCssRule> newRules = new ArrayList<>();

		for(MCssRule mRule : file.getRules())
		{
			List<MSelector> ineffectiveSelectors = new ArrayList<>();

			ineffectiveSelectors.addAll(mRule.getUnmatchedSelectors());
			_unmatchedSelectors += ineffectiveSelectors.size();

			boolean effective = false;

			for(MSelector mSelector : mRule.getMatchedSelectors())
			{
				if(mSelector.hasEffectiveDeclarations())
				{
					effective = true;
					_ineffectiveDeclarations += mSelector.getDeclarations().stream().filter(p -> !p.isIgnored() && !p.isEffective()).count();
					mSelector.removeIneffectiveDeclarations();
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
				mRule.removeSelectors(ineffectiveSelectors);
				newRules.add(mRule);
			}
		}

		return new MCssFile(newRules, file);
	}
}