package com.crawljax.plugins.csssuite.plugins;

import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.util.XPathHelper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.*;

public class MatchedElements
{
	private static final Map<String, ListMultimap<Integer, MSelector>> elementSelectors = new HashMap<>();

	public static void SetMatchedElement(ElementWrapper element, MSelector selector, int order)
	{
		String key = element.GetKey();

		if(!elementSelectors.containsKey(key))
		{
			elementSelectors.put(key, ArrayListMultimap.create());
		}

		elementSelectors.get(key).put(order, selector);
	}

	public static Set<String> GetMatchedElements()
	{
		return elementSelectors.keySet();
	}

	public static List GetCssFileOrder(String element)
	{
		return new ArrayList(elementSelectors.get(element).keySet());
	}

	public static ListMultimap GetSelectors(String element)
	{
		return elementSelectors.get(element);
	}
}
