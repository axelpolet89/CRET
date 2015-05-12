package com.crawljax.plugins.cilla.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by axel on 5/12/2015.
 */
public class Constants {
    private static final ArrayList<String> nonStructuralPseudoClasses = new ArrayList<>(Arrays.asList(":link",
            ":visited", ":hover", ":focus", ":active", ":target", ":lang", ":enabled",
            ":disabled", ":checked", ":indeterminate"));

    private static final ArrayList<String> pseudoElements = new ArrayList<>(Arrays.asList(":before", ":after", ":first-line", ":first-letter"));

    public static ArrayList<String> NonStructuralPseudos(){
        return nonStructuralPseudoClasses;
    }

    public static ArrayList<String> PseudoElements(){
        return pseudoElements;
    }
}
