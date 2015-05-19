package com.crawljax.plugins.cilla.util.specificity;

/**
 * Represents the Specificity Calculation according to http://www.w3.org/TR/css3-selectors/#specificity
 * 
 * B. # count the number of ID attributes in the selector (= b)
 * 
 * C. # count the number of classes, attributes and pseudo-classes in the selector (= c)
 * 
 * D. # count the number of element names and pseudo-elements in the selector (= d)
 *
 */
public class SpecificityCalculator
{
	//holds the current specificity
	private int value;

	//allow for up to 99 IDs, 99 classes, 99 element-types
	private final int BASE = 100;

	/**
	 * The number of units representing a single Element Count. This unit represents (d) in the 4
	 * rules used to calculate com.crawljax.plugins.cilla.util.specificity.
	 */
	private final int ELEMENT_NAMES_AND_PSEUDO_ELEMENT_UNITS = 1;

	/**
	 * The number of units representing a single Class Count. This unit represents (c) in the 4
	 * rules used to calculate com.crawljax.plugins.cilla.util.specificity.
	 */
	private final int CLASSES_ATTRIBUTES_AND_PSEUDO_CLASS_UNITS =
	        ELEMENT_NAMES_AND_PSEUDO_ELEMENT_UNITS * BASE;

	/**
	 * The number of units representing a single ID Count. This unit represents (b) in the 4 rules
	 * used to calculate com.crawljax.plugins.cilla.util.specificity.
	 */
	private final int ID_ATTRIBUTE_UNITS = CLASSES_ATTRIBUTES_AND_PSEUDO_CLASS_UNITS * BASE;

	/**
	 * applies to element types and pseudo-elements
	 */
	private void AddElementSelector() {
		value += ELEMENT_NAMES_AND_PSEUDO_ELEMENT_UNITS;
	}


	/**
	 * applies to classes, attributes and pseudo-classes
	 */
	private void AddClassSelector() {
		value += CLASSES_ATTRIBUTES_AND_PSEUDO_CLASS_UNITS;
	}

	/**
	 * applies to IDs only
	 */
	private void AddIDSelector() {
		value += ID_ATTRIBUTE_UNITS;
	}


	/**
	 * Recursively parse all attributes in given part
	 * @param part
	 */
	private void RecursiveParseAttribute(String part)
	{
		//first split on first closing brace
		String[] temp = part.split("\\]", 2);

		if(temp.length > 1 && !temp[1].isEmpty())
		{
			RecursiveParseAttribute(temp[0] + "]");
			RecursiveParseAttribute(temp[1]);
		}
		else
		{
			temp = part.split("\\[");
			if(temp.length > 1 && !temp[0].isEmpty())
			{
				this.AddElementSelector();
			}
			this.AddClassSelector();
		}
	}


	/**
	 * Ignore any part (by splitting on white-space) that is a combinator
	 * @param part
	 * @return
	 */
	private static boolean IgnorePart(String part)
	{
		return part.isEmpty() || part.equals("+") || part.equals(">") || part.equals("~");
	}

	public Specificity ComputeSpecificity(String selector, int pseudoClassCount, boolean isPseudoElement)
	{
		for(int i = 0; i < pseudoClassCount; i++)
		{
			this.AddClassSelector();
		}

		if(isPseudoElement)
		{
			this.AddElementSelector();
		}

		// replace every :not with a whitespace char,
		// in order to analyze the contents of the :not
		if(selector.contains(":not"))
		{
			selector = selector.replaceAll(":not\\(", " ");
			selector = selector.replaceAll("\\)", "");
		}

		// get every selector in the sequence
		// we can split on whitespace, because we know the cssparser library
		// automatically spaces other combinators, like +, ~ and >
		// we check for those in the IgnorePart() call below
		String[] parts = selector.split(" ");

		for (String part : parts)
		{
			if(IgnorePart(part))
				continue;

			if (part.contains(".")) {
				String[] temp = part.split("\\.");

				if(!temp[0].isEmpty())
				{
					this.AddElementSelector();
				}

				for(int i = 0; i < temp.length - 1; i++)
				{
					this.AddClassSelector();
				}
			}
			else if (part.contains("#"))
			{
				String[] temp = part.split("\\#");
				if (temp.length > 1 && !temp[0].isEmpty())
				{
					this.AddElementSelector();
				}

				this.AddIDSelector();
			}
			else if (part.contains("["))
			{
				RecursiveParseAttribute(part);
			}
			else
			{
				this.AddElementSelector();
			}
		}

		return new Specificity(value);
	}
}