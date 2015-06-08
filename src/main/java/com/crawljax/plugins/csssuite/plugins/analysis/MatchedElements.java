package com.crawljax.plugins.csssuite.plugins.analysis;

import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityHelper;
import com.crawljax.plugins.csssuite.util.specificity.SpecificitySelector;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.*;
import java.util.stream.Collectors;

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

	/**
	 * USE ONLY IN TESTS!
	 */
	public static void Clear(){elementSelectors.clear();}

	/**
	 * Transform all selectors that match a given element into a list of SpecificitySelector instances
	 * Use that list to sort the selectors in place, and then return the MSelectors contained by the SpecificitySelectors instances in the sorted list
	 * @param element
	 * @return
	 */
	public static List<MSelector> SortSelectorsForMatchedElem(String element)
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
}

