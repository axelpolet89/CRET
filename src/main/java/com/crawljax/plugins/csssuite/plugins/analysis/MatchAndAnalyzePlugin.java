package com.crawljax.plugins.csssuite.plugins.analysis;

import java.util.*;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.interfaces.ICssCrawlPlugin;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;
import se.fishtank.css.selectors.parser.ParserException;

public class MatchAndAnalyzePlugin implements ICssCrawlPlugin, ICssPostCrawlPlugin
{
	@Override
	public void Transform(String stateName, Document dom, Map<String, MCssFile> cssRules, LinkedHashMap<String, Integer> stateFileOrder)
	{
		for(String file : stateFileOrder.keySet())
		{
			LogHandler.info("[CssAnalyzer] Matching DOM elements for css file '%s'...", file);
			int matchCount = 0;

			int order = stateFileOrder.get(file);

			for (MCssRule mRule : cssRules.get(file).GetRules())
			{
				List<MSelector> mSelectors = mRule.GetSelectors();
				for (MSelector mSelector : mSelectors)
				{
					if (mSelector.IsIgnored())
						continue;

					String cssSelector = mSelector.GetFilteredSelectorText();

					Selectors seSelectors = new Selectors(new W3CNode(dom));

					List<Node> result;
					try
					{
						result = seSelectors.querySelectorAll(cssSelector);
					}
					catch (ParserException ex)
					{
						LogHandler.warn("Could not query DOM tree with selector '%s' from rule '%s' from file '%s'", cssSelector, mRule, file);
						continue;
					}
					catch (Exception ex)
					{
						LogHandler.error("Could not query DOM tree with selector '%s' from rule '%s' from file '%s'" + cssSelector, mRule, file);
						continue;
					}

					for (Node node : result)
					{
						//compare any selector containing non-structural pseudo classes on their compatibility with the node they matched
						if (mSelector.IsNonStructuralPseudo())
						{
							if (!mSelector.CheckPseudoCompatibility(node.getNodeName(), node.getAttributes()))
							{
								continue;
							}
						}

						if (node instanceof Document)
						{
							LogHandler.warn("CSS rule returns the whole document, rule '%s", mRule);
							mSelector.SetMatched(true);
							matchCount++;
						}
						else
						{
							ElementWrapper ew = new ElementWrapper(stateName, (Element) node);
							mSelector.AddMatchedElement(ew);
							MatchedElements.SetMatchedElement(ew, mSelector, order);
							matchCount++;
						}
					}
				}
			}

			LogHandler.info("[CssAnalyzer] Matched '%d' elements in DOM to CSS selectors", matchCount);
		}
	}


	@Override
	public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
	{
		Random random = new Random();

		LogHandler.info("[CssAnalyzer] Performing effectiveness analysis on matched CSS selectors...");

		// performance
		Set<Set<MSelector>> processedSets = new HashSet<>();

		for (String keyElement : MatchedElements.GetMatchedElements())
		{
			List<MSelector> matchedSelectors = MatchedElements.SortSelectorsForMatchedElem(keyElement);

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

		return new MCssFile(file.GetName(), newRules, file.GetIgnoredRules());
	}
}