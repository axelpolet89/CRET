package com.crawljax.plugins.csssuite.plugins.analysis;

import java.util.*;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;
import se.fishtank.css.selectors.parser.ParserException;

public class ElementSelectorMatcher
{
	public static void MatchElementsToDocument(String stateName, Document dom, Map<String, MCssFile> cssRules, LinkedHashMap<String, Integer> stateFileOrder, MatchedElements matchedElements)
	{
			for (String fileName : stateFileOrder.keySet())
			{
				LogHandler.info("[CssAnalyzer] Matching DOM elements for css file '%s'...", fileName);
				int matchCount = 0;

				int order = stateFileOrder.get(fileName);

				for (MCssRule mRule : cssRules.get(fileName).getRules())
				{
					List<MSelector> mSelectors = mRule.getSelectors();
					for (MSelector mSelector : mSelectors)
					{
						if (mSelector.isIgnored())
						{
							continue;
						}

						String cssSelector = mSelector.getFilteredSelectorText();

						Selectors seSelectors = new Selectors(new W3CNode(dom));

						List<Node> result;
						try
						{
							result = seSelectors.querySelectorAll(cssSelector);
						}
						catch (ParserException ex)
						{
							LogHandler.warn("Could not query DOM tree with selector '%s' from rule '%s' from file '%s'", cssSelector, mRule, fileName);
							continue;
						}
						catch (Exception ex)
						{
							LogHandler.error("Could not query DOM tree with selector '%s' from rule '%s' from file '%s'" + cssSelector, mRule, fileName);
							continue;
						}

						for (Node node : result)
						{
							//compare any selector containing non-structural pseudo classes on their compatibility with the node they matched
							if (mSelector.isNonStructuralPseudo())
							{
								if (!mSelector.checkPseudoCompatibility(node.getNodeName(), node.getAttributes()))
								{
									continue;
								}
							}

							if (node instanceof Document)
							{
								LogHandler.warn("CSS rule returns the whole document, rule '%s", mRule);
								mSelector.setMatched(true);
								matchCount++;
							}
							else
							{
								ElementWrapper ew = new ElementWrapper(stateName, (Element) node);
								mSelector.addMatchedElement(ew);
								matchedElements.SetMatchedElement(ew, mSelector, order);
								matchCount++;
							}
						}
					}
				}

				LogHandler.info("[CssAnalyzer] Matched '%d' elements in DOM to CSS selectors", matchCount);
		}
	}
}