package com.crawljax.plugins.csssuite.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by axel on 5/12/2015.
 */
public class PseudoHelper
{
    private static final ArrayList<String> nonStructuralPseudoClasses = new ArrayList<>(Arrays.asList(":link",
            ":visited", ":hover", ":focus", ":active", ":target", ":lang", ":enabled",
            ":disabled", ":checked", ":indeterminate"));

    /**
     * Verify whether the given pseudo string is a structural or non-structural pseudo-class
     * @param pseudo the string containing a pseudo-class
     * @return boolean
     */
    public static boolean IsNonStructuralPseudo(String pseudo)
    {
        if(pseudo.contains(":lang"))
        {
            return true;
        }

        if(nonStructuralPseudoClasses.contains(pseudo))
        {
            return true;
        }

        return false;
    }
}