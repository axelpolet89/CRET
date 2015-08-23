package com.crawljax.plugins.cret.data;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

/**
 * Created by axel on 6/9/2015.
 *
 * Extension of ElementWrapper, which holds any classes and unmatched classes for this Element
 */
public class ElementWithClass extends ElementWrapper
{
	private List<String> _classValues;
	private List<String> _unmatchedClasses;

	public ElementWithClass(String stateName, Element element, List<String> classValues) 
	{
		super(stateName, element);

		_classValues = classValues;
		_unmatchedClasses = new ArrayList<>();
	}

	/** Getter */
	public List<String> getUnmatchedClasses()
	{
		return _unmatchedClasses;
	}

	/** Getter */
	public List<String> getClassValues()
	{
		return _classValues;
	}
}
