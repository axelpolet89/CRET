package com.crawljax.plugins.cilla.data;

import com.crawljax.plugins.cilla.util.SuiteStringBuilder;
import org.w3c.dom.Element;

import com.crawljax.util.DomUtils;

/**
 * Wraps the org.w3c.dom.Element class, and combines a given Element with the state crawljax found it in
 */
public class ElementWrapper 
{
	private String _stateName;
	private Element _element;


	/**
	 * Constructor
	 * @param stateName  name of the corresponding DOM state.
	 * @param element the affected element.
	 */
	public ElementWrapper(String stateName, Element element)
	{
		_stateName = stateName;
		_element = element;
	}

	/** Getter */
	public String GetStateName()
	{
		return _stateName;
	}

	/** Getter */
	public Element GetElement()
	{
		return _element;
	}


	@Override
	public String toString()
	{
		SuiteStringBuilder buffer = new SuiteStringBuilder();

		buffer.append("Statename: " + _stateName);
		buffer.appendLine("<" + _element.getNodeName() + " " + DomUtils.getAllElementAttributes(_element) + ">");

		if (_element.getNodeValue() != null)
		{
			buffer.append(_element.getNodeValue());
		}

		buffer.append("\n");
		return buffer.toString();
	}
}