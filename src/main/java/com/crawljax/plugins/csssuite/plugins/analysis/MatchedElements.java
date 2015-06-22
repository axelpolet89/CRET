package com.crawljax.plugins.csssuite.plugins.analysis;

import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityHelper;
import com.crawljax.plugins.csssuite.util.specificity.SpecificitySelector;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.xerces.impl.xpath.regex.Match;

import java.util.*;
import java.util.stream.Collectors;

public class MatchedElements
{
	private final Map<String, ListMultimap<Integer, MSelector>> _elementSelectors;

	public MatchedElements()
	{
		_elementSelectors = new HashMap<>();
	}

	public void SetMatchedElement(ElementWrapper element, MSelector selector, int order)
	{
		String key = element.GetKey();

		if(!_elementSelectors.containsKey(key))
		{
			_elementSelectors.put(key, ArrayListMultimap.create());
		}

		_elementSelectors.get(key).put(order, selector);
	}

	public Set<String> GetMatchedElements()
	{
		return _elementSelectors.keySet();
	}

	public List GetCssFileOrder(String element)
	{
		return new ArrayList(_elementSelectors.get(element).keySet());
	}

	public ListMultimap GetSelectors(String element)
	{
		return _elementSelectors.get(element);
	}

	/**
	 * Transform all selectors that match a given element into a list of SpecificitySelector instances
	 * Use that list to sort the selectors in place, and then return the MSelectors contained by the SpecificitySelectors instances in the sorted list
	 * @param cssFilesOrder we need a list of selectors first by their 'file' order and then by their specificity
	 * @param orderSelectorMap
	 * @return
	 */
	public List<MSelector> SortSelectorsForMatchedElem(List<Integer> cssFilesOrder, ListMultimap orderSelectorMap)
	{
		// we know that cssFilesOrder is ordered (by using LinkedHashMap and ListMultiMap implementations),
		// just need to reverse it (so we get highest order first)
		Collections.reverse(cssFilesOrder);

		List<SpecificitySelector> selectorsToSort = new ArrayList<>();
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
	 * Transform all selectors that match a given element into a list of SpecificitySelector instances
	 * Use that list to sort the selectors in place, and then return the MSelectors contained by the SpecificitySelectors instances in the sorted list
	 * @param element
	 * @return
	 */
	public List<MSelector> SortSelectorsForMatchedElem(String element)
	{
		return SortSelectorsForMatchedElem(GetCssFileOrder(element), GetSelectors(element));
	}
}

