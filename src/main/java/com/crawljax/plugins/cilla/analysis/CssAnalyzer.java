package com.crawljax.plugins.cilla.analysis;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;

public class CssAnalyzer {

	private static final Logger LOGGER = Logger.getLogger(CssAnalyzer.class.getName());

	public static void checkCssSelectorRulesOnDom(String stateName, Document dom, List<MCssRule> mRules) {

		for (MCssRule mRule : mRules)
		{
			List<MSelector> mSelectors = mRule.GetSelectors();
			for (MSelector mSelector : mSelectors)
			{
				if(mSelector.IsIgnored())
					continue;

				String cssSelector = mSelector.getSelectorText();

				Selectors seSelectors = new Selectors(new W3CNode(dom));
				List<Node> result = seSelectors.querySelectorAll(cssSelector);

				for (Node node : result) {

					//compare selectors containing non-structural pseudo classes on their compatibility with the node they matched
					if(mSelector.IsNonStructuralPseudo())
					{
						if(!mSelector.CheckPseudoCompatibility(node.getNodeName(), node.getAttributes()))
							continue;
					}

					if (node instanceof Document) {
						LOGGER.debug("CSS rule returns the whole document!!!");
						mSelector.setMatched(true);
					} else {
						ElementWrapper ew = new ElementWrapper(stateName, (Element) node);
						mSelector.addMatchedElement(ew);
						MatchedElements.SetMatchedElement(ew, mSelector);
					}
				}

			}

		}
	}

	public static void analyzeCssSelectorEffectiveness()
	{
		Random random = new Random();

		for (String keyElement : MatchedElements.elementSelectors.keySet()) {

			LOGGER.debug("keyElement: " + keyElement);

			//get selectors that matched the keyElement
			List<MSelector> selectors = MatchedElements.elementSelectors.get(keyElement);

			//order the selectors by their specificity and location
			OrderSpecifictity(selectors);

			String overridden = "overridden-" + random.nextInt();

			LOGGER.debug("RANDOM: " + overridden);

			for (int i = 0; i < selectors.size(); i++) {
				MSelector selector = selectors.get(i);
				for (MProperty property : selector.getProperties()) {
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
	}

	private static void CompareProperties(MProperty property, MSelector otherSelector, String overridden)
	{
		for (MProperty nextProperty : otherSelector.getProperties())
		{
			if (property.GetName().equalsIgnoreCase(nextProperty.GetName()))
			{
				nextProperty.SetStatus(overridden);
			}
		}
	}

	private static void ComparePropertiesWithValue(MProperty property, MSelector otherSelector, String overridden)
	{
		for (MProperty nextProperty : otherSelector.getProperties())
		{
			if (property.GetName().equalsIgnoreCase(nextProperty.GetName())
					&& property.GetValue().equalsIgnoreCase(nextProperty.GetValue()))
			{
				nextProperty.SetStatus(overridden);
			}
		}
	}

	private static void OrderSpecifictity(List<MSelector> selectors) {
		Collections.sort(selectors, new Comparator<MSelector>() {

			public int compare(MSelector o1, MSelector o2) {
				int value1 = o1.getSpecificity().getValue();
				int value2 = o2.getSpecificity().getValue();

				//if two selectors have the same _specificity,
				//then the one that is defined later (e.g. a higher row number in the css file)
				//has a higher order
				if (value1 == value2) {
					return new Integer(o1.GetRuleNumber()).compareTo(o2.GetRuleNumber());
				}

				return new Integer(value1).compareTo(new Integer(value2));
			}

		});

		//we need selectors sorted ascending (from specific to less-specific)
		Collections.reverse(selectors);
	}
}