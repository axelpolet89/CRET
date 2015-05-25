package com.crawljax.plugins.csssuite.util.specificity;

import com.crawljax.plugins.csssuite.data.MSelector;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Created by axel on 5/19/2015.
 */
public class SpecificityHelper
{
    public static void SortBySpecificity(List<MSelector> selectors)
    {
        Collections.sort(selectors, (s1, s2) ->
        {
            MSelector m1 = s1;//.getLeft();
            MSelector m2 = s2;//.getLeft();

            int value1 = m1.GetSpecificity().GetValue();
            int value2 = m2.GetSpecificity().GetValue();

            //if two selectors have the same _specificity,
            //then the one that is defined later (e.g. a higher row number in the css file)
            //has a higher order, so we return the highest rule to be placed first
            if (value1 == value2)
            {
                return new Integer(m2.GetRuleNumber()).compareTo(m1.GetRuleNumber()); //  -1 if m1 higher, 1 if m2 higher
            }

            return new Integer(value2).compareTo(value1);
        });

//        Collections.sort(selectors, (s1, s2) ->
//        {
//            int value1 = s1.getRight();
//            int value2 = s2.getRight();
//
//            if (value1 == value2)
//            {
//                return 0; //do not alter order
//            }
//
//            return new Integer(value2).compareTo(value1);
//        });
    }
}