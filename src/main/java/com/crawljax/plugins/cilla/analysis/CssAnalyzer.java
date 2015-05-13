package com.crawljax.plugins.cilla.analysis;

import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;

public class CssAnalyzer {

	private static final Logger LOGGER = Logger.getLogger(CssAnalyzer.class.getName());

	public static List<MCssRule> checkCssSelectorRulesOnDom(String stateName, Document dom,
	        List<MCssRule> mRules) {

		for (MCssRule mRule : mRules)
		{
			List<MSelector> mSelectors = mRule.getSelectors();
			for (MSelector mSelector : mSelectors)
			{
				if(mSelector.isIgnored())
					continue;

				String cssSelector = mSelector.getSelectorText();

				Selectors seSelectors = new Selectors(new W3CNode(dom));
				List<Node> result = seSelectors.querySelectorAll(cssSelector);

				for (Node node : result) {

//					if(mSelector.isNonStructuralPseudo()) {
//						if(!mSelector.TryTestPseudo(node.getNodeName(), node.getAttributes()))
//							continue;
//					}

					if (node instanceof Document) {
						LOGGER.debug("CSS rule returns the whole document!!!");
						mSelector.setMatched(true);
					} else {
						ElementWrapper ew = new ElementWrapper(stateName, (Element) node);
						mSelector.addMatchedElement(ew);
						MatchedElements.setMatchedElement(ew, mSelector);
					}
				}

			}

		}

		return mRules;
	}
}