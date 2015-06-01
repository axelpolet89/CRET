package com.crawljax.plugins.csssuite.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.interfaces.ICssCrawlPlugin;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityHelper;
import com.crawljax.plugins.csssuite.util.specificity.SpecificitySelector;
import com.google.common.collect.ListMultimap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;
import se.fishtank.css.selectors.parser.ParserException;

public class CssAnalyzer implements ICssCrawlPlugin, ICssPostCrawlPlugin
{
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
	 * Transform all selectors that match a given element into a list of SpecificitySelector instances
	 * Use that list to sort the selectors in place, and then return the MSelectors contained by the SpecificitySelectors instances in the sorted list
	 * @param element
	 * @return
	 */
	private static List<MSelector> SortSelectorsForMatchedElem(String element)
	{
		// we need a list of selectors first by their 'file' order and then by their specificity
		List<Integer> cssFilesOrder = MatchedElements.GetCssFileOrder(element);

		// we know that cssFilesOrder is ordered (by using LinkedHashMap and ListMultiMap implementations),
		// just need to reverse it (so we get highest order first), potential sort performance improvement
		Collections.reverse(cssFilesOrder);

		List<SpecificitySelector> selectorsToSort = new ArrayList<>();

		ListMultimap orderSelectorMap = MatchedElements.GetSelectors(element);
		for(int order : cssFilesOrder)
		{
			List<MSelector> selectorsForFile = orderSelectorMap.get(order);

			//wrap MSelector in SpecificitySelector
			selectorsToSort.addAll(selectorsForFile.stream().map(selector -> new SpecificitySelector(selector, order)).collect(Collectors.toList()));
		}

		SpecificityHelper.SortBySpecificity(selectorsToSort);

		// extract the MSelectors from the list of sorted SpecificitySelectors and return
		return selectorsToSort.stream().map((ss) -> ss.GetSelector()).collect(Collectors.toList());
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

		file.OverwriteAllRules(newRules);
		return file;
	}


	/**
	 *
	 * @return
	 */
	private static Map<String, String> CreateDefaultStyles()
	{
		Map<String, String> defaultStyles = new HashMap<>();

		defaultStyles.put("width", "0");
		defaultStyles.put("min-width", "0");
		defaultStyles.put("max-width", "none");
		defaultStyles.put("height", "0");
		defaultStyles.put("min-height", "0");
		defaultStyles.put("max-height", "none");

		SetSeparateStyles("padding-%s", "0", defaultStyles);
		SetSeparateStyles("margin-%s", "0", defaultStyles);

		SetSeparateStyles("border-%s-width", "0", defaultStyles);
		SetSeparateStyles("border-%s-style", "none", defaultStyles);
		defaultStyles.put("border-top-left-radius", "0");
		defaultStyles.put("border-top-right-radius", "0");
		defaultStyles.put("border-bottom-right-radius", "0");
		defaultStyles.put("border-bottom-left-radius", "0");

		defaultStyles.put("outline-width", "0");
		defaultStyles.put("outline-style", "none");

		defaultStyles.put("background-image", "none");
		defaultStyles.put("background-color", "transparent");
		defaultStyles.put("background-repeat", "repeat");
		defaultStyles.put("background-position", "0% 0%");
		defaultStyles.put("background-attachment", "scroll");
		defaultStyles.put("background-size", "auto");
		defaultStyles.put("background-clip", "border-box");
		defaultStyles.put("background-origin", "padding-box");

		return defaultStyles;
	}


	/**
	 *
	 * @param formatter
	 * @param value
	 * @param styles
	 */
	private static void SetSeparateStyles(String formatter, String value, Map<String, String> styles)
	{
		styles.put(String.format(formatter, "top"), value);
		styles.put(String.format(formatter, "right"), value);
		styles.put(String.format(formatter, "bottom"), value);
		styles.put(String.format(formatter, "left"), value);
	}


	/**
	 *
	 * @param rules
	 */
	private static void FindUndoingStyles(Map<String, MCssFile> rules)
	{
		Map<String, String> defaultStyles =CreateDefaultStyles();
		Set<Set<MSelector>> processedSets = new HashSet<>();

		for (String keyElement : MatchedElements.GetMatchedElements())
		{
			List<MSelector> matchedSelectors = SortSelectorsForMatchedElem(keyElement);
			List<MSelector> effectiveSelectors = matchedSelectors.stream().filter(s -> s.HasEffectiveProperties()).collect(Collectors.toList());

			if(processedSets.contains(new HashSet<>(effectiveSelectors)))
			{
				LogHandler.info("Set of effective selectors for element '%s' already processed", keyElement);
				continue;
			}

			for (int i = 0; i < effectiveSelectors.size(); i++)
			{
				MSelector selector = effectiveSelectors.get(i);
				for (MProperty property : selector.GetProperties())
				{
					final String name = property.GetName();
					final String value = property.GetValue();

					// skip declarations that we do not check for default values
					if(!defaultStyles.containsKey(name))
					{
						continue;
					}

					final String defaultValue = defaultStyles.get(name);

					// verify this property is effective and has a default value
					if (property.IsEffective() && value.equals(defaultValue))
					{
						LogHandler.info("[CssAnalyzer] Found possible undoing property: '%s' with a (default) value '%s' in selector '%s'",
								name, value, selector);

						boolean validUndo = false;

						for (int j = i + 1; j < effectiveSelectors.size(); j++)
						{
							MSelector nextSelector = effectiveSelectors.get(j);

							// media-query selector is more specific, but regular also applies
							if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() == 0)
							{
								continue;
							}

							// media-query selector is less specific, but it could trigger other properties
							if (selector.GetMediaQueries().size() == 0 && nextSelector.GetMediaQueries().size() > 0)
							{
								continue;
							}

							// both selectors have different media-queries
							if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() > 0
									&& !selector.HasEqualMediaQueries(nextSelector))
							{
								continue;
							}

							// when 'this' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect other selectors, so we can break
							if (selector.HasPseudoElement())
							{
								break;
							}

							// if 'the other' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect 'this' selector
							if (nextSelector.HasPseudoElement())
							{
								continue;
							}

							if (selector.IsNonStructuralPseudo() || nextSelector.IsNonStructuralPseudo())
							{
								if (!selector.CompareKeyPseudoClass(nextSelector))
								{
									continue;
								}
							}

							for (MProperty otherProperty : nextSelector.GetProperties())
							{
								final String otherName = otherProperty.GetName();
								final String otherValue = otherProperty.GetValue();

								// verify whether another effective property in a less-specific selector has the same name
								if (otherProperty.IsEffective() && otherName.equals(name))
								{

									// verify that it has a different value
									if (!otherValue.equals(defaultStyles.get(otherName)))
									{
										validUndo = true;
										LogHandler.info("[TRUE] Found an effective property '%s' with a different value '%s' in (less-specific) selector '%s'\n" +
														"that is (correctly) undone by effective property with value '%s' in (more-specific) selector '%s'",
												otherProperty.GetName(), otherProperty.GetValue(), nextSelector, property.GetValue(), selector);
										break;
									}
								}
							}

							if(validUndo)
								break;
						}

						if(!validUndo)
							property.SetInvalidUndo();
					}
				}
			}

			processedSets.add(new HashSet<>(effectiveSelectors));
		}

		for(MCssFile file : rules.values())
		{
			for(MCssRule rule : file.GetRules())
			{
				rule.GetSelectors().stream().filter(mSelector -> mSelector.HasEffectiveProperties()).forEach(mSelector -> {
					mSelector.GetProperties().stream().filter(mProp -> mProp.IsEffective() && mProp.IsInvalidUndo()).forEach(mProp -> {
						LogHandler.info("Property %s with value %s in selector %s is an INVALID undo style", mProp.GetName(), mProp.GetValue(), mSelector);
					});
				});
			}
		}
	}


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
						//compare selectors containing non-structural pseudo classes on their compatibility with the node they matched
						if (mSelector.IsNonStructuralPseudo())
						{
							if (!mSelector.CheckPseudoCompatibility(node.getNodeName(), node.getAttributes()))
								continue;
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

		for (String keyElement : MatchedElements.GetMatchedElements())
		{
			List<MSelector> matchedSelectors = SortSelectorsForMatchedElem(keyElement);

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

							// media-query selector is less specific, but it could trigger other properties
							if (selector.GetMediaQueries().size() == 0 && nextSelector.GetMediaQueries().size() > 0)
							{
								compareOnValuesToo = true;
							}

							// both selectors have different media-queries
							if (selector.GetMediaQueries().size() > 0 && nextSelector.GetMediaQueries().size() > 0
									&& !selector.HasEqualMediaQueries(nextSelector))
							{
								continue;
							}

							// when 'this' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect other selectors, so we can break
							if (selector.HasPseudoElement())
							{
								break;
							}

							// if 'the other' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect 'this' selector
							if (nextSelector.HasPseudoElement())
							{
								continue;
							}

							if (selector.IsNonStructuralPseudo() || nextSelector.IsNonStructuralPseudo())
							{
								if (!selector.CompareKeyPseudoClass(nextSelector))
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
		}


		Map<String, MCssFile> result = new HashMap<>();

		for(String file : cssRules.keySet())
		{
			result.put(file, FilterIneffectiveRules(cssRules.get(file)));
		}

		FindUndoingStyles(cssRules);

		return result;
	}
}