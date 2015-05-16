package com.crawljax.plugins.cilla.analysis;

import java.util.*;

import com.crawljax.plugins.cilla.util.PseudoHelper;
import com.steadystate.css.parser.selectors.PseudoElementSelectorImpl;

import com.crawljax.plugins.cilla.util.specificity.Specificity;
import com.crawljax.plugins.cilla.util.specificity.SpecificityCalculator;
import org.w3c.css.sac.*;
import org.w3c.dom.NamedNodeMap;

/**
 * POJO class for a CSS selector.
 * 
 */
public class MSelector
{
	private String _selectorText;
	private boolean _isIgnored;
	private Specificity _specificity;
	private boolean _isMatched;
	private boolean _effective;

	private Selector _selector;
	private List<ElementWrapper> _matchedElements;
	private List<MProperty> _properties;

	private boolean _isNonStructuralPseudo;
	private boolean _isPseudoElement;

	private int _pseudoLevel;
	private String _keyPseudoClass;
	private String _selectorTextWithoutPseudo;
	private LinkedHashMap<String, String> _pseudoClasses;

	private String _pseudoElement;
	private int _ruleNumber;

	//todo: only used in test components
	public MSelector(List<MProperty> properties)
	{
		this(null, properties, 1);
	}

	/**
	 * Constructor.
	 * 
	 * @param selector
	 *            the selector text (CSS).
	 */
	public MSelector(Selector selector,  List<MProperty> properties, int ruleNumber)
	{
		_selector = selector;
		_ruleNumber = ruleNumber;
		_selectorText = selector.toString();
		_isIgnored = _selectorText.contains(":not");
		_properties = properties;

		_specificity = new SpecificityCalculator().getSpecificity(_selectorText);
		_matchedElements = new ArrayList<>();
		_pseudoClasses = new LinkedHashMap<>();
		_keyPseudoClass = "";

		DeterminePseudo();
	}

	/**
	 * Determines whether this selector contains pseudo-selectors
	 * If they do, then find combinations of ancestor selector and pseudo-selector
	 * Foreach selector that is a non-structural pseudo-selector, we filter the _selectorText with that pseudo-selector,
	 * so that we can use the selector to track any elements (without pseudo-state in a DOM tree)
	 */
	private void DeterminePseudo()
	{
		//todo: what to do with negation pseudo?
		if(_selectorText.contains(":not"))
			return;

		if(_selectorText.contains(":"))
		{
			_selectorTextWithoutPseudo = _selectorText;

			//find all pseudo selectors in the whole selector
			_pseudoLevel = 0;
			RecursiveParsePseudoClasses(_selector);

			for(String value : _pseudoClasses.values())
			{
				//escape brackets for regex compare
				if(value.contains("lang"))
				{
					value = value.replace("(", "\\(");
					value = value.replace(")", "\\)");
				}

				_selectorTextWithoutPseudo = _selectorTextWithoutPseudo.replaceFirst(value, "");
			}
		}
	}


	/**
	 * Recursively check every selector for a pseudo-class, starting at the key-selector (right-most)
	 * If a pseudo-class is present, set it via PutPseudoClass
	 * @param selector
	 */
	private void RecursiveParsePseudoClasses(Selector selector)
	{
		if(selector instanceof PseudoElementSelectorImpl)
		{
			_isPseudoElement = true;
			_pseudoElement = ":" + selector.toString();
		}
		else if(selector instanceof SimpleSelector)
		{
			PutPseudoClass(selector.toString().split(":"));
		}
		else if (selector instanceof DescendantSelector)
		{
			DescendantSelector dSelector = (DescendantSelector)selector;

			RecursiveParsePseudoClasses(dSelector.getSimpleSelector());

			_pseudoLevel++;
			RecursiveParsePseudoClasses(dSelector.getAncestorSelector());
		}
		else if (selector instanceof SiblingSelector)
		{
			SiblingSelector dSelector = (SiblingSelector)selector;

			RecursiveParsePseudoClasses(dSelector.getSelector());

			_pseudoLevel++;
			RecursiveParsePseudoClasses(dSelector.getSiblingSelector());
		}
	}


	/**
	 *
	 * @param parts
	 */
	private void PutPseudoClass(String[] parts)
	{
		//will be 3 if pseudo in :not
		if(parts.length != 2)
			return;

		String pseudo = ":" + parts[1];
		if(PseudoHelper.IsNonStructuralPseudo(pseudo))
		{
			if(_pseudoLevel == 0)
				_keyPseudoClass = pseudo;

			_pseudoClasses.put(parts[0], pseudo);
			_isNonStructuralPseudo = true;
		}
	}

	/**
	 *
	 * @param attributes
	 * @param attributeName
	 * @return
	 */
	private static String GetAttributeValue(NamedNodeMap attributes, String attributeName)
	{
		return attributes.getNamedItem(attributeName).getNodeValue();
	}



	public int GetRuleNumber() { return _ruleNumber; }

	public boolean IsIgnored() { return _isIgnored; }

	public Specificity getSpecificity() { return _specificity; }

	public List<MProperty> getProperties() { return _properties; }

	public String getSelectorText() {
		if(_isNonStructuralPseudo)
			return _selectorTextWithoutPseudo;

		return _selectorText;
	}


	public boolean IsNonStructuralPseudo() { return _isNonStructuralPseudo; }

	public boolean IsPseudoElement() { return _isPseudoElement; }

	public String GetPseudoClass() { return _keyPseudoClass; }

	public boolean isMatched() { return _isMatched; }

	public void setMatched(boolean matched) { _isMatched = matched; }

	public void addMatchedElement(ElementWrapper element) {
		if (element != null) {
			_matchedElements.add(element);
			setMatched(true);
		}
	}

	public boolean isEffective() { return _effective; }

	public void setEffective(boolean effective) { _effective = effective; }


	/**
	 *
	 * @param elementType
	 * @param attributes
	 * @return
	 */
	public boolean CheckPseudoCompatibility(String elementType, NamedNodeMap attributes)
	{
		switch (_keyPseudoClass)
		{
			case ":link":
			case ":visited":
				if(elementType.equalsIgnoreCase("a") && GetAttributeValue(attributes, "href") != null)
					return true;
				break;
			case ":checked":
				if(elementType.equalsIgnoreCase("input"))
				{
					String type = GetAttributeValue(attributes, "type");
					if(type.equalsIgnoreCase("checkbox") || type.equalsIgnoreCase("radio") || type.equalsIgnoreCase("option"))
					 	return true;
				}
				break;
			case ":focus":
			case ":active":
				if((_keyPseudoClass.equals(":active") && elementType.equalsIgnoreCase("a")) || (elementType.equalsIgnoreCase("textarea")))
					return true;
				if(elementType.equalsIgnoreCase("input"))
				{
					String type = GetAttributeValue(attributes, "type");
					if(type.equalsIgnoreCase("button") || type.equalsIgnoreCase("text"))
						return true;
				}
				break;
			default:
				return true;
		}

		return false;
	}


	/**
	 *
	 * @param otherSelector
	 * @return
	 */
	public boolean CompareKeyPseudoClass(MSelector otherSelector)
	{
		return _keyPseudoClass.equals(otherSelector.GetPseudoClass());
	}


	/**
	 *
	 * @return
	 */
	public boolean hasEffectiveProperties()
	{
		for (MProperty p : _properties) {
			if (p.IsEffective()) {
				return true;
			}
		}

		return false;
	}

	/**
	 *
	 * @return
	 */
	public int getSize()
	{
		int propsSize = 0;
		for (MProperty prop : _properties) {
			propsSize += prop.GetSize();
		}
		return (propsSize + _selectorText.trim().replace(" ", "").getBytes().length);
	}



	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append("Selector: " + _selectorText + "\n");
		buffer.append(" Matched?: " + _isMatched + "\n");

		if (_matchedElements.size() > 0) {
			buffer.append(" Matched elements:: \n");
		}

		for (ElementWrapper eWrapper : _matchedElements) {
			buffer.append(eWrapper.toString());
		}

		return buffer.toString();
	}
}