package com.crawljax.plugins.csssuite.verification;

import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.colors.BrowserColorParser;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.plugins.DetectClonedPropertiesPlugin;
import com.crawljax.plugins.csssuite.plugins.EffectivenessPlugin;
import com.crawljax.plugins.csssuite.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchSelectors;
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
        Map<String, String> defaultStyles = DefaultStylesHelper.CreateDefaultStyles();
        MatchedElements matchesOrigs = new MatchedElements();
        MatchedElements matchesGenerated = new MatchedElements();

        NormalizeAndSplitPlugin normalizer = new NormalizeAndSplitPlugin();
        originalStyles = normalizer.Transform(originalStyles, matchesOrigs);
        generatedStyles = normalizer.Transform(generatedStyles, matchesGenerated);

        DetectClonedPropertiesPlugin clonedProps = new DetectClonedPropertiesPlugin();
        originalStyles = clonedProps.Transform(originalStyles, matchesOrigs);
        generatedStyles = clonedProps.Transform(generatedStyles, matchesGenerated);

        // copy required for lambda
        final Map<String, MCssFile> finalOriginal = originalStyles;
        final Map<String, MCssFile> finalGenerated = generatedStyles;

        for(StateVertex state : states.keySet())
        {
            LogHandler.info("[VERIFICATION] Match selectors from original and generated styles to DOM for state %s...", state.getUrl());
            LinkedHashMap<String, Integer> stateFileOrder = states.get(state);

            Map<String, MCssFile> stateFilesOrig = new HashMap<>();
            originalStyles.keySet().stream().filter(f -> stateFileOrder.keySet().contains(f)).forEach(f -> stateFilesOrig.put(f, finalOriginal.get(f)));

            Map<String, MCssFile> stateFilesGnr = new HashMap<>();
            generatedStyles.keySet().stream().filter(f -> stateFileOrder.keySet().contains(f)).forEach(f -> stateFilesGnr.put(f, finalGenerated.get(f)));

            MatchSelectors.MatchElementsToDocument(state.getName(), state.getDocument(), stateFilesOrig, states.get(state), matchesOrigs);
            MatchSelectors.MatchElementsToDocument(state.getName(), state.getDocument(), stateFilesGnr, states.get(state), matchesGenerated);
        }

        LogHandler.info("[VERIFICATION] Start effectiveness analysis for matched elements of original and generated styles...");

        EffectivenessPlugin effectiveness = new EffectivenessPlugin();
        effectiveness.Transform(originalStyles, matchesOrigs);
        effectiveness.Transform(generatedStyles, matchesGenerated);

        Set<String> origs = new HashSet<>(matchesOrigs.GetMatchedElements());
        Set<String> diff = new HashSet<>();

        for(String element : matchesGenerated.GetMatchedElements())
        {
            if(!origs.contains(element))
            {
                diff.add(element);
            }
            else
            {
                origs.remove(element);
            }
        }

        Set<String> equallyMatchedElems = Sets.difference(new HashSet<>(matchesGenerated.GetMatchedElements()), diff);

        BrowserColorParser bcp = new BrowserColorParser();

        for(String matchedElement : equallyMatchedElems)
        {
            List<MSelector> origSelectors = matchesOrigs.SortSelectorsForMatchedElem(matchedElement);
            List<MSelector> genSelectors = matchesGenerated.SortSelectorsForMatchedElem(matchedElement);

            List<MProperty> origProps = new ArrayList<>();
            Map<MProperty, MSelector> origParents = new HashMap<>();
            origSelectors.forEach((s) -> {
                List<MProperty> mProperties = s.GetProperties().stream().filter(p -> p.IsIgnored() || p.IsEffective()).collect(Collectors.toList());
                origProps.addAll(mProperties);
                mProperties.forEach(p -> origParents.put(p, s));
            });

            List<MProperty> gnrProps = new ArrayList<>();
            Map<MProperty, MSelector> gnrParents = new HashMap<>();
            genSelectors.forEach((s) -> {
                List<MProperty> mProperties = s.GetProperties().stream().filter(p -> p.IsIgnored() || p.IsEffective()).collect(Collectors.toList());
                gnrProps.addAll(mProperties);
                mProperties.forEach(p -> gnrParents.put(p, s));
            });

            origProps.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));
            gnrProps.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));

            Set<MProperty> processedOrig = new HashSet<>();
            Set<MProperty> processedGnr = new HashSet<>();

            for(MProperty origProperty : origProps)
            {
                final String name = origProperty.GetName();
                final String value = bcp.TryParseToRgb(origProperty.GetValue());

                for(MProperty gnrProperty : gnrProps)
                {
                    if(processedGnr.contains(gnrProperty))
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
                            processedOrig.add(origProperty);
                            processedGnr.add(gnrProperty);
                            break;
                        }
                    }
                }
            }

            List<MProperty> remainderOrig = origProps.stream().filter((p) -> !processedOrig.contains(p)).collect(Collectors.toList());
            List<MProperty> remainderGnr = gnrProps.stream().filter((p) -> !processedGnr.contains(p)).collect(Collectors.toList());

            for(MProperty remainingProperty : remainderOrig)
            {
                if(defaultStyles.containsKey(remainingProperty.GetName()))
                {
                    if(remainingProperty.GetValue().equals(defaultStyles.get(remainingProperty.GetName())))
                    {
                        continue;
                    }
                }

                LogHandler.debug("[VERIFICATION] Found property '%s  in selector '%s' from the original stylesheet that is not available from the generated stylesheet",
                        remainingProperty, origParents.get(remainingProperty));
            }
        }
    }
}
