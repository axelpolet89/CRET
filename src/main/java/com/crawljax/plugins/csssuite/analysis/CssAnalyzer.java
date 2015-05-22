package com.crawljax.plugins.csssuite.analysis;

import java.util.*;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.interfaces.ICssCrawlPlugin;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityHelper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
	private static void CompareProperties(MProperty property, MSelector otherSelector, String overridden)
	{
		for (MProperty nextProperty : otherSelector.GetProperties())
		{
			if (property.GetName().equalsIgnoreCase(nextProperty.GetName()))
			{
				// it is possible, due to specificity ordering, that 'this' property was already deemed effective,
				// but a less specific ('next') selector contained an !important declaration
				if(nextProperty.IsImportant())
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
	 * Compare properties of a (less specific) selector with a given property on their name AND value
	 * set the other (less specific) properties overridden or set 'this' property overridden due to !important
	 * @param property
	 * @param otherSelector
	 * @param overridden
	 */
	private static void ComparePropertiesWithValue(MProperty property, MSelector otherSelector, String overridden)
	{
		for (MProperty nextProperty : otherSelector.GetProperties())
		{
			if (property.GetName().equalsIgnoreCase(nextProperty.GetName())
					&& property.GetValue().equalsIgnoreCase(nextProperty.GetValue()))
			{
				// it is possible, due to specificity ordering, that 'this' property was already deemed effective,
				// but a less specific ('next') selector contained an !important declaration
				if(nextProperty.IsImportant())
				{
					property.SetStatus(overridden);
					property.SetEffective(false);
				}
				else {
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

			if(effective) {
				mRule.RemoveSelectors(ineffectiveSelectors);
				newRules.add(mRule);
			}
		}

		file.SetRules(newRules);
		return file;
	}

	@Override
	public void Transform(String stateName, Document dom, Map<String, MCssFile> cssRules)
	{
		for(String file : cssRules.keySet())
		{
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
						} else
						{
							ElementWrapper ew = new ElementWrapper(stateName, (Element) node);
							mSelector.AddMatchedElement(ew);
							MatchedElements.SetMatchedElement(ew, mSelector);
						}
					}

				}

			}
		}
	}

	@Override
	public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
	{
		Random random = new Random();

		for (String keyElement : MatchedElements.elementSelectors.keySet()) {

			//get selectors that matched the keyElement
			List<MSelector> selectors = MatchedElements.elementSelectors.get(keyElement);

			//order the selectors by their specificity and location
			SpecificityHelper.OrderSpecificity(selectors);

			String overridden = "overridden-" + random.nextInt();

			for (int i = 0; i < selectors.size(); i++) {
				MSelector selector = selectors.get(i);
				for (MProperty property : selector.GetProperties()) {
					if (!property.GetStatus().equals(overridden))
					{
						property.SetEffective(true);

						// not set all the similar properties in other selectors to overridden

						for (int j = i + 1; j < selectors.size(); j++) {
							MSelector nextSelector = selectors.get(j);

							// when 'this' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect other selectors, so we can break
							if(selector.HasPseudoElement())
								break;

							// if 'the other' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect 'this' selector
							if(nextSelector.HasPseudoElement())
								continue;

							if(selector.IsNonStructuralPseudo() || nextSelector.IsNonStructuralPseudo())
							{
								if(selector.CompareKeyPseudoClass(nextSelector))
								{
									CompareProperties(property, nextSelector, overridden);
								}
								else
								{
									ComparePropertiesWithValue(property, nextSelector, overridden);
								}
							}
							else
							{
								CompareProperties(property, nextSelector, overridden);
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

		return result;
	}
}