package com.crawljax.plugins.cilla.analysis;

import org.w3c.dom.Element;

import com.crawljax.util.DomUtils;

public class ElementWrapper 
{
	private String _stateName;
	private Element _element;

	/**
	 * @param stateName
	 *            name of the corresponding DOM state.
	 * @param element
	 *            the affected element.
	 */
	public ElementWrapper(String stateName, Element element) {
		_stateName = stateName;
		_element = element;
	}

	public String getStateName()
	{
		return _stateName;
	}

	public Element getElement()
	{
		return _element;
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append("Statename: " + _stateName + "\n");
		buffer.append("<" + _element.getNodeName() + " "
		        + DomUtils.getAllElementAttributes(_element) + ">");

		if (_element.getNodeValue() != null) {
			buffer.append(_element.getNodeValue());
		}

		buffer.append("\n");
		return buffer.toString();
	}
}
