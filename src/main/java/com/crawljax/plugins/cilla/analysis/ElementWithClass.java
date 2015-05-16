package com.crawljax.plugins.cilla.analysis;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

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

	public void AddUnmatchedClass(String unmatchedClass)
	{
		_unmatchedClasses.add(unmatchedClass);
	}

	public List<String> GetUnmatchedClasses()
	{
		return _unmatchedClasses;
	}

	public List<String> GetClassValues()
	{
		return _classValues;
	}

	//todo: remove?
	@Override
	public String toString()
	{
		return super.toString();
	}
}
