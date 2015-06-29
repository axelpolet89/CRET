package com.crawljax.plugins.csssuite.verification;

import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.colors.BrowserColorParser;
import com.crawljax.plugins.csssuite.data.ElementWrapper;
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
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/22/2015.
 */
public class CssOnDomVerifier
{
    private Map<MSelector, String> selFileMapOrig = new HashMap<>();
    private Map<MSelector, String> selFileMapGnr = new HashMap<>();

    private Set<String> _matchedElementsOrig = new HashSet<>();
    private Set<String> _matchedAndEffectiveOrig = new HashSet<>();

    private Set<String> _matchedElementsGnr = new HashSet<>();
    private Set<String> _equallyMatchedElems = new HashSet<>();
    private Set<String> _additionalMatchedElems = new HashSet<>();
    private Set<String> _missedMatchedElements = new HashSet<>();

    private Set<MProperty> _totalEffectivePropsOrig = new HashSet<>();
    private Set<MProperty> _totalEquallyEffectiveProps = new HashSet<>();
    private Map<MProperty, MProperty> _totalEffectiveByName = new HashMap<>();
    private Set<MProperty> _totalDefaultPropsOrig = new HashSet<>();
    private Set<MProperty> _totalMissingProps = new HashSet<>();
    private Set<MProperty> _totalAdditionalProps = new HashSet<>();

    private final Map<String, String> _defaultStyles = DefaultStylesHelper.CreateDefaultStyles();

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

    public void Verify(Map<StateVertex, LinkedHashMap<String, Integer>> states, Map<String, MCssFile> originalStyles, Map<String, MCssFile> generatedStyles) throws IOException
    {
        Map<MSelector, String> selFileMapOrig = GenerateSelectorFileMap(originalStyles);
        Map<MSelector, String> selFileMapGnr = GenerateSelectorFileMap(generatedStyles);


        MatchedElements matchedElementsOrig = new MatchedElements();
        MatchedElements matchedElementsGnr = new MatchedElements();

        NormalizeAndSplitPlugin normalizer = new NormalizeAndSplitPlugin();
        originalStyles = normalizer.Transform(originalStyles, matchedElementsOrig);
        generatedStyles = normalizer.Transform(generatedStyles, matchedElementsGnr);

        DetectClonedPropertiesPlugin clonedProps = new DetectClonedPropertiesPlugin();
        originalStyles = clonedProps.Transform(originalStyles, matchedElementsOrig);
        generatedStyles = clonedProps.Transform(generatedStyles, matchedElementsGnr);

        // perform matched element analysis
        for(StateVertex state : states.keySet())
        {
            LogHandler.info("[VERIFICATION] Match selectors from original and generated styles to DOM for state %s...", state.getUrl());
            LinkedHashMap<String, Integer> stateFileOrder = states.get(state);

            ElementSelectorMatcher.MatchElementsToDocument(state.getName(), state.getDocument(), originalStyles, stateFileOrder, matchedElementsOrig);
            ElementSelectorMatcher.MatchElementsToDocument(state.getName(), state.getDocument(), generatedStyles, stateFileOrder, matchedElementsGnr);
        }

        _matchedElementsOrig = matchedElementsOrig.GetMatchedElements();
        _matchedElementsGnr = matchedElementsGnr.GetMatchedElements();

        // effectiveness analysis
        LogHandler.info("[VERIFICATION] Start effectiveness analysis for matched elements of original and generated styles...");

        int count = 0;
        int total = _matchedElementsOrig.size();
        BrowserColorParser bcp = new BrowserColorParser();

        for(String matchedElement : _matchedElementsOrig)
        {
            count++;
            LogHandler.debug("[VERIFICATION] Find out if original matches are effective (i.e. at least 1 effective property) for element %d of %d...", count, total);

            List<MSelector> selectorsOrig = matchedElementsOrig.SortSelectorsForMatchedElem(matchedElement);
            List<MProperty> effectivePropsOrig = FindEffectivePropertiesForElement(selectorsOrig);

            if(ContainsEffectiveProps(effectivePropsOrig))
            {
                _totalEffectivePropsOrig.addAll(effectivePropsOrig);
                _matchedAndEffectiveOrig.add(matchedElement);
            }
        }


        _additionalMatchedElems = Sets.difference(_matchedElementsGnr, _matchedAndEffectiveOrig);
        _missedMatchedElements = Sets.difference(_matchedAndEffectiveOrig, _matchedElementsGnr);
        _equallyMatchedElems = Sets.difference(_matchedAndEffectiveOrig, _missedMatchedElements);

        count = 0;
        total = _matchedAndEffectiveOrig.size();

        for(String matchedElement : _matchedAndEffectiveOrig)
        {
            count++;
            LogHandler.debug("[VERIFICATION] Start effectiveness analysis and comparison for element %d of %d...", count, total);

            List<MSelector> selectorsOrig = matchedElementsOrig.SortSelectorsForMatchedElem(matchedElement);
            Map<MProperty, MSelector> propSelMapOrig = GeneratePropertySelectorMap(selectorsOrig);

            List<MProperty> effectivePropsOrig = FindEffectivePropertiesForElement(selectorsOrig);

            // only continue when both styles matched the same element
            if(!_equallyMatchedElems.contains(matchedElement))
            {
                continue;
            }

            List<MSelector> selectorsGnr = matchedElementsGnr.SortSelectorsForMatchedElem(matchedElement);
            Map<MProperty, MSelector> propSelMapGnr = GeneratePropertySelectorMap(selectorsGnr);

            List<MProperty> effectivePropsGnr = FindEffectivePropertiesForElement(selectorsGnr);


            effectivePropsOrig.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));
            effectivePropsGnr.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));

            Set<MProperty> matchedPropsOnValueOrig = new HashSet<>();
            Set<MProperty> matchedPropsOnValueGnr = new HashSet<>();

            // find all property matches by name, value and !important
            for(MProperty origProperty : effectivePropsOrig)
            {
                final String name = origProperty.GetName();
                final String value = bcp.TryParseColorToHex(origProperty.GetValue());

                for(MProperty gnrProperty : effectivePropsGnr)
                {
                    if(matchedPropsOnValueGnr.contains(gnrProperty))
                    {
                        continue;
                    }

                    if(gnrProperty.GetName().equals(name))
                    {
                        String gnrValue = bcp.TryParseColorToHex(gnrProperty.GetValue());

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
                if(_defaultStyles.containsKey(remainingProperty.GetName()))
                {
                    if(remainingProperty.GetValue().equals(_defaultStyles.get(remainingProperty.GetName())))
                    {
                        _totalDefaultPropsOrig.add(remainingProperty);
                        continue;
                    }
                }

                _totalMissingProps.add(remainingProperty);
                LogHandler.debug("[VERIFICATION] Missing property '%s' in selector '%s' in file '%s'",
                        remainingProperty, propSelMapOrig.get(remainingProperty), selFileMapOrig.get(propSelMapOrig.get(remainingProperty)));
            }

            for(MProperty remainingProperty : remainderGnr)
            {
                _totalAdditionalProps.add(remainingProperty);
                LogHandler.debug("[VERIFICATION] Additional property '%s' in selector '%s' in file '%s'",
                        remainingProperty, propSelMapGnr.get(remainingProperty), selFileMapGnr.get(propSelMapGnr.get(remainingProperty)));
            }

            _totalEquallyEffectiveProps.addAll(matchedPropsOnValueGnr);
            _totalEffectiveByName.putAll(matchedOnName);
        }


        // all unmatched elements
        for(String unmatchedElement : _missedMatchedElements)
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
        for(String extraElement : _additionalMatchedElems)
        {
            List<MSelector> selectors = matchedElementsGnr.SortSelectorsForMatchedElem(extraElement);

            String selectorsText = "";
            for(MSelector mSelector : selectors)
            {
                selectorsText += String.format("%s in file: %s\n", mSelector, selFileMapGnr.get(mSelector));
            }

            LogHandler.info("[VERIFICATION] Additionally matched element detected: %s\nwith selectors from generated stylesheets", selectorsText);
        }


        // finalize collections

        // filter effective props by name with missing props
        // missing props have higher precedence than effective by name
        for(MProperty mProperty : _totalMissingProps)
        {
            if(_totalEffectiveByName.containsKey(mProperty))
            {
                _totalEffectiveByName.remove(mProperty);
            }
        }

        // filter missing props and effective by name props from effective by value props
        // they have higher precedence
        _totalEquallyEffectiveProps = Sets.difference(_totalEquallyEffectiveProps, _totalEffectiveByName.keySet());
        _totalEquallyEffectiveProps = Sets.difference(_totalEquallyEffectiveProps, _totalMissingProps);

        LogHandler.info("[VERIFICATION] %d elements matched originally, %d elements matches effective originally, %d elements unmatched, %d additional elements matched",
                _equallyMatchedElems.size(), _matchedAndEffectiveOrig.size(), _missedMatchedElements.size(), _additionalMatchedElems.size());
        LogHandler.info("[VERIFICATION] %d effective props originally, %d effective props by compare, %d effective props by name, %d missing props but default style, %d missing props, %d additional props",
                _totalEffectivePropsOrig.size(), _totalEquallyEffectiveProps.size(), _totalEffectiveByName.size(), _totalDefaultPropsOrig.size(), _totalMissingProps.size(), _totalAdditionalProps.size());
    }

    public void GenerateXml(SuiteStringBuilder builder, String prefix)
    {
        builder.append("%s<matched_elements_orig>%d</matched_elements_orig>", prefix, _matchedElementsOrig.size());
        builder.appendLine("%s<matched_effective_orig>%d</matched_effective_orig>", prefix, _matchedAndEffectiveOrig.size());
        builder.appendLine("%s<equally_matched_elements>%d</equally_matched_elements>", prefix, _equallyMatchedElems.size());
        builder.appendLine("%s<missed_matched_elements>%d</missed_matched_elements>", prefix, _missedMatchedElements.size());
        builder.appendLine("%s<additional_matched_elements>%d</additional_matched_elements>", prefix, _additionalMatchedElems.size());

        builder.appendLine("%s<effective_props_orig>%d</effective_props_orig>", prefix, _totalEffectivePropsOrig.size());
        builder.appendLine("%s<equally_effective_props>%d</equally_effective_props>", prefix, _totalEquallyEffectiveProps.size());
        builder.appendLine("%s<effective_by_name_props>%d</effective_by_name_props>", prefix, _totalEffectiveByName.size());
        builder.appendLine("%s<missing_but_default_props>%d</missing_but_default_props>", prefix, _totalDefaultPropsOrig.size());
        builder.appendLine("%s<missing_props>%d</missing_props>", prefix, _totalMissingProps.size());
        builder.appendLine("%s<additional_props>%d</additional_props>", prefix,_totalAdditionalProps.size());
    }

    public String GenerateXml()
    {
        SuiteStringBuilder builder = new SuiteStringBuilder();
        GenerateXml(builder, "");
        return builder.toString();
    }

    public boolean ContainsEffectiveProps(List<MProperty> properties)
    {
        if(properties.isEmpty())
        {
            return false;
        }

        return !properties.stream().allMatch(p -> _defaultStyles.containsKey(p.GetName()) && _defaultStyles.get(p.GetName()).equals(p.GetValue()));
    }
}
