package com.crawljax.plugins.cilla.analysis;

import java.util.*;

import com.crawljax.plugins.cilla.util.Constants;
import com.steadystate.css.parser.selectors.ChildSelectorImpl;
import org.apache.log4j.Logger;

import com.crawljax.plugins.cilla.util.CssToXpathConverter;
import com.crawljax.plugins.cilla.util.specificity.Specificity;
import com.crawljax.plugins.cilla.util.specificity.SpecificityCalculator;
import org.w3c.css.sac.*;
import org.w3c.css.selectors.ChildSelector;

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
	private LinkedHashMap<String, String> pseudoClasses;
	private String selectorTextWithoutPseudo;

	//todo: only used in test components
	public MSelector(List<MProperty> properties) {
		this(null, properties);
	}

	/**
	 * Constructor.
	 * 
	 * @param selector
	 *            the selector text (CSS).
	 */
	public MSelector(Selector selector,  List<MProperty> properties) {
		this.selectorText = selector.toString();
		this.selector = selector;
		this.isIgnored = selectorText.contains(":not");
		this.matchedElements = new ArrayList<ElementWrapper>();
		this.properties = properties;
		this.pseudoClasses = new LinkedHashMap<>();

		setXPathSelector();
		setSpecificity();
		determinePseudo();
	}

	/**
	 * Determines whether this selector contains pseudo-selectors
	 * If they do, then find combinations of ancestor selector and pseudo-selector
	 * Foreach selector that is a non-structural pseudo-selector, we filter the selectorText with that pseudo-selector,
	 * so that we can use the selector to track any elements (without pseudo-state in a DOM tree)
	 */
	private void determinePseudo(){

		//todo: what to do with negation pseudo?
		if(selectorText.contains(":not"))
			return;

		if(selectorText.contains(":"))
		{
			selectorTextWithoutPseudo = selectorText;

			//find all pseudo selectors in the whole selector
			RecursiveParsePseudos(this.selector);

			for(String value : pseudoClasses.values())
			{
				boolean isLang = false;

				//escape brackets
				if(value.contains("lang"))
				{
					isLang = true;
					value = value.replace("(", "\\(");
					value = value.replace(")", "\\)");
				}

				if(isLang || Constants.NonStructuralPseudos().contains(value))
				{
					//replaceFirst allowed here, because pseudoClasses is a LinkedHashMap (ordered)
					selectorTextWithoutPseudo = selectorTextWithoutPseudo.replaceFirst(value, "");
					isNonStructuralPseudo = true;
				}
			}
		}
	}


	private void RecursiveParsePseudos(Selector selector)
	{
		if(selector instanceof SimpleSelector)
		{
			TryPutPseudo(selector.toString().split(":"));
		}
		else if (selector instanceof DescendantSelector)
		{
			DescendantSelector dSelector = (DescendantSelector)selector;

			TryPutPseudo(dSelector.getSimpleSelector().toString().split(":"));

			RecursiveParsePseudos(dSelector.getAncestorSelector());
		}
		else if (selector instanceof SiblingSelector)
		{
			SiblingSelector dSelector = (SiblingSelector)selector;

			TryPutPseudo(dSelector.getSelector().toString().split(":"));

			RecursiveParsePseudos(dSelector.getSiblingSelector());
		}
	}

	private void TryPutPseudo(String[] parts)
	{
		//will be 3 if :not
		if(parts.length > 2)
		{
		}

		if(parts.length == 2)
		{
			pseudoClasses.put(parts[0], ":" + parts[1]);
		}
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

				// if two selectors have the same com.crawljax.plugins.cilla.util.specificity, the
				// one closest to element is
				// effective
				if (value1 == value2) {
					return -1;
				}

				return new Integer(value1).compareTo(new Integer(value2));
			}

		});

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
