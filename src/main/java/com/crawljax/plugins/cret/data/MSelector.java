package com.crawljax.plugins.cret.data;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.data.declarations.MDeclaration;
import com.crawljax.plugins.cret.util.PseudoHelper;
import com.crawljax.plugins.cret.util.specificity.Specificity;
import com.crawljax.plugins.cret.util.specificity.SpecificityCalculator;

import com.steadystate.css.dom.Property;
import com.steadystate.css.parser.media.MediaQuery;
import com.steadystate.css.parser.selectors.PseudoElementSelectorImpl;

import org.w3c.css.sac.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Created by axel on 6/9/2015.
 *
 * Represent a CSS selector
 * Containing one or more declarations
 *
 * Holds selector-specific information, including pseudo-selector presence
 */
public class MSelector
{
	private final Selector _selector;
	private final List<MDeclaration> _declarations;
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
	 * @param declarations: the declarations that are contained in this selector
	 * @param ruleNumber: the lineNumber on which the rule, in which this selector is contained, exists in the file/html document
	 */
	public MSelector(Selector w3cSelector, List<MDeclaration> declarations, int ruleNumber, int order, List<MediaQuery> queries, MCssRuleBase parent, String w3cError)
	{
		_selector = w3cSelector;
		_declarations = declarations;
		_lineNumber = ruleNumber;
		_order = order;
		_selectorText = w3cSelector.toString().trim();
		_mediaQueries = queries;
		_parent = parent;
		_w3cError = w3cError;

		init();
	}


	/**
	 * Partial copy constructor, generate from new w3cSelector, with declarations from old MSelector
	 * @param w3cSelector
	 * @param mSel
	 */
	public MSelector(Selector w3cSelector, MSelector mSel)
	{
		this(w3cSelector, mSel.getDeclarations(), mSel.getLineNumber(), mSel.getOrder(), mSel.getMediaQueries(), mSel.getParent(), "");

		// set additional declarations, left empty by default constructor
		_isMatched = mSel.isMatched();
		_matchedElements.addAll(mSel.getMatchedElements());
	}


	/**
	 * Full copy constructor
	 */
	public MSelector(MSelector mSel)
	{
		_selector = mSel.getW3CSelector();
		_selectorText = _selector.toString().trim();
		_mediaQueries = new ArrayList<>();
		_mediaQueries.addAll(mSel.getMediaQueries());
		_lineNumber = mSel.getLineNumber();
		_order = mSel.getOrder();
		_parent = mSel.getParent();
		_w3cError = mSel.getW3CError();

		//copy construct declarations
		_declarations = mSel.getDeclarations().stream().map(MDeclaration::new).collect(Collectors.toList());

		init();

		// set additional declarations, left empty by default constructor
		_isMatched = mSel.isMatched();
		_matchedElements.addAll(mSel.getMatchedElements());
	}


	/**
	 * Initialize other declarations, by recursively parsing w3c selector object
	 */
	private void init()
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
			_declarations.forEach(p -> p.setEffective(true));
			_selectorText = _selectorText.replace("*","");
		}
		else
		{

			try
			{
				recursiveFilterUniversalSelector(_selector);
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[MSelector] Error in filtering universal selectors in selector '%s':", _selector);
			}

			try
			{
				determinePseudo();
			}
			catch (Exception ex)
			{
				LogHandler.error(ex, "[MSelector] Error in determining pseudo presence in selector '%s':", _selector);
			}
		}

		_specificity = new SpecificityCalculator().computeSpecificity(_selectorText,
				(_nonStructuralPseudoClasses.size() + _structuralPseudoClasses.size()),
				_hasPseudoElement);
	}


	/**
	 * Recursively filter every selector in the sequence on universal selectors that were added by the CssParser
	 * @param selector
	 */
	private void recursiveFilterUniversalSelector(Selector selector)
	{
		if(selector instanceof PseudoElementSelectorImpl)
		{
			String selectorText = selector.toString();
			if(selectorText.contains("*"))
			{
				filterUniversalSelector(selectorText);
			}
		}
		else if(selector instanceof SimpleSelector)
		{
			String selectorText = selector.toString();
			if(selectorText.contains("*") && !selectorText.equals("*") && !selectorText.contains("["))
			{
				filterUniversalSelector(selectorText);
			}
		}
		else if (selector instanceof DescendantSelector)
		{
			DescendantSelector dSelector = (DescendantSelector)selector;
			recursiveFilterUniversalSelector(dSelector.getSimpleSelector());
			recursiveFilterUniversalSelector(dSelector.getAncestorSelector());
		}
		else if (selector instanceof SiblingSelector)
		{
			SiblingSelector dSelector = (SiblingSelector)selector;
			recursiveFilterUniversalSelector(dSelector.getSelector());
			recursiveFilterUniversalSelector(dSelector.getSiblingSelector());
		}
	}


	/**
	 * Replace part of the selector sequence from the universal selector
	 * @param part
	 */
	private void filterUniversalSelector(String part)
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
	private void determinePseudo()
	{
		if(_selectorText.contains(":"))
		{
			_selectorTextWithoutPseudo = _selectorText;

			//find all pseudo selectors in the whole selector
			_pseudoLevel = 0;
			recursiveParsePseudoClasses(_selector);

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
	private void recursiveParsePseudoClasses(Selector selector)
	{
		if(selector instanceof PseudoElementSelectorImpl)
		{
			_hasPseudoElement = true;
			_keyPseudoElement = ":" + selector.toString();
		}
		else if(selector instanceof SimpleSelector)
		{
			putPseudoClass(selector.toString().split(":"));
		}
		else if (selector instanceof DescendantSelector)
		{
			DescendantSelector dSelector = (DescendantSelector)selector;

			recursiveParsePseudoClasses(dSelector.getSimpleSelector());

			_pseudoLevel++;
			recursiveParsePseudoClasses(dSelector.getAncestorSelector());
		}
		else if (selector instanceof SiblingSelector)
		{
			SiblingSelector dSelector = (SiblingSelector)selector;

			recursiveParsePseudoClasses(dSelector.getSelector());

			_pseudoLevel++;
			recursiveParsePseudoClasses(dSelector.getSiblingSelector());
		}
	}



	/**
	 *
	 * @param parts
	 */
	private void putPseudoClass(String[] parts)
	{
		//will be 3 if pseudo in :not
		if(parts.length != 2)
			return;

		String pseudo = ":" + parts[1];
		if(PseudoHelper.isNonStructuralPseudo(pseudo))
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
	public Selector getW3CSelector() { return _selector; }

	/** Getter */
	public String getSelectorText() { return _selectorText;	}

	/** Getter */
	public List<MDeclaration> getDeclarations() { return _declarations; }

	/** Getter */
	public int getLineNumber() { return _lineNumber; }

	/** Getter */
	public int getOrder() { return _order; }

	/** Getter */
	public String getW3CError() { return _w3cError; }

	/** Getter */
	public List<MediaQuery> getMediaQueries() { return _mediaQueries; }

	/** Getter */
	public MCssRuleBase getParent() { return _parent; };

	/** Getter */
	public boolean isIgnored() { return _isIgnored; }

	/** Getter */
	public boolean isNonStructuralPseudo() { return _isNonStructuralPseudo; }

	/** Getter */
	public boolean hasPseudoElement() { return _hasPseudoElement; }

	/** Getter */
	public String getPseudoElement() { return _keyPseudoElement; }

	/** Getter */
	public String getPseudoClass() { return _keyPseudoClass; }

	/** Getter */
	public boolean isMatched() { return _isMatched; }

	/** Getter */
	public Specificity getSpecificity() { return _specificity; }

	/** Getter */
	public List<ElementWrapper> getMatchedElements() { return _matchedElements; }


	/**
	 * @return css code that is usable to query a DOM, e.g. with filtered-out pseudo selectors
	 */
	public String getFilteredSelectorText()
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
	public boolean checkPseudoCompatibility(String elementType, NamedNodeMap attributes)
	{
		switch (_keyPseudoClass)
		{
			case ":link":
			case ":visited":
				if(elementType.equalsIgnoreCase("a") && getAttributeValue(attributes, "href") != null)
					return true;
				break;
			case ":checked":
				if(elementType.equalsIgnoreCase("input"))
				{
					String type = getAttributeValue(attributes, "type");
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
					String type = getAttributeValue(attributes, "type");
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
	private static String getAttributeValue(NamedNodeMap attributes, String attributeName)
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
	public void setMatched(boolean matched)
	{
		_isMatched = matched;
	}


	/**
	 * Add a DOM element that matches this selector
	 * @param element
	 */
	public void addMatchedElement(ElementWrapper element)
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
	public boolean hasEqualPseudoElement(MSelector otherSelector)
	{
		return _keyPseudoElement.equals(otherSelector.getPseudoElement());
	}



	/**
	 * @param otherSelector
	 * @return true if the 'key' (right-most) pseudo-class is equal to the key pseudo-class of the other selector
	 */
	public boolean hasEqualPseudoClass(MSelector otherSelector)
	{
		return _keyPseudoClass.equals(otherSelector.getPseudoClass());
	}



	/**
	 * Verifies if the otherSelector applies under the same conditions as this selector
	 * @param otherSelector the less specific selector in relation to this selector
	 * @return
	 */
	public boolean HasEqualMediaQueries(MSelector otherSelector)
	{
		List<MediaQuery> commonQueries = findCommonMediaQueries(otherSelector);
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
	private List<MediaQuery> findCommonMediaQueries(MSelector otherSelector)
	{
		List<MediaQuery> result = new ArrayList<>();

		List<MediaQuery> otherQueries = otherSelector.getMediaQueries();

		for(MediaQuery query : _mediaQueries)
		{
			List<Property> declarations = query.getProperties();

			for(MediaQuery otherQuery : otherQueries)
			{
				// both queries apply to same media type
				if(query.getMedia().equals(otherQuery.getMedia()))
				{
					List<Property> otherProperties = otherQuery.getProperties();

					if(declarations.size() == otherProperties.size())
					{
						boolean matched = true;

						HashMap<String, String> propertiesToMatch = new HashMap<>();

						for(Property prop : declarations)
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
	 * @return true if any declaration contained in this selector is effective
	 */
	public boolean hasEffectiveDeclarations()
	{
		return _declarations.stream().anyMatch((declaration) -> declaration.isEffective());
	}


	/**
	 *
	 * @param mDeclaration
	 */
	public void restoreDeclaration(MDeclaration mDeclaration)
	{
		_declarations.add(mDeclaration);
	}


	/**
	 *
	 * @param newProps
	 */
	public void setNewDeclarations(List<MDeclaration> newProps)
	{
		_declarations.clear();
		_declarations.addAll(newProps);
	}


	/**
	 * Remove any declaration that has not been deemed effective
	 */
	public void removeIneffectiveDeclarations()
	{
		_declarations.removeIf((p) -> !p.isIgnored() && !p.isEffective());
	}


	/**
	 * Remove any declaration that performs an invalid undo
	 */
	public void removeInvalidUndoDeclarations()
	{
		_declarations.removeIf((p) -> !p.isIgnored() && p.isInvalidUndo());
	}


	/**
	 *
	 * @param declarations
	 */
	public void removeDeclarations(List<MDeclaration> declarations)
	{
		_declarations.removeAll(declarations);
	}


	/**
	 *
	 * @param declarations
	 */
	public void removeDeclarationsByText(List<MDeclaration> declarations)
	{
		List<MDeclaration> toRemove = new ArrayList<>();
		for(MDeclaration mDeclaration : declarations)
		{
			for(MDeclaration thisProperty : _declarations)
			{
				if(mDeclaration.toString().equals(thisProperty.toString()))
				{
					toRemove.add(thisProperty);
				}
			}
		}

		_declarations.removeAll(toRemove);
	}

	@Override
	public String toString()
	{
		return String.format("%s (line '%d')", _selectorText, _lineNumber);
	}
}