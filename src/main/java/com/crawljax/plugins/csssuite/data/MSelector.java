package com.crawljax.plugins.csssuite.data;

import java.util.*;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.util.PseudoHelper;
import com.steadystate.css.parser.selectors.PseudoElementSelectorImpl;

import com.crawljax.plugins.csssuite.util.specificity.Specificity;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityCalculator;
import org.w3c.css.sac.*;
import org.w3c.dom.NamedNodeMap;

public class MSelector
{
	private final  Selector _selector;
	private final  List<MProperty> _properties;
	private String _selectorSequence;
	private final int _ruleNumber;

	private boolean _isIgnored;
	private boolean _isMatched;
	private boolean _isNonStructuralPseudo;
	private boolean _hasPseudoElement;

	private int _pseudoLevel;
	private String _keyPseudoClass;
	private String _selectorTextWithoutPseudo;
	private String _pseudoElement;

	private final LinkedHashMap<String, String> _nonStructuralPseudoClasses;
	private final LinkedHashMap<String, String> _structuralPseudoClasses;

	private final List<ElementWrapper> _matchedElements;
	private final Specificity _specificity;


	/**
	 * Constructor for testing purposes only
	 * @param selector
	 * @param ruleNumber
	 */
	public MSelector(Selector selector, int ruleNumber)
	{
		this(selector, new ArrayList<>(), ruleNumber);
	}


	/**
	 * Constructor
	 * @param selector: the selector text (CSS).
	 * @param properties: the properties that are contained in this selector
	 * @param ruleNumber: the lineNumber on which the rule, in which this selector is contained, exists in the file/html document
	 */
	public MSelector(Selector selector,  List<MProperty> properties, int ruleNumber) {
		_selector = selector;
		_properties = properties;
		_ruleNumber = ruleNumber;

		_selectorSequence = selector.toString().trim();
		_isIgnored = _selectorSequence.contains(":not") || _selectorSequence.contains("[disabled]");

		try
		{
			RecursiveFilterUniversalSelector(selector);
		}
		catch (Exception ex)
		{
			LogHandler.error(ex, "Error in filtering universal selectors in selector '%s':", selector);
		}

		_matchedElements = new ArrayList<>();
		_nonStructuralPseudoClasses = new LinkedHashMap<>();
		_structuralPseudoClasses = new LinkedHashMap<>();
		_keyPseudoClass = "";

		try
		{
			DeterminePseudo();
		}
		catch(Exception ex)
		{
			LogHandler.error(ex, "Error in determining pseudo presence in selector '%s':", selector);
		}

		_specificity = new SpecificityCalculator().ComputeSpecificity(_selectorSequence,
				(_nonStructuralPseudoClasses.size() + _structuralPseudoClasses.size()),
				_hasPseudoElement);
	}


	/**
	 * Recursively filter every selector in the sequence on universal selectors that were added by the CssParser
	 * @param selector
	 */
	private void RecursiveFilterUniversalSelector(Selector selector)
	{
		if(selector instanceof PseudoElementSelectorImpl)
		{
			String selectorText = selector.toString();
			if(selectorText.contains("*"))
			{
				UpdateSelectorSequence(selectorText);
			}
		}
		else if(selector instanceof SimpleSelector)
		{
			String selectorText = selector.toString();
			if(selectorText.contains("*") && !selectorText.equals("*") && !selectorText.contains("\\["))
			{
				UpdateSelectorSequence(selectorText);
			}
		}
		else if (selector instanceof DescendantSelector)
		{
			DescendantSelector dSelector = (DescendantSelector)selector;
			RecursiveFilterUniversalSelector(dSelector.getSimpleSelector());
			RecursiveFilterUniversalSelector(dSelector.getAncestorSelector());
		}
		else if (selector instanceof SiblingSelector)
		{
			SiblingSelector dSelector = (SiblingSelector)selector;
			RecursiveFilterUniversalSelector(dSelector.getSelector());
			RecursiveFilterUniversalSelector(dSelector.getSiblingSelector());
		}
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
		if(_selectorSequence.contains(":not"))
			return;

		if(_selectorSequence.contains(":"))
		{
			_selectorTextWithoutPseudo = _selectorSequence;

			//find all pseudo selectors in the whole selector
			_pseudoLevel = 0;
			RecursiveParsePseudoClasses(_selector);

			for(String value : _nonStructuralPseudoClasses.values())
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
			_hasPseudoElement = true;
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


	private void UpdateSelectorSequence(String part)
	{
		//first escape the selectorText (which will be used as a regular expression below)
		part = part.replaceAll("\\*", "\\\\*");
		String replacement = part.replaceAll("\\*", "");

		//replace part of the original selector sequence, by a string without an asterisk
		_selectorSequence = _selectorSequence.replaceAll(part, replacement);
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

			_nonStructuralPseudoClasses.put(parts[0], pseudo);
			_isNonStructuralPseudo = true;
		}
		else
		{
			_structuralPseudoClasses.put(parts[0], pseudo);
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


	/** Getter */
	public int GetRuleNumber() { return _ruleNumber; }

	/** Getter */
	public List<MProperty> GetProperties() { return _properties; }

	/** Getter */
	public String GetSelectorText() { return _selectorSequence;	}

	/** Getter */
	public boolean IsIgnored() { return _isIgnored; }

	/** Getter */
	public boolean IsNonStructuralPseudo() { return _isNonStructuralPseudo; }

	/** Getter */
	public boolean HasPseudoElement() { return _hasPseudoElement; }

	/** Getter */
	public String GetPseudoClass() { return _keyPseudoClass; }

	/** Getter */
	public boolean IsMatched() { return _isMatched; }

	/** Getter */
	public Specificity GetSpecificity() { return _specificity; }


	/**
	 * @return css code that is usable to query a DOM
	 */
	public String GetFilteredSelectorText(){
		if(_isNonStructuralPseudo)
			return _selectorTextWithoutPseudo;

		return _selectorSequence;
	}


	/**
	 * Indicate that this selector matches to one or more elements in a DOM
	 * @param matched
	 */
	public void SetMatched(boolean matched)
	{
		_isMatched = matched;
	}


	/**
	 * Add a DOM element that matches this selector
	 * @param element
	 */
	public void AddMatchedElement(ElementWrapper element)
	{
		if (element != null)
		{
			_matchedElements.add(element);
			_isMatched = true;
		}
	}


	/**
	 * Based on the W3C CSS3 specification, some elements are compatible with some pseudo-classes and others not
	 * @param elementType
	 * @param attributes
	 * @return whether the given element is compatible with the 'key' pseudo-class of this selector
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
	 * @param otherSelector
	 * @return true if the 'key' (right-most) pseudo-class is equal to the key pseudo-class of the other selector
	 */
	public boolean CompareKeyPseudoClass(MSelector otherSelector)
	{
		return _keyPseudoClass.equals(otherSelector.GetPseudoClass());
	}


	/**
	 * @return true if any property is effective
	 */
	public boolean HasEffectiveProperties()
	{
		return _properties.stream().anyMatch((property) -> property.IsEffective());
	}


	/**
	 * @return size of the css code without whitespace + property size
	 */
	public int ComputeSizeBytes()
	{
		int propsSize = 0;
		for (MProperty prop : _properties)
		{
			propsSize += prop.ComputeSizeBytes();
		}
		return (propsSize + _selectorSequence.trim().replace(" ", "").getBytes().length);
	}


	/**
	 * Remove any property that has not been deemed effective
	 */
	public void RemoveIneffectiveProperties()
	{
		_properties.removeIf((MProperty) -> !MProperty.IsEffective());
	}


	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append("Selector: " + _selectorSequence + "\n");
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