package com.crawljax.plugins.csssuite.util.specificity;

import java.util.Collections;
import java.util.List;

/**
 * Created by axel on 5/19/2015.
 */
public class SpecificityHelper
{
    /**
     * Sort a list of MSelectors wrapped in SpecificitySelectors by their specificity, rulenumber and fileorder
     * @param selectors
     */
    public static void SortBySpecificity(List<SpecificitySelector> selectors)
    {
        Collections.sort(selectors, (s1, s2) ->
        {
            int value1 = s1.GetSpecificity();
            int value2 = s2.GetSpecificity();

            //if two selectors have the same _specificity, we need to verify the position in the file or the position of the file in the DOM document
            if (value1 == value2)
            {
                int fileOrder1 = s1.GetOrder();
                int fileOrder2 = s2.GetOrder();

                // if both selectors occur in same file, we simply check which selector has a higher rule number (placed lower in file)
                if(fileOrder1 == fileOrder2)
                {
                    return new Integer(s2.GetRuleNumber()).compareTo(s1.GetRuleNumber()); //-1 if m1 higher, 1 if m2 higher
                }
                else // otherwise we check which file was included later-on in the DOM (embedded/internal styles will always have a higher order than external styles_
                {
                    return new Integer(fileOrder2).compareTo(fileOrder1);
                }
            }

            return new Integer(value2).compareTo(value1);
        });
    }
}