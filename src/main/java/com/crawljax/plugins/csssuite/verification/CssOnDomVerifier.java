package com.crawljax.plugins.csssuite.verification;

import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.colors.BrowserColorParser;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.plugins.DetectClonedDeclarationsPlugin;
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
    private Map<MSelector, String> _selFileMapOrig = new HashMap<>();
    private Map<MSelector, String> _selFileMapGnr = new HashMap<>();
    private Map<MDeclaration, MSelector> _declSelMapOrig = new HashMap<>();
    private Map<MDeclaration, MSelector> _declSelMapGnr = new HashMap<>();

    private Set<String> _matchedElementsOrig = new HashSet<>();
    private Set<String> _matchedAndEffectiveOrig = new HashSet<>();

    private Set<String> _matchedElementsGnr = new HashSet<>();
    private Set<String> _equallyMatchedElems = new HashSet<>();
    private Set<String> _additionalMatchedElems = new HashSet<>();
    private Set<String> _missedMatchedElements = new HashSet<>();

    private Set<MDeclaration> _totalEffectiveDeclsOrig = new HashSet<>();
    private Set<MDeclaration> _totalEquallyEffectiveDecls = new HashSet<>();
    private Map<MDeclaration, MDeclaration> _totalEffectiveByName = new HashMap<>();
    private Set<MDeclaration> _totalDefaultDeclsOrig = new HashSet<>();
    private Set<MDeclaration> _totalMissingDecls = new HashSet<>();
    private Set<MDeclaration> _totalAdditionalDecls = new HashSet<>();

    private final Map<String, String> _defaultStyles = DefaultStylesHelper.CreateDefaultStyles();

    private Map<MSelector, String> GenerateSelectorFileMap(Map<String, MCssFile> mcssFiles)
    {
        Map<MSelector, String> result = new HashMap<>();

        for(String fileName : mcssFiles.keySet())
        {
            for(MCssRule mCssRule : mcssFiles.get(fileName).getRules())
            {
                for(MSelector mSelector : mCssRule.getSelectors())
                {
                    result.put(mSelector, fileName);
                }
            }
        }

        return result;
    }


    private Map<MDeclaration, MSelector> GenerateDeclarationSelectorMap(List<MSelector> selectors)
    {
        Map<MDeclaration, MSelector> result = new HashMap<>();

        selectors.forEach((s) -> {
            List<MDeclaration> mDeclarations = s.getDeclarations().stream().filter(p -> p.isIgnored() || p.isEffective()).collect(Collectors.toList());
            mDeclarations.forEach(p -> result.put(p, s));
        });

        return result;
    }


    private List<MDeclaration> FindEffectiveDeclarationsForElement(List<MSelector> selectors)
    {
        // first reset all previously deemed effective declarations to non-effective
        selectors.forEach(s -> s.getDeclarations().forEach(p -> p.setEffective(false)));

        String overridden = "overridden-" + new Random().nextInt();

        EffectivenessAnalysis.ComputeEffectiveness(selectors, overridden);

        List<MDeclaration> effectiveProps = new ArrayList<>();
        selectors.forEach(s -> effectiveProps.addAll(s.getDeclarations().stream().filter(p -> p.isEffective() || p.isIgnored()).collect(Collectors.toList())));
        return effectiveProps;
    }

    public void Verify(Map<StateVertex, LinkedHashMap<String, Integer>> states, Map<String, MCssFile> originalStyles, Map<String, MCssFile> generatedStyles) throws IOException
    {
        _selFileMapOrig = GenerateSelectorFileMap(originalStyles);
        _selFileMapGnr = GenerateSelectorFileMap(generatedStyles);

        MatchedElements matchedElementsOrig = new MatchedElements();
        MatchedElements matchedElementsGnr = new MatchedElements();

        NormalizeAndSplitPlugin normalizer = new NormalizeAndSplitPlugin();
        originalStyles = normalizer.transform(originalStyles, matchedElementsOrig);
        generatedStyles = normalizer.transform(generatedStyles, matchedElementsGnr);

        DetectClonedDeclarationsPlugin clonedProps = new DetectClonedDeclarationsPlugin();
        originalStyles = clonedProps.transform(originalStyles, matchedElementsOrig);
        generatedStyles = clonedProps.transform(generatedStyles, matchedElementsGnr);

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
            LogHandler.debug("[VERIFICATION] Find out if original matches are effective (i.e. at least 1 effective declaration) for element %d of %d...", count, total);

            List<MSelector> selectorsOrig = matchedElementsOrig.SortSelectorsForMatchedElem(matchedElement);
            List<MDeclaration> effectivePropsOrig = FindEffectiveDeclarationsForElement(selectorsOrig);

            if(ContainsEffectiveDecls(effectivePropsOrig))
            {
                _totalEffectiveDeclsOrig.addAll(effectivePropsOrig);
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
            _declSelMapOrig.putAll(GenerateDeclarationSelectorMap(selectorsOrig));

            List<MDeclaration> effectivePropsOrig = FindEffectiveDeclarationsForElement(selectorsOrig);

            // only continue when both styles matched the same element
            if(!_equallyMatchedElems.contains(matchedElement))
            {
                continue;
            }

            List<MSelector> selectorsGnr = matchedElementsGnr.SortSelectorsForMatchedElem(matchedElement);
            _declSelMapGnr.putAll(GenerateDeclarationSelectorMap(selectorsGnr));

            List<MDeclaration> effectivePropsGnr = FindEffectiveDeclarationsForElement(selectorsGnr);

            effectivePropsOrig.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
            effectivePropsGnr.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));

            Set<MDeclaration> matchedPropsOnValueOrig = new HashSet<>();
            Set<MDeclaration> matchedPropsOnValueGnr = new HashSet<>();

            // find all declaration matches by name, value and !important
            for(MDeclaration origDeclaration : effectivePropsOrig)
            {
                final String name = origDeclaration.getName();
                final String value = bcp.tryParseColorToHex(origDeclaration.getValue());

                for(MDeclaration gnrDeclaration : effectivePropsGnr)
                {
                    if(matchedPropsOnValueGnr.contains(gnrDeclaration))
                    {
                        continue;
                    }

                    if(gnrDeclaration.getName().equals(name))
                    {
                        String gnrValue = bcp.tryParseColorToHex(gnrDeclaration.getValue());

                        if(gnrValue.equals(value) && gnrDeclaration.isImportant() == origDeclaration.isImportant())
                        {
                            matchedPropsOnValueOrig.add(origDeclaration);
                            matchedPropsOnValueGnr.add(gnrDeclaration);
                            break;
                        }
                    }
                }
            }



            List<MDeclaration> remainderOrig = effectivePropsOrig.stream().filter((p) -> !matchedPropsOnValueOrig.contains(p)).collect(Collectors.toList());
            List<MDeclaration> remainderGnr = effectivePropsGnr.stream().filter((p) -> !matchedPropsOnValueGnr.contains(p)).collect(Collectors.toList());
            Map<MDeclaration, MDeclaration> matchedOnName = new HashMap<>();
            Set<MDeclaration> alreadyNameMatchedGnr = new HashSet<>();

            // find all declaration matches by name from remainders
            for(MDeclaration origDeclaration : remainderOrig)
            {
                if(matchedOnName.containsKey(origDeclaration))
                {
                    continue;
                }

                for(MDeclaration gnrDeclaration : remainderGnr)
                {
                    if(alreadyNameMatchedGnr.contains(gnrDeclaration))
                    {
                        continue;
                    }

                    if(gnrDeclaration.getName().equals(origDeclaration.getName()))
                    {
                        matchedOnName.put(origDeclaration, gnrDeclaration);
                        alreadyNameMatchedGnr.add(gnrDeclaration);
                        break;
                    }
                }
            }


            //update remainders
            remainderOrig =  remainderOrig.stream().filter((p) -> !matchedOnName.containsKey(p)).collect(Collectors.toList());
            remainderGnr =  remainderGnr.stream().filter((p) -> !alreadyNameMatchedGnr.contains(p)).collect(Collectors.toList());

            for(MDeclaration origDeclaration : matchedOnName.keySet())
            {
                MDeclaration gnrDeclaration = matchedOnName.get(origDeclaration);
                LogHandler.debug("[VERIFICATION] Match by name only: new:'%s', old:'%s'\nnew:'%s', old:'%s'\nnew:'%s', old:'%s'",
                        gnrDeclaration, origDeclaration, _declSelMapGnr.get(gnrDeclaration), _declSelMapOrig.get(origDeclaration),
                        _selFileMapGnr.get(_declSelMapGnr.get(gnrDeclaration)), _selFileMapOrig.get(_declSelMapOrig.get(origDeclaration)));
            }

            for(MDeclaration remainingDeclaration : remainderOrig)
            {
                // verify that remaing declaration from original styles is not a default style, then it is a valid mismatch
                if(_defaultStyles.containsKey(remainingDeclaration.getName()))
                {
                    if(remainingDeclaration.getValue().equals(_defaultStyles.get(remainingDeclaration.getName())))
                    {
                        _totalDefaultDeclsOrig.add(remainingDeclaration);
                        continue;
                    }
                }

                _totalMissingDecls.add(remainingDeclaration);
                LogHandler.debug("[VERIFICATION] Missing declaration '%s' in selector '%s' in file '%s'",
                        remainingDeclaration, _declSelMapOrig.get(remainingDeclaration), _selFileMapOrig.get(_declSelMapOrig.get(remainingDeclaration)));
            }

            for(MDeclaration remainingDeclaration : remainderGnr)
            {
                _totalAdditionalDecls.add(remainingDeclaration);
                LogHandler.debug("[VERIFICATION] Additional declaration '%s' in selector '%s' in file '%s'",
                        remainingDeclaration, _declSelMapGnr.get(remainingDeclaration), _selFileMapGnr.get(_declSelMapGnr.get(remainingDeclaration)));
            }

            _totalEquallyEffectiveDecls.addAll(matchedPropsOnValueGnr);
            _totalEffectiveByName.putAll(matchedOnName);
        }


        // all unmatched elements
        for(String unmatchedElement : _missedMatchedElements)
        {
            List<MSelector> selectors = matchedElementsOrig.SortSelectorsForMatchedElem(unmatchedElement);

            String selectorsText = "";
            for(MSelector mSelector : selectors)
            {
                selectorsText += String.format("%s in file: %s\n", mSelector, _selFileMapOrig.get(mSelector));
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
                selectorsText += String.format("%s in file: %s\n", mSelector, _selFileMapGnr.get(mSelector));
            }

            LogHandler.info("[VERIFICATION] Additionally matched element detected: %s\nwith selectors from generated stylesheets", selectorsText);
        }


        // finalize collections

        // filter effective props by name with missing props
        // missing props have higher precedence than effective by name
        for(MDeclaration mDeclaration : _totalMissingDecls)
        {
            if(_totalEffectiveByName.containsKey(mDeclaration))
            {
                _totalEffectiveByName.remove(mDeclaration);
            }
        }

        // filter missing props and effective by name props from effective by value props
        // they have higher precedence
        _totalEquallyEffectiveDecls = Sets.difference(_totalEquallyEffectiveDecls, _totalEffectiveByName.keySet());
        _totalEquallyEffectiveDecls = Sets.difference(_totalEquallyEffectiveDecls, _totalMissingDecls);

        LogResults();
    }

    private void LogResults()
    {
        LogHandler.info("[VERIFICATION] %d elements matched originally, %d elements matches effective originally, %d elements equally matched by new,  %d elements unmatched, %d additional elements matched",
                _equallyMatchedElems.size(), _matchedAndEffectiveOrig.size(), _equallyMatchedElems.size(), _missedMatchedElements.size(), _additionalMatchedElems.size());
        LogHandler.info("[VERIFICATION] %d effective props originally, %d effective props by compare, %d effective props by name, %d missing props but default style, %d missing props, %d additional props",
                _totalEffectiveDeclsOrig.size(), _totalEquallyEffectiveDecls.size(), _totalEffectiveByName.size(), _totalDefaultDeclsOrig.size(), _totalMissingDecls.size(), _totalAdditionalDecls.size());

        for(MDeclaration orig : _totalEffectiveByName.keySet())
        {
            MDeclaration gnr = _totalEffectiveByName.get(orig);
            LogHandler.info("[VERIFICATION] Matched by name: '%s'\nwith '%s'", PrintOrigDeclaration(orig), PrintGnrDeclaration(gnr));
        }
        for(MDeclaration orig : _totalMissingDecls)
        {
            LogHandler.info("[VERIFICATION] Missing declaration: '%s'", PrintOrigDeclaration(orig));
        }
        for(MDeclaration gnr : _totalAdditionalDecls)
        {
            LogHandler.info("[VERIFICATION] Additional declaration: '%s'", PrintGnrDeclaration(gnr));
        }
    }

    private String PrintOrigDeclaration(MDeclaration declaration)
    {
        return String.format("%s %s %s", declaration, _declSelMapOrig.get(declaration), _selFileMapOrig.get(_declSelMapOrig.get(declaration)));
    }

    private String PrintGnrDeclaration(MDeclaration declaration)
    {
        return String.format("%s %s %s", declaration, _declSelMapGnr.get(declaration), _selFileMapGnr.get(_declSelMapGnr.get(declaration)));
    }

    public void GenerateXml(SuiteStringBuilder builder, String prefix)
    {
        builder.append("%s<matched_elements_orig>%d</matched_elements_orig>", prefix, _matchedElementsOrig.size());
        builder.appendLine("%s<matched_effective_orig>%d</matched_effective_orig>", prefix, _matchedAndEffectiveOrig.size());
        builder.appendLine("%s<equally_matched_elements>%d</equally_matched_elements>", prefix, _equallyMatchedElems.size());
        builder.appendLine("%s<missed_matched_elements>%d</missed_matched_elements>", prefix, _missedMatchedElements.size());
        builder.appendLine("%s<additional_matched_elements>%d</additional_matched_elements>", prefix, _additionalMatchedElems.size());

        builder.appendLine("%s<effective_props_orig>%d</effective_props_orig>", prefix, _totalEffectiveDeclsOrig.size());
        builder.appendLine("%s<equally_effective_props>%d</equally_effective_props>", prefix, _totalEquallyEffectiveDecls.size());
        builder.appendLine("%s<effective_by_name_props>%d</effective_by_name_props>", prefix, _totalEffectiveByName.size());
        builder.appendLine("%s<missing_but_default_props>%d</missing_but_default_props>", prefix, _totalDefaultDeclsOrig.size());
        builder.appendLine("%s<missing_props>%d</missing_props>", prefix, _totalMissingDecls.size());
        builder.appendLine("%s<additional_props>%d</additional_props>", prefix, _totalAdditionalDecls.size());
    }

    public boolean ContainsEffectiveDecls(List<MDeclaration> declarations)
    {
        if(declarations.isEmpty())
        {
            return false;
        }

        return !declarations.stream().allMatch(p -> _defaultStyles.containsKey(p.getName()) && _defaultStyles.get(p.getName()).equals(p.getValue()));
    }
}
