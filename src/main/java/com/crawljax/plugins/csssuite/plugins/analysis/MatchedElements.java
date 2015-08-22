package com.crawljax.plugins.csssuite.plugins.analysis;

import com.crawljax.plugins.csssuite.data.ElementWrapper;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.util.specificity.SpecificitySelector;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.*;
import java.util.stream.Collectors;

public class MatchedElements
{
	private final Map<String, ListMultimap<Integer, MSelector>> _elementSelectors;

	public MatchedElements()
	{
		_elementSelectors = new HashMap<>();
	}

	public void setMatchedElement(ElementWrapper element, MSelector selector, int order)
	{
		String key = element.getKey();

		if(!_elementSelectors.containsKey(key))
		{
			_elementSelectors.put(key, ArrayListMultimap.create());
		}

		_elementSelectors.get(key).put(order, selector);
	}

	/**
	 * @return all matched elements stored in this instance, by their string representations
	 */
	public Set<String> getMatchedElements()
	{
		return _elementSelectors.keySet();
	}


	/**
	 * Transform all selectors that match a given element into a list of SpecificitySelector instances
	 * Use that list to sort the selectors in place, and then return the MSelectors contained by the SpecificitySelectors instances in the sorted list
	 * @param cssFilesOrder we need a list of selectors first by their 'file' order and then by their specificity
	 * @param orderSelectorMap
	 * @return all selectors that were matched to the given element, sorted by their specificity and cascading rules
	 */
	public List<MSelector> sortSelectorsForMatchedElem(List<Integer> cssFilesOrder, ListMultimap orderSelectorMap)
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

		sortBySpecificity(selectorsToSort);

		// extract the MSelectors from the list of sorted SpecificitySelectors and return
		return selectorsToSort.stream().map((ss) -> ss.getSelector()).collect(Collectors.toList());
	}


	/**
	 * Transform all selectors that match a given element into a list of SpecificitySelector instances
	 * Use that list to sort the selectors in place, and then return the MSelectors contained by the SpecificitySelectors instances in the sorted list
	 * @param element
	 * @return
	 */
	public List<MSelector> sortSelectorsForMatchedElem(String element)
	{
		return sortSelectorsForMatchedElem(new ArrayList<>(_elementSelectors.get(element).keySet()), _elementSelectors.get(element));
	}


	/**
	 * @param selectors
	 */
	public static void sortBySpecificity(List<SpecificitySelector> selectors)
	{
		Collections.sort(selectors, (s1, s2) ->
		{
			int value1 = s1.getSpecificity();
			int value2 = s2.getSpecificity();

			//if two selectors have the same _specificity, we need to verify the position in the file or the position of the file in the DOM document
			if (value1 == value2)
			{
				int fileOrder1 = s1.getFileOrder();
				int fileOrder2 = s2.getFileOrder();

				// if both selectors occur in same file, we simply check which selector has a higher rule number (placed lower in file)
				if(fileOrder1 == fileOrder2)
				{
					int lineNo1 = s1.getLineNumber();
					int lineNo2 = s2.getLineNumber();

					// if both selectors occur on the same line, we verify which selector occurs later
					if(lineNo1 == lineNo2)
					{
						return Integer.compare(s2.getOrder(), s1.getOrder());
					}
					else
					{
						return Integer.compare(s2.getLineNumber(), s1.getLineNumber());
					}
				}
				else // otherwise we check which file was included later-on in the DOM (embedded/internal styles will always have a higher order than external styles_
				{
					return Integer.compare(fileOrder2, fileOrder1);
				}
			}

			return Integer.compare(value2, value1);
		});
	}
}

