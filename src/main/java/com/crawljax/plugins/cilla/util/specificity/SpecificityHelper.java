package com.crawljax.plugins.cilla.util.specificity;

import com.crawljax.plugins.cilla.data.MSelector;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by axel on 5/19/2015.
 */
public class SpecificityHelper
{
    public static void OrderSpecificity(List<MSelector> selectors)
    {
        Collections.sort(selectors, new Comparator<MSelector>()
        {
            public int compare(MSelector m1, MSelector m2)
            {
                int value1 = m1.getSpecificity().GetValue();
                int value2 = m2.getSpecificity().GetValue();

                //if two selectors have the same _specificity,
                //then the one that is defined later (e.g. a higher row number in the css file)
                //has a higher order
                if (value1 == value2)
                {
                    return new Integer(m1.GetRuleNumber()).compareTo(m2.GetRuleNumber()); //  1 if m1 higher, -1 if m2 higher
                }

                return new Integer(value1).compareTo(value2);
            }

        });

        //we need selectors sorted ascending (from specific to less-specific)
        Collections.reverse(selectors);
    }
}