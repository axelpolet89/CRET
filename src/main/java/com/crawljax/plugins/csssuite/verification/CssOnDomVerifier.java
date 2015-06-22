package com.crawljax.plugins.csssuite.verification;

import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.csssuite.LogHandler;
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
import org.apache.commons.lang3.tuple.Pair;
import sun.rmi.runtime.Log;

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
            LogHandler.info("[VERIFICATION] Performing verification for state %s...", state.getUrl());
            LinkedHashMap<String, Integer> stateFileOrder = states.get(state);

            Map<String, MCssFile> stateFilesOrig = new HashMap<>();
            originalStyles.keySet().stream().filter(f -> stateFileOrder.keySet().contains(f)).forEach(f -> stateFilesOrig.put(f, finalOriginal.get(f)));

            Map<String, MCssFile> stateFilesGnr = new HashMap<>();
            generatedStyles.keySet().stream().filter(f -> stateFileOrder.keySet().contains(f)).forEach(f -> stateFilesGnr.put(f, finalGenerated.get(f)));

            MatchSelectors.MatchElementsToDocument(state.getName(), state.getDocument(), stateFilesOrig, states.get(state), matchesOrigs);
            MatchSelectors.MatchElementsToDocument(state.getName(), state.getDocument(), stateFilesGnr, states.get(state), matchesGenerated);

            EffectivenessPlugin effectiveness = new EffectivenessPlugin();
            Map<String, MCssFile> effectiveOrig = effectiveness.Transform(stateFilesOrig, matchesOrigs);
            Map<String, MCssFile> effectiveGnr = effectiveness.Transform(stateFilesGnr, matchesGenerated);

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

            for(String matchedElement : equallyMatchedElems)
            {
                List<MSelector> origSelectors = matchesOrigs.SortSelectorsForMatchedElem(matchedElement);
                List<MSelector> genSelectors = matchesGenerated.SortSelectorsForMatchedElem(matchedElement);

                List<MProperty> origProps = new ArrayList<>();
                origSelectors.forEach(s -> origProps.addAll(s.GetProperties().stream().filter(p -> p.IsIgnored() || p.IsEffective()).collect(Collectors.toList())));

                List<MProperty> gnrProps = new ArrayList<>();
                genSelectors.forEach(s -> gnrProps.addAll(s.GetProperties().stream().filter(p -> p.IsIgnored() || p.IsEffective()).collect(Collectors.toList())));

                origProps.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));
                gnrProps.sort((p1, p2) -> p1.GetName().compareTo(p2.GetName()));

                Set<MProperty> processedOrig = new HashSet<>();
                Set<MProperty> processedGnr = new HashSet<>();

                for(MProperty origProperty : origProps)
                {
                    final String name = origProperty.GetName();
                    final String value = origProperty.GetValue();

                    for(MProperty gnrProperty : gnrProps)
                    {
                        if(processedGnr.contains(gnrProperty))
                        {
                            continue;
                        }

                        if(gnrProperty.GetName().equals(name))
                        {
                            String gnrValue = gnrProperty.GetValue();

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

                for(MProperty remainder : remainderOrig)
                {
                    if(defaultStyles.containsKey(remainder.GetName()))
                    {
                        if(remainder.GetValue().equals(defaultStyles.get(remainder.GetName())))
                        {
                            continue;
                        }
                    }

                    LogHandler.debug("[VERIFICATION] Found property %s in selector '....' from the original stylesheet that is not available from the generated stylesheet for state %s", remainder, state.getUrl());
                }
            }
        }
    }
}
