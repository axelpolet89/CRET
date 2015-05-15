package com.crawljax.plugins.cilla.analysis;

import java.util.*;

import com.crawljax.plugins.cilla.util.Constants;
import com.steadystate.css.parser.selectors.PseudoElementSelectorImpl;
import org.apache.log4j.Logger;

import com.crawljax.plugins.cilla.util.CssToXpathConverter;
import com.crawljax.plugins.cilla.util.specificity.Specificity;
import com.crawljax.plugins.cilla.util.specificity.SpecificityCalculator;
import org.w3c.css.sac.*;
import org.w3c.dom.NamedNodeMap;

/**
 * POJO class for a CSS selector.
 * 
 */
public class MSelector {
	private static final Logger LOGGER = Logger.getLogger(MSelector.class.getName());

	private String selectorText;
	private String xpathSelector;
	private boolean isIgnored;
	private Specificity specificity;
	private boolean isMatched;
	private boolean effective;

	private Selector selector;
	private List<ElementWrapper> matchedElements;
	private List<MProperty> properties;

	private boolean isNonStructuralPseudo;
	private boolean isPseudoElement;

	private int pseudoLevel;
	private String keyPseudoClass;
	private String selectorTextWithoutPseudo;
	private LinkedHashMap<String, String> pseudoClasses;

	private String pseudoElement;
	private int ruleNumber;

	//todo: only used in test components
	public MSelector(List<MProperty> properties) {
		this(null, properties, 1);
	}

	/**
	 * Constructor.
	 * 
	 * @param selector
	 *            the selector text (CSS).
	 */
	public MSelector(Selector selector,  List<MProperty> properties, int ruleNumber) {
		this.selectorText = selector.toString();
		this.selector = selector;
		this.isIgnored = selectorText.contains(":not");
		this.matchedElements = new ArrayList<ElementWrapper>();
		this.properties = properties;
		this.pseudoClasses = new LinkedHashMap<>();
		this.ruleNumber = ruleNumber;
		this.keyPseudoClass = "";

		setXPathSelector();
		setSpecificity();
		DeterminePseudo();
	}


	public int getRuleNumber() {
		return ruleNumber;
	}

	public boolean IsNonStructuralPseudo() { return isNonStructuralPseudo; }

	public boolean IsPseudoElement() { return isPseudoElement; }

	public String GetPseudoClass() { return keyPseudoClass; }


	/**
	 * Determines whether this selector contains pseudo-selectors
	 * If they do, then find combinations of ancestor selector and pseudo-selector
	 * Foreach selector that is a non-structural pseudo-selector, we filter the selectorText with that pseudo-selector,
	 * so that we can use the selector to track any elements (without pseudo-state in a DOM tree)
	 */
	private void DeterminePseudo(){

		//todo: what to do with negation pseudo?
		if(selectorText.contains(":not"))
			return;

		if(selectorText.contains(":"))
		{
			selectorTextWithoutPseudo = selectorText;

			//find all pseudo selectors in the whole selector
			pseudoLevel = 0;
			RecursiveParsePseudoClasses(this.selector);

			for(String value : pseudoClasses.values())
			{
				//escape brackets for regex compare
				if(value.contains("lang"))
				{
					value = value.replace("(", "\\(");
					value = value.replace(")", "\\)");
				}

				selectorTextWithoutPseudo = selectorTextWithoutPseudo.replaceFirst(value, "");
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
			isPseudoElement = true;
			pseudoElement = ":" + selector.toString();
		}
		else if(selector instanceof SimpleSelector)
		{
			PutPseudoClass(selector.toString().split(":"));
		}
		else if (selector instanceof DescendantSelector)
		{
			DescendantSelector dSelector = (DescendantSelector)selector;

			RecursiveParsePseudoClasses(dSelector.getSimpleSelector());

			pseudoLevel++;
			RecursiveParsePseudoClasses(dSelector.getAncestorSelector());
		}
		else if (selector instanceof SiblingSelector)
		{
			SiblingSelector dSelector = (SiblingSelector)selector;

			RecursiveParsePseudoClasses(dSelector.getSelector());

			pseudoLevel++;
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
		if(Constants.IsNonStructuralPseudo(pseudo))
		{
			if(pseudoLevel == 0)
				keyPseudoClass = pseudo;

			pseudoClasses.put(parts[0], pseudo);
			isNonStructuralPseudo = true;
		}
	}


	/**
	 *
	 * @param elementType
	 * @param attributes
	 * @return
	 */
	public boolean CheckPseudoCompatibility(String elementType, NamedNodeMap attributes)
	{
		String pseudo = pseudoClasses.values().toArray()[0].toString();

		switch (pseudo)
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
				if((pseudo.equals(":active") && elementType.equalsIgnoreCase("a")) || (elementType.equalsIgnoreCase("textarea")))
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

	public boolean CompareKeyPseudoClass(MSelector otherSelector)
	{
		return keyPseudoClass.equals(otherSelector.GetPseudoClass());
	}

	private static String GetAttributeValue(NamedNodeMap attributes, String attributeName)
	{
		return attributes.getNamedItem(attributeName).getNodeValue();
	}


	public Specificity getSpecificity() {
		return specificity;
	}

	private void setSpecificity() {
		this.specificity = new SpecificityCalculator().getSpecificity(this.selectorText);
	}

	public void addProperty(MProperty property) {
		this.properties.add(property);
	}

	public List<MProperty> getProperties() {
		return this.properties;
	}

	public boolean isEffective() {
		return effective;
	}

	public void setEffective(boolean effective) {
		this.effective = effective;
	}

	public boolean isMatched() {

		return isMatched;
	}

	public void setMatched(boolean matched) {
		this.isMatched = matched;
	}

	public List<ElementWrapper> getAffectedElements() {
		return matchedElements;
	}

	public void addMatchedElement(ElementWrapper element) {
		if (element != null) {
			this.matchedElements.add(element);
			setMatched(true);
		}
	}

	//todo: refactor usages in CillaPlugin and Visualizer
	public String getSelectorText() {
		if(isNonStructuralPseudo)
			return selectorTextWithoutPseudo;

		return selectorText;
	}

	//todo: unused?
	private void setXPathSelector() {
		this.xpathSelector = CssToXpathConverter.convert(this.selectorText);
	}


	//todo: unused?
	public String getXpathSelector() {
		return xpathSelector;
	}

	@Override
	public String toString() {

		StringBuffer buffer = new StringBuffer();

		buffer.append("Selector: " + this.selectorText + "\n");
		buffer.append(" XPath: " + this.xpathSelector + "\n");
		buffer.append(" Matched?: " + this.isMatched + "\n");

		if (this.matchedElements.size() > 0) {
			buffer.append(" Matched elements:: \n");
		}

		for (ElementWrapper eWrapper : this.matchedElements) {
			buffer.append(eWrapper.toString());
		}

		return buffer.toString();
	}

	public boolean isIgnored() {
		return isIgnored;
	}

	public static void orderSpecificity(List<MSelector> selectors) {
		Collections.sort(selectors, new Comparator<MSelector>() {

			public int compare(MSelector o1, MSelector o2) {
				int value1 = o1.getSpecificity().getValue();
				int value2 = o2.getSpecificity().getValue();

				//if two selectors have the same specificity,
				//then the one that is defined later (e.g. a higher row number in the css file)
				//has a higher order
				if (value1 == value2) {
					return new Integer(o1.getRuleNumber()).compareTo(o2.getRuleNumber());
				}

				return new Integer(value1).compareTo(new Integer(value2));
			}

		});

		//we need selectors sorted ascending (from specific to less-specific)
		Collections.reverse(selectors);
	}

	public boolean hasEffectiveProperties() {

		for (MProperty p : this.properties) {
			if (p.isEffective()) {
				return true;
			}
		}

		return false;
	}

	public int getSize() {
		int propsSize = 0;
		for (MProperty prop : this.properties) {
			propsSize += prop.getsize();
		}
		return (propsSize + selectorText.trim().replace(" ", "").getBytes().length);
	}
}