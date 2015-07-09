package com.crawljax.plugins.csssuite.data;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.util.PseudoHelper;
import com.crawljax.plugins.csssuite.util.specificity.Specificity;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityCalculator;

import com.steadystate.css.dom.Property;
import com.steadystate.css.parser.media.MediaQuery;
import com.steadystate.css.parser.selectors.PseudoElementSelectorImpl;

import org.w3c.css.sac.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MSelector
{
	private final Selector _selector;
	private final List<MProperty> _properties;
	private final List<MediaQuery> _mediaQueries;
	private final MCssRuleBase _parent;
	private final String _w3cError;

	private String _selectorText; // possibly updatet in filtering universal selectors
	private final int _lineNumber;
	private final int _order;

	private boolean _isIgnored;
	private boolean _isMatched;
	private boolean _isNonStructuralPseudo;
	private boolean _hasPseudoElement;

	private LinkedHashMap<String, String> _nonStructuralPseudoClasses;
	private LinkedHashMap<String, String> _structuralPseudoClasses;
	private int _pseudoLevel;
	private String _keyPseudoClass;
	private String _selectorTextWithoutPseudo;
	private String _keyPseudoElement;

	private Specificity _specificity;

	private List<ElementWrapper> _matchedElements;

	/**
	 * Constructor
	 *
	 * @param w3cSelector:   the selector text (CSS).
	 * @param properties: the properties that are contained in this selector
	 * @param ruleNumber: the lineNumber on which the rule, in which this selector is contained, exists in the file/html document
	 */
	public MSelector(Selector w3cSelector, List<MProperty> properties, int ruleNumber, int order, List<MediaQuery> queries, MCssRuleBase parent, String w3cError)
	{
		_selector = w3cSelector;
		_properties = properties;
		_lineNumber = ruleNumber;
		_order = order;
		_selectorText = w3cSelector.toString().trim();
		_mediaQueries = queries;
		_parent = parent;
		_w3cError = w3cError;

		Init();
	}


	/**
	 * Partial copy constructor, generate from new w3cSelector, with properties from old MSelector
	 * @param w3cSelector
	 * @param mSel
	 */
	public MSelector(Selector w3cSelector, MSelector mSel)
	{
		this(w3cSelector, mSel.GetProperties(), mSel.GetLineNumber(), mSel.GetOrder(), mSel.GetMediaQueries(), mSel.GetParent(), "");

		// set additional properties, left empty by default constructor
		_isMatched = mSel.IsMatched();
		_matchedElements.addAll(mSel.GetMatchedElements());
	}


	/**
	 * Full copy constructor
	 */
	public MSelector(MSelector mSel)
	{
		_selector = mSel.GetW3cSelector();
		_selectorText = _selector.toString().trim();
		_mediaQueries = new ArrayList<>();
		_mediaQueries.addAll(mSel.GetMediaQueries());
		_lineNumber = mSel.GetLineNumber();
		_order = mSel.GetOrder();
		_parent = mSel.GetParent();
		_w3cError = mSel.GetW3cError();

		//copy construct properties
		_properties = mSel.GetProperties().stream().map(MProperty::new).collect(Collectors.toList());

		Init();

		// set additional properties, left empty by default constructor
		_isMatched = mSel.IsMatched();
		_matchedElements.addAll(mSel.GetMatchedElements());
	}


	/**
	 * Initialize other properties, by recursively parsing w3c selector object
	 */
	private void Init()
	{
		_matchedElements = new ArrayList<>();
		_nonStructuralPseudoClasses = new LinkedHashMap<>();
		_structuralPseudoClasses = new LinkedHashMap<>();
		_keyPseudoClass = "";
		_keyPseudoElement = "";

		_isIgnored = _selectorText.contains(":not") || _selectorText.contains("[disabled]") || !_w3cError.isEmpty();
		if(_isIgnored)
		{
			_isMatched = true;
			_properties.forEach(p -> p.SetEffective(true));
			_selectorText = _selectorText.replace("*","");
		}
		else
		{

			try
			{
				RecursiveFilterUniversalSelector(_selector);
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[MSelector] Error in filtering universal selectors in selector '%s':", _selector);
			}

			try
			{
				DeterminePseudo();
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[MSelector] Error in determining pseudo presence in selector '%s':", _selector);
			}
		}

		_specificity = new SpecificityCalculator().ComputeSpecificity(_selectorText,
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
				FilterUniversalSelector(selectorText);
			}
		}
		else if(selector instanceof SimpleSelector)
		{
			String selectorText = selector.toString();
			if(selectorText.contains("*") && !selectorText.equals("*") && !selectorText.contains("["))
			{
				FilterUniversalSelector(selectorText);
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
	 * Replace part of the selector sequence from the universal selector
	 * @param part
	 */
	private void FilterUniversalSelector(String part)
	{
		//first escape the selectorText (which will be used as a regular expression below)
		part = part.replaceAll("\\*", "\\\\*");
		String replacement = part.replaceAll("\\*", "");

		//replace part of the original selector sequence, by a string without an asterisk
		_selectorText = _selectorText.replaceAll(part, replacement);
	}



	/**
	 * Determines whether this selector contains pseudo-selectors
	 * If they do, then find combinations of ancestor selector and pseudo-selector
	 * Foreach selector that is a non-structural pseudo-selector, we filter the _selectorText with that pseudo-selector,
	 * so that we can use the selector to track any elements (without pseudo-state in a DOM tree)
	 */
	private void DeterminePseudo()
	{
		if(_selectorText.contains(":"))
		{
			_selectorTextWithoutPseudo = _selectorText;

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
			_keyPseudoElement = ":" + selector.toString();
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

			_nonStructuralPseudoClasses.put(parts[0], pseudo);
			_isNonStructuralPseudo = true;
		}
		else
		{
			_structuralPseudoClasses.put(parts[0], pseudo);
		}
	}


	/** Getter */
	public Selector GetW3cSelector() { return _selector; }

	/** Getter */
	public String GetSelectorText() { return _selectorText;	}

	/** Getter */
	public List<MProperty> GetProperties() { return _properties; }

	/** Getter */
	public int GetLineNumber() { return _lineNumber; }

	/** Getter */
	public int GetOrder() { return _order; }

	/** Getter */
	public String GetW3cError() { return _w3cError; }

	/** Getter */
	public List<MediaQuery> GetMediaQueries() { return _mediaQueries; }

	/** Getter */
	public MCssRuleBase GetParent() { return _parent; };

	/** Getter */
	public boolean IsIgnored() { return _isIgnored; }

	/** Getter */
	public boolean IsNonStructuralPseudo() { return _isNonStructuralPseudo; }

	/** Getter */
	public boolean HasPseudoElement() { return _hasPseudoElement; }

	/** Getter */
	public String GetPseudoElement() { return _keyPseudoElement; }

	/** Getter */
	public String GetPseudoClass() { return _keyPseudoClass; }

	/** Getter */
	public boolean IsMatched() { return _isMatched; }

	/** Getter */
	public Specificity GetSpecificity() { return _specificity; }

	/** Getter */
	public List<ElementWrapper> GetMatchedElements() { return _matchedElements; }


	/**
	 * @return css code that is usable to query a DOM, e.g. with filtered-out pseudo selectors
	 */
	public String GetFilteredSelectorText()
	{
		if(_isNonStructuralPseudo)
			return _selectorTextWithoutPseudo;

		return _selectorText;
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
					if(type != null && type.equalsIgnoreCase("checkbox") || type.equalsIgnoreCase("radio") || type.equalsIgnoreCase("option"))
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
					if(type != null && type.equalsIgnoreCase("button") || type.equalsIgnoreCase("text"))
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
	 * @param attributes
	 * @param attributeName
	 * @return
	 */
	/**
	 * @param attributes
	 * @param attributeName
	 * @return the value for the given attribute, or NULL
	 */
	private static String GetAttributeValue(NamedNodeMap attributes, String attributeName)
	{
		if(attributes == null)
			return null;

		Node node = attributes.getNamedItem(attributeName);

		if(node != null)
			return node.getNodeValue();

		return null;
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
	 * @param otherSelector
	 * @return true if the 'key' (right-most) pseudo-class is equal to the key pseudo-class of the other selector
	 */
	public boolean HasEqualPseudoElement(MSelector otherSelector)
	{
		return _keyPseudoElement.equals(otherSelector.GetPseudoElement());
	}



	/**
	 * @param otherSelector
	 * @return true if the 'key' (right-most) pseudo-class is equal to the key pseudo-class of the other selector
	 */
	public boolean HasEqualPseudoClass(MSelector otherSelector)
	{
		return _keyPseudoClass.equals(otherSelector.GetPseudoClass());
	}



	/**
	 * Verifies if the otherSelector applies under the same conditions as this selector
	 * @param otherSelector the less specific selector in relation to this selector
	 * @return
	 */
	public boolean HasEqualMediaQueries(MSelector otherSelector)
	{
		List<MediaQuery> commonQueries = FindCommonMediaQueries(otherSelector);
		if(_mediaQueries.containsAll(commonQueries) && commonQueries.containsAll(_mediaQueries))
		{
			return true;
		}

		return false;
	}


	/**
	 * Find all common media-queries between two selectors
	 * @param otherSelector
	 * @return
	 */
	private List<MediaQuery> FindCommonMediaQueries(MSelector otherSelector)
	{
		List<MediaQuery> result = new ArrayList<>();

		List<MediaQuery> otherQueries = otherSelector.GetMediaQueries();

		for(MediaQuery query : _mediaQueries)
		{
			List<Property> properties = query.getProperties();

			for(MediaQuery otherQuery : otherQueries)
			{
				// both queries apply to same media type
				if(query.getMedia().equals(otherQuery.getMedia()))
				{
					List<Property> otherProperties = otherQuery.getProperties();

					if(properties.size() == otherProperties.size())
					{
						boolean matched = true;

						HashMap<String, String> propertiesToMatch = new HashMap<>();

						for(Property prop : properties)
						{
							propertiesToMatch.put(prop.getName(), prop.getValue().getCssText());
						}

						for (Property otherProp : otherProperties)
						{
							if(propertiesToMatch.containsKey(otherProp.getName()))
							{
								if(!propertiesToMatch.get(otherProp.getName()).equals(otherProp.getValue().getCssText()))
								{
									matched = false;
								}
							}
							else
							{
								matched = false;
							}
						}

						if(matched)
						{
							result.add(query);
						}
					}
				}
			}
		}

		return result;
	}



	/**
	 * @return true if any property contained in this selector is effective
	 */
	public boolean HasEffectiveProperties()
	{
		return _properties.stream().anyMatch((property) -> property.IsEffective());
	}


	/**
	 *
	 * @param mProperty
	 */
	public void RestoreProperty(MProperty mProperty)
	{
		_properties.add(mProperty);
	}


	/**
	 *
	 * @param newProps
	 */
	public void SetNewProperties(List<MProperty> newProps)
	{
		_properties.clear();
		_properties.addAll(newProps);
	}


	/**
	 * Remove any property that has not been deemed effective
	 */
	public void RemoveIneffectiveProperties()
	{
		_properties.removeIf((p) -> !p.IsIgnored() && !p.IsEffective());
	}


	/**
	 * Remove any property that performs an invalid undo
	 */
	public void RemoveInvalidUndoProperties()
	{
		_properties.removeIf((p) -> !p.IsIgnored() && p.IsInvalidUndo());
	}


	/**
	 *
	 * @param properties
	 */
	public void RemoveProperties(List<MProperty> properties)
	{
		_properties.removeAll(properties);
	}


	/**
	 *
	 * @param properties
	 */
	public void RemovePropertiesByText(List<MProperty> properties)
	{
		List<MProperty> toRemove = new ArrayList<>();
		for(MProperty mProperty : properties)
		{
			for(MProperty thisProperty : _properties)
			{
				if(mProperty.toString().equals(thisProperty.toString()))
				{
					toRemove.add(thisProperty);
				}
			}
		}

		_properties.removeAll(toRemove);
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
		return (propsSize + _selectorText.trim().replace(" ", "").getBytes().length);
	}


	/**
	 *
	 * @return
	 */
	public String Print()
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


	@Override
	public String toString()
	{
		return String.format("%s (line '%d')", _selectorText, _lineNumber);
	}
}