package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;

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

			for (int i = 0; i < matchedSelectors.size(); i++)
			{
				MSelector selector = matchedSelectors.get(i);
				for (MProperty property : selector.GetProperties())
				{
					// find out if property was already deemed effective in another matched element
					boolean alreadyEffective = property.IsEffective();

					if (!property.GetStatus().equals(overridden))
					{
						property.SetEffective(true);

						for (int j = i + 1; j < matchedSelectors.size(); j++)
						{
							boolean compareOnValuesToo = false;

							MSelector nextSelector = matchedSelectors.get(j);

							// media-query selector is more specific, but regular also applies
							if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() == 0)
							{
								continue;
							}

//							// media-query selector is less specific, but it could trigger other properties, just do a property compare
//							if (selector.GetMediaQueries().size() == 0 && nextSelector.GetMediaQueries().size() > 0)
//							{
//								compareOnValuesToo = true;
//							}

							// both selectors have different media-queries
							if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() > 0
									&& !selector.HasEqualMediaQueries(nextSelector))
							{
								continue;
							}

							// when 'this' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect other selectors, so we can break
							if (selector.HasPseudoElement() || nextSelector.HasPseudoElement())
							{
								if (!selector.HasEqualPseudoElement(nextSelector))
								{
									continue;
								}
							}

							if (selector.IsNonStructuralPseudo() || nextSelector.IsNonStructuralPseudo())
							{
								if (!selector.HasEqualPseudoClass(nextSelector))
								{
									compareOnValuesToo = true;
								}
							}

							if (compareOnValuesToo)
							{
								ComparePropertiesWithValue(property, nextSelector, overridden, alreadyEffective);
							}
							else
							{
								// by default: if both selectors apply under the same condition, simply check matching property names
								// otherwise, the only way for next selector to be ineffective is too have same property name AND value
								CompareProperties(property, nextSelector, overridden, alreadyEffective);
							}
						}
					}
				}
			}

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
	 * Compare properties of a (less specific) selector with a given property on ONLY their name
	 * set the other (less specific) properties overridden or set 'this' property overridden due to !important
	 * @param property
	 * @param otherSelector
	 * @param overridden
	 */
	private static Void CompareProperties(MProperty property, MSelector otherSelector, String overridden, boolean alreadyEffective)
	{
		for (MProperty nextProperty : otherSelector.GetProperties())
		{
			if (property.GetName().equalsIgnoreCase(nextProperty.GetName()))
			{
				// it is possible, due to specificity ordering, that 'this' property was already deemed effective,
				// but a less specific ('next') selector contained an !important declaration
				// this property should not be !important or not previously deemed effective
				if(!alreadyEffective && nextProperty.IsImportant() && !property.IsImportant())
				{
					property.SetStatus(overridden);
					property.SetEffective(false);
				}
				else
				{
					nextProperty.SetStatus(overridden);
				}
			}
		}
		return null;
	}


	/**
	 * Compare properties of a (less specific) selector with a given property on their name AND value
	 * set the other (less specific) properties overridden or set 'this' property overridden due to !important
	 * @param property
	 * @param otherSelector
	 * @param overridden
	 */
	private static void ComparePropertiesWithValue(MProperty property, MSelector otherSelector, String overridden, boolean alreadyEffective)
	{
		for (MProperty nextProperty : otherSelector.GetProperties())
		{
			if (property.GetName().equalsIgnoreCase(nextProperty.GetName())
					&& property.GetValue().equalsIgnoreCase(nextProperty.GetValue()))
			{
				// it is possible, due to specificity ordering, that 'this' property was already deemed effective,
				// but a less specific ('next') selector contained an !important declaration
				// this property should not be !important or not previously deemed effective
				if(!alreadyEffective && nextProperty.IsImportant() && !property.IsImportant())
				{
					property.SetStatus(overridden);
					property.SetEffective(false);
				}
				else
				{
					nextProperty.SetStatus(overridden);
				}
			}
		}
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