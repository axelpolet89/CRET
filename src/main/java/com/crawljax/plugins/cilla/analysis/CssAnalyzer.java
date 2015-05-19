package com.crawljax.plugins.cilla.analysis;

import java.util.*;

import com.crawljax.plugins.cilla.data.*;
import com.crawljax.plugins.cilla.interfaces.ICssCrawlPlugin;
import com.crawljax.plugins.cilla.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.cilla.util.specificity.SpecificityHelper;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;
import se.fishtank.css.selectors.parser.ParserException;

public class CssAnalyzer implements ICssCrawlPlugin, ICssPostCrawlPlugin
{

	private static final Logger LOGGER = Logger.getLogger(CssAnalyzer.class.getName());

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

	@Override
	public void Transform(String stateName, Document dom, Map<String, List<MCssRule>> cssRules)
	{
		for(String file : cssRules.keySet())
		{
			for (MCssRule mRule : cssRules.get(file))
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
						LOGGER.debug("Could not query DOM tree with selector" + cssSelector);
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
							LOGGER.debug("CSS rule returns the whole document!!!");
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
	public Map<String, List<MCssRule>> Transform(Map<String, List<MCssRule>> cssRules)
	{
		Random random = new Random();

		for (String keyElement : MatchedElements.elementSelectors.keySet()) {

			LOGGER.debug("keyElement: " + keyElement);

			//get selectors that matched the keyElement
			List<MSelector> selectors = MatchedElements.elementSelectors.get(keyElement);

			//order the selectors by their specificity and location
			SpecificityHelper.OrderSpecificity(selectors);

			String overridden = "overridden-" + random.nextInt();

			LOGGER.debug("RANDOM: " + overridden);

			for (int i = 0; i < selectors.size(); i++) {
				MSelector selector = selectors.get(i);
				for (MProperty property : selector.GetProperties()) {
					if (!property.GetStatus().equals(overridden)) {
						property.SetEffective(true);
						LOGGER.debug("SET effective: " + property);

						// not set all the similar properties in other selectors to overridden

						for (int j = i + 1; j < selectors.size(); j++) {
							MSelector nextSelector = selectors.get(j);

							// when 'this' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect other selectors, so we can break
							if(selector.IsPseudoElement())
								break;

							// if 'the other' selector includes a pseudo-element (as selector-key),
							// it is always effective and does not affect 'this' selector
							if(nextSelector.IsPseudoElement())
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
					} else {
						LOGGER.debug("OVRRIDDEN: " + property);
					}
				}
			}
		}

		Map<String, List<MCssRule>> result = new HashMap<>();

		//filter all unmatched -and ineffective rules -or individual selectors
		for(String file : cssRules.keySet())
		{
			List<MCssRule> newRules = new ArrayList<>();

			for(MCssRule mRule : cssRules.get(file))
			{
				boolean effective = false;

				List<MSelector> ineffectiveSelectors = new ArrayList<>();

				ineffectiveSelectors.addAll(mRule.GetUnmatchedSelectors());

				for(MSelector mSelector : mRule.GetMatchedSelectors())
				{
					if(mSelector.hasEffectiveProperties())
					{
						effective = true;
						mSelector.RemoveIneffectiveSelectors();
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

			result.put(file, newRules);
		}

		return result;
	}
}