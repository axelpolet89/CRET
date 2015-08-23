package com.crawljax.plugins.cret.dommodel;

import com.crawljax.plugins.cret.util.CretStringBuilder;
import com.crawljax.util.XPathHelper;
import org.w3c.dom.Element;

import com.crawljax.util.DomUtils;

/**
 * Created by axel on 6/9/2015.
 *
 * Wraps the org.w3c.dom.Element class, and combines a given Element with the state crawljax found it in
 */
public class ElementWrapper 
{
	private final String _stateName;
	private final Element _element;
	private final String _key;


	/**
	 * Constructor
	 * @param stateName  name of the corresponding DOM state.
	 * @param element the affected element.
	 */
	public ElementWrapper(String stateName, Element element)
	{
		_stateName = stateName;
		_element = element;
		_key = stateName+ XPathHelper.getXPathExpression(element);
	}

	/** Getter */
	public Element getElement()
	{
		return _element;
	}

	/** Getter */
	public String getKey() { return _key; }


	@Override
	public String toString()
	{
		CretStringBuilder buffer = new CretStringBuilder();

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