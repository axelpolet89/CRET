package com.crawljax.plugins.cilla.analysis;

import com.crawljax.plugins.cilla.data.ElementWrapper;
import com.crawljax.plugins.cilla.data.MSelector;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class MatchedElements
{
	public static final ListMultimap<String, MSelector> elementSelectors = ArrayListMultimap.create();

	public static void SetMatchedElement(ElementWrapper element, MSelector selector)
	{
		// Use state name and the XPath of the element as key for the element
		String key = element.GetStateName() + XPathHelper.getXPathExpression(element.GetElement());
		elementSelectors.put(key, selector);
	}
}
