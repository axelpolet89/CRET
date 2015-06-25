package com.crawljax.plugins.csssuite.verification;

import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.colors.BrowserColorParser;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.plugins.DetectClonedPropertiesPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.EffectivenessAnalysis;
import com.crawljax.plugins.csssuite.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.ElementSelectorMatcher;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.util.DefaultStylesHelper;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/22/2015.
 */
public class CssOnDomVerifier
{
    public void Verify(Map<StateVertex, LinkedHashMap<String, Integer>> states, Map<String, MCssFile> originalStyles, Map<String, MCssFile> generatedStyles) throws IOException
    {
        Map<MSelector, String> selFileMapOrig = GenerateSelectorFileMap(originalStyles);
        Map<MSelector, String> selFileMapGnr = GenerateSelectorFileMap(generatedStyles);

        Map<String, String> defaultStyles = DefaultStylesHelper.CreateDefaultStyles();
        MatchedElements matchedElementsOrig = new MatchedElements();
        MatchedElements matchedElementsGnr = new MatchedElements();

        NormalizeAndSplitPlugin normalizer = new NormalizeAndSplitPlugin();
        originalStyles = normalizer.Transform(originalStyles, matchedElementsOrig);
        generatedStyles = normalizer.Transform(generatedStyles, matchedElementsGnr);

        DetectClonedPropertiesPlugin clonedProps = new DetectClonedPropertiesPlugin();
        originalStyles = clonedProps.Transform(originalStyles, matchedElementsOrig);
        generatedStyles = clonedProps.Transform(generatedStyles, matchedElementsGnr);

        for(StateVertex state : states.keySet())
        {
            LogHandler.info("[VERIFICATION] Match selectors from original and generated styles to DOM for state %s...", state.getUrl());
            LinkedHashMap<String, Integer> stateFileOrder = states.get(state);

            ElementSelectorMatcher.MatchElementsToDocument(state.getName(), state.getDocument(), originalStyles, stateFileOrder, matchedElementsOrig);
            ElementSelectorMatcher.MatchElementsToDocument(state.getName(), state.getDocument(), generatedStyles, stateFileOrder, matchedElementsGnr);
        }

        Set<String> matchesOrig = matchedElementsOrig.GetMatchedElements();
        Set<String> matchesGnr = matchedElementsGnr.GetMatchedElements();
        Set<String> additionalMatchedElems = Sets.difference(matchesGnr, matchesOrig);
        Set<String> unmatchedElems = Sets.difference(matchesOrig, matchesGnr);
        Set<String> equallyMatchedElems = Sets.difference(matchesOrig, unmatchedElems);


        // all unmatched elements
        for(String unmatchedElement : unmatchedElems)
        {
            List<MSelector> selectors = matchedElementsOrig.SortSelectorsForMatchedElem(unmatchedElement);

            String selectorsText = "";
            for(MSelector mSelector : selectors)
            {
                selectorsText += String.format("%s in file: %s\n", mSelector, selFileMapOrig.get(mSelector));
            }

            LogHandler.info("[VERIFICATION] Unmatched element detected: '%s'\nwith selectors from original stylesheets: %s", unmatchedElement, selectorsText);
        }


        // all additionally matched elements
        for(String extraElement : additionalMatchedElems)
        {
            List<MSelector> selectors = matchedElementsGnr.SortSelectorsForMatchedElem(extraElement);

            String selectorsText = "";
            for(MSelector mSelector : selectors)
            {
                selectorsText += String.format("%s in file: %s\n", mSelector, selFileMapGnr.get(mSelector));
            }

            LogHandler.info("[VERIFICATION] Additionally matched element detected: %s\nwith selectors from generated stylesheets", selectorsText);
        }


        // effectiveness analysis
        LogHandler.info("[VERIFICATION] Start effectiveness analysis for matched elements of original and generated styles...");

        Set<MProperty> totalEffectivePropsOrig = new HashSet<>();
        Set<MProperty> totalEffectivePropsCompare = new HashSet<>();
        Map<MProperty, MProperty> totalMatchedByName = new HashMap<>();
        Set<MProperty> totalDefaultPropsOrig = new HashSet<>();
        Set<MProperty> totalMissingProps = new HashSet<>();
        Set<MProperty> totalAdditionalProps = new HashSet<>();

        BrowserColorParser bcp = new BrowserColorParser();

        int count = 0;
        int total = equallyMatchedElems.size();

        for(String matchedElement : matchesOrig)
        {
            count++;
            LogHandler.debug("[VERIFICATION] Start effectiveness analysis for element %d of %d...", count, total);

            List<MSelector> selectorsOrig = matchedElementsOrig.SortSelectorsForMatchedElem(matchedElement);
            List<MProperty> effectivePropsOrig = FindEffectivePropertiesForElement(selectorsOrig);
            totalEffectivePropsOrig.addAll(effectivePropsOrig);

            // only continue when both styles matched the same element
            if(!equallyMatchedElems.contains(matchedElement))
            {
                continue;
            }

            List<MSelector> selectorsGnr = matchedElementsGnr.SortSelectorsForMatchedElem(matchedElement);
            List<MProperty> effectivePropsGnr = FindEffectivePropertiesForElement(selectorsGnr);

            Map<MProperty, MSelector> propSelMapOrig = GeneratePropertySelectorMap(selectorsOrig);
            Map<MProperty, MSelector> propSelMapGnr = GeneratePropertySelectorMap(selectorsGnr);

            effectivePropsOrig.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));
            effectivePropsGnr.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));

            Set<MProperty> matchedPropsOnValueOrig = new HashSet<>();
            Set<MProperty> matchedPropsOnValueGnr = new HashSet<>();

            // find all property matches by name, value and !important
            for(MProperty origProperty : effectivePropsOrig)
            {
                final String name = origProperty.GetName();
                final String value = bcp.TryParseToRgb(origProperty.GetValue());

                for(MProperty gnrProperty : effectivePropsGnr)
                {
                    if(matchedPropsOnValueGnr.contains(gnrProperty))
                    {
                        continue;
                    }

                    if(gnrProperty.GetName().equals(name))
                    {
                        String gnrValue = bcp.TryParseToRgb(gnrProperty.GetValue());

                        if(gnrValue.equals("transparent"))
                        {
                            gnrValue = "rgba(0, 0, 0, 0)";
                        }
                        else if (gnrValue.contains("transparent"))
                        {
                            gnrValue = gnrValue.replace("transparent", "rgba(0, 0, 0, 0)");
                        }

                        if(gnrValue.equals(value) && gnrProperty.IsImportant() == origProperty.IsImportant())
                        {
                            matchedPropsOnValueOrig.add(origProperty);
                            matchedPropsOnValueGnr.add(gnrProperty);
                            break;
                        }
                    }
                }
            }



            List<MProperty> remainderOrig = effectivePropsOrig.stream().filter((p) -> !matchedPropsOnValueOrig.contains(p)).collect(Collectors.toList());
            List<MProperty> remainderGnr = effectivePropsGnr.stream().filter((p) -> !matchedPropsOnValueGnr.contains(p)).collect(Collectors.toList());
            Map<MProperty, MProperty> matchedOnName = new HashMap<>();
            Set<MProperty> alreadyNameMatchedGnr = new HashSet<>();

            // find all property matches by name from remainders
            for(MProperty origProperty : remainderOrig)
            {
                if(matchedOnName.containsKey(origProperty))
                {
                    continue;
                }

                for(MProperty gnrProperty : remainderGnr)
                {
                    if(alreadyNameMatchedGnr.contains(gnrProperty))
                    {
                        continue;
                    }

                    if(gnrProperty.GetName().equals(origProperty.GetName()))
                    {
                        matchedOnName.put(origProperty, gnrProperty);
                        alreadyNameMatchedGnr.add(gnrProperty);
                        break;
                    }
                }
            }


            //update remainders
            remainderOrig =  remainderOrig.stream().filter((p) -> !matchedOnName.containsKey(p)).collect(Collectors.toList());
            remainderGnr =  remainderGnr.stream().filter((p) -> !alreadyNameMatchedGnr.contains(p)).collect(Collectors.toList());

            for(MProperty origProperty : matchedOnName.keySet())
            {
                MProperty gnrProperty = matchedOnName.get(origProperty);
                LogHandler.debug("[VERIFICATION] Match by name only: new:'%s', old:'%s'\nnew:'%s', old:'%s'\nnew:'%s', old:'%s'",
                                    gnrProperty, origProperty, propSelMapGnr.get(gnrProperty), propSelMapOrig.get(origProperty),
                                    selFileMapGnr.get(propSelMapGnr.get(gnrProperty)), selFileMapOrig.get(propSelMapOrig.get(origProperty)));
            }

            for(MProperty remainingProperty : remainderOrig)
            {
                // verify that remaing property from original styles is not a default style, then it is a valid mismatch
                if(defaultStyles.containsKey(remainingProperty.GetName()))
                {
                    if(remainingProperty.GetValue().equals(defaultStyles.get(remainingProperty.GetName())))
                    {
                        totalDefaultPropsOrig.add(remainingProperty);
                        continue;
                    }
                }

                totalMissingProps.add(remainingProperty);
                LogHandler.debug("[VERIFICATION] Missing property '%s' in selector '%s' in file '%s'",
                        remainingProperty, propSelMapOrig.get(remainingProperty), selFileMapOrig.get(propSelMapOrig.get(remainingProperty)));
            }

            for(MProperty remainingProperty : remainderGnr)
            {
                totalAdditionalProps.add(remainingProperty);
                LogHandler.debug("[VERIFICATION] Additional property '%s' in selector '%s' in file '%s'",
                        remainingProperty, propSelMapGnr.get(remainingProperty), selFileMapGnr.get(propSelMapGnr.get(remainingProperty)));
            }

            totalEffectivePropsCompare.addAll(matchedPropsOnValueGnr);
            totalMatchedByName.putAll(matchedOnName);
        }


        // finalize collections

        // filter effective props by name with missing props
        // missing props have higher precedence than effective by name
        for(MProperty mProperty : totalMissingProps)
        {
            if(totalMatchedByName.containsKey(mProperty))
            {
                totalMatchedByName.remove(mProperty);
            }
        }

        // filter missing props and effective by name props from effective by value props
        // they have higher precedence
        totalEffectivePropsCompare = Sets.difference(totalEffectivePropsCompare, totalMatchedByName.keySet());
        totalEffectivePropsCompare = Sets.difference(totalEffectivePropsCompare, totalMissingProps);

        LogHandler.debug("[VERIFICATION] %d elements matched, %d elements unmatched, %d additional elements matched", equallyMatchedElems.size(), unmatchedElems.size(), additionalMatchedElems.size());
        LogHandler.debug("[VERIFICATION] %d effective props originally, %d effective props by compare, %d effective props by name, %d missing props but default style, %d missing props, %d additional props",
                totalEffectivePropsOrig.size(), totalEffectivePropsCompare.size(), totalMatchedByName.size(), totalDefaultPropsOrig.size(), totalMissingProps.size(), totalAdditionalProps.size());
    }


    private Map<MSelector, String> GenerateSelectorFileMap(Map<String, MCssFile> mcssFiles)
    {
        Map<MSelector, String> result = new HashMap<>();

        for(String fileName : mcssFiles.keySet())
        {
            for(MCssRule mCssRule : mcssFiles.get(fileName).GetRules())
            {
                for(MSelector mSelector : mCssRule.GetSelectors())
                {
                    result.put(mSelector, fileName);
                }
            }
        }

        return result;
    }


    private Map<MProperty, MSelector> GeneratePropertySelectorMap(List<MSelector> selectors)
    {
        Map<MProperty, MSelector> result = new HashMap<>();

        selectors.forEach((s) -> {
            List<MProperty> mProperties = s.GetProperties().stream().filter(p -> p.IsIgnored() || p.IsEffective()).collect(Collectors.toList());
            mProperties.forEach(p -> result.put(p, s));
        });

        return result;
    }


    private List<MProperty> FindEffectivePropertiesForElement(List<MSelector> selectors)
    {
        // first reset all previously deemed effective properties to non-effective
        selectors.forEach(s -> s.GetProperties().forEach(p -> p.SetEffective(false)));

        String overridden = "overridden-" + new Random().nextInt();

        EffectivenessAnalysis.ComputeEffectiveness(selectors, overridden);

        List<MProperty> effectiveProps = new ArrayList<>();
        selectors.forEach(s -> effectiveProps.addAll(s.GetProperties().stream().filter(p -> p.IsEffective() || p.IsIgnored()).collect(Collectors.toList())));
        return effectiveProps;
    }

    private Set<MProperty> FindUniqueProperties(Set<String> elements, MatchedElements matchedElements)
    {
        Set<MProperty> result = new HashSet<>();

        for(String element : elements)
        {
            List<MSelector> selectors = matchedElements.SortSelectorsForMatchedElem(element);

            for(MSelector mSelector : selectors)
            {
                result.addAll(mSelector.GetProperties());
            }
        }

        return result;
    }
}
