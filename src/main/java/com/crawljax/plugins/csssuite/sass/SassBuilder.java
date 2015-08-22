package com.crawljax.plugins.csssuite.sass;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.sass.clonedetection.CloneDetector;
import com.crawljax.plugins.csssuite.colors.ColorNameFinder;
import com.crawljax.plugins.csssuite.sass.mixins.SassBoxMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.sass.mixins.SassMixinBase;
import com.crawljax.plugins.csssuite.sass.variables.SassVarType;
import com.crawljax.plugins.csssuite.sass.variables.SassVariable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/8/2015.
 */
public class SassBuilder
{
    private final int _propUpperLimit;
    private final int _mixinMinPropCount;
    private final MCssFile _mcssFile;
    private final List<SassVariable> _sassVariables;
    private final Map<String, String> _alreadyDefinedVars;
    private final SassStatistics _statistics;

    public SassBuilder(MCssFile mCssFile)
    {
        this(mCssFile, 999);
    }

    public SassBuilder(MCssFile mCssFile, int propUpperLimit)
    {
        _mixinMinPropCount = 0;
        _propUpperLimit = propUpperLimit;
        _mcssFile = mCssFile;
        _sassVariables = new ArrayList<>();
        _alreadyDefinedVars = new HashMap<>();
        _statistics = new SassStatistics();
    }

    public SassStatistics getStatistics()
    {
        _statistics.variableCount = _sassVariables.size();
        return _statistics;
    }

    public SassFile generateSass()
    {
        CloneDetector cd = new CloneDetector();

        List<MCssRule> cssRules = _mcssFile.GetRules();

        // copy all MSelectors, so we won't affect the original rules
        List<MSelector> validSelectors = new ArrayList<>();
        List<MSelector> largeSelectors = new ArrayList<>();

        for(MCssRule rule : cssRules)
        {
            for(MSelector mSelector : rule.GetSelectors())
            {
                if(mSelector.GetDeclarations().size() > _propUpperLimit)
                {
                    largeSelectors.add(new MSelector(mSelector));
                }
                else
                {
                    validSelectors.add(new MSelector(mSelector));
                }
            }
        }

        LogHandler.debug("[SassGenerator] Generate SASS variables...");
        GenerateVariables(validSelectors);
        GenerateVariables(largeSelectors);

        LogHandler.debug("[SassGeneratpr] Generate SASS mixins...");
        List<SassCloneMixin> validMixins = ProcessAndFilterClones(cd.GenerateMixins(validSelectors));

        LogHandler.debug("[SassGenerator] Generate SASS selectors...");
        List<SassSelector> sassSelectors = GenerateSassSelectors(validSelectors, validMixins);
        sassSelectors.addAll(GenerateSassSelectors(largeSelectors, validMixins));

        LogHandler.debug("[SassGenerator] Generate SASS convenience mixins...");
        List<SassMixinBase> sassMixins = GenerateConvenienceMixins(sassSelectors);

        LogHandler.debug("[SassGenerator] Generate SASS rules...");
        List<SassRuleBase> sassRules = GenerateSassRules(sassSelectors, _mcssFile.GetMediaRules(), _mcssFile.GetIgnoredRules());

        return new SassFile(_sassVariables, validMixins, sassMixins, sassRules);
    }


    /**
     *
     * @param selectors
     */
    private void GenerateVariables(List<MSelector> selectors)
    {
        ColorNameFinder ctn = new ColorNameFinder();

        for(MSelector mSelector : selectors)
        {
            for(MDeclaration mDeclaration : mSelector.GetDeclarations())
            {
                if (mDeclaration.IsIgnored())
                {
                    continue;
                }

                String origName = mDeclaration.GetName();
                String origValue = mDeclaration.GetValue();

                try
                {
                    if (origName.equals("font-family"))
                    {
                        SassVarType varType = SassVarType.FONT;
                        String varName = "font-stack";
                        String varValue = origValue;

                        varName = GenerateVariable(varName, varValue, varType, mDeclaration);
                        String escapedValue = varValue.replaceFirst("\\(", "\\\\(").replaceFirst("\\)", "\\\\)");
                        String escapedName = String.format("\\$%s", varName);
                        origValue = origValue.replaceFirst(escapedValue, escapedName);
                    }
                    else
                    {
                        String[] parts = origValue.split("\\s");

                        if(parts.length > 10)
                        {
                            parts = new String[]{origValue};
                        }

                        for (int i = 0; i < parts.length; i++)
                        {
                            SassVarType varType = null;
                            String varName = "";
                            String varValue = "";

                            String part = parts[i];

                            try
                            {
                                if (part.contains("url("))
                                {
                                    if (!part.endsWith(")"))
                                    {
                                        for (int j = i + 1; j < parts.length; j++)
                                        {
                                            String part2 = parts[j];
                                            part = part + part2;
                                            if (part2.endsWith(")"))
                                            {
                                                i = j;
                                                break;
                                            }
                                        }
                                    }

                                    varType = SassVarType.URL;
                                    varName = "url";
                                    varValue = TryFindUrl(part);
                                }
                                else if (part.contains("rgba"))
                                {
                                    String rgbaValue = TryFindRgba(part);
                                    String[] rgbaParts = rgbaValue.replaceFirst("rgba\\(", "").replaceFirst("\\)", "").split(",");

                                    varType = SassVarType.ALPHA_COLOR;
                                    varName = String.format("%s_%s", "alpha_color", ctn.TryGetNameForRgb(Integer.parseInt(rgbaParts[0].trim()), Integer.parseInt(rgbaParts[1].trim()), Integer.parseInt(rgbaParts[2].trim())));
                                    varValue = rgbaValue;
                                }
                                else if (part.contains("#"))
                                {
                                    String hexValue = TryFindHex(part);

                                    varType = SassVarType.COLOR;
                                    varName = String.format("%s_%s", "color", ctn.TryGetNameForHex(hexValue));
                                    varValue = hexValue;
                                }
                            }
                            catch (CssSuiteException ex)
                            {
                                LogHandler.error(ex, "[SassGenerator] Error occurred while creating SassVariable for declaration '%s' with value '%s' for selector '%s'",
                                        origName, origValue, mSelector.GetSelectorText());
                                continue;
                            }

                            if (!varName.isEmpty() && !varValue.isEmpty())
                            {
                                varName = String.format("$%s", GenerateVariable(varName, varValue, varType, mDeclaration));

                                String escapedValue = varValue.replaceFirst("\\(", "\\\\(").replaceFirst("\\)", "\\\\)");
                                String escapedName = String.format("\\%s", varName);

                                if(origValue.contains(escapedValue))
                                {
                                    origValue = origValue.replaceFirst(escapedValue, escapedName);
                                }
                                else
                                {
                                    origValue = origValue.replace(varValue, varName);
                                }

                                _statistics.declarationsTouchedByVars++;
                            }
                        }
                    }

                    mDeclaration.SetNormalizedValue(origValue);
                }
                catch (Exception e)
                {
                    LogHandler.error(e, "[SassGenerator] Error occurred while creating SassVariable for declaration '%s' with value '%s' for selector '%s'",
                            origName, origValue, mSelector.GetSelectorText());
                }
            }
        }
    }


    private String GenerateVariable(String varName, String varValue, SassVarType varType, MDeclaration originalProperty)
    {
        // if varName already used, extend varName with an id
        if (_alreadyDefinedVars.containsKey(varName) && !_alreadyDefinedVars.get(varName).equals(varValue))
        {
            int id = 1;
            while (true)
            {
                String replace = String.format("%s_%d", varName, id);
                if (!_alreadyDefinedVars.containsKey(replace) || (_alreadyDefinedVars.containsKey(replace) && _alreadyDefinedVars.get(replace).equals(varValue)))
                {
                    varName = replace;
                    break;
                }
                id++;
            }
        }

        if (!_alreadyDefinedVars.containsKey(varName))
        {
            SassVariable sv = new SassVariable(varType, varName, varValue);
            _sassVariables.add(sv);
            _alreadyDefinedVars.put(varName, varValue);
        }

        return varName;
    }


    private String TryFindUrl(String value)
    {
        int s =  value.indexOf("url(");
        //int e = value.indexOf(")", s);
        return value.substring(s, value.length());
    }


    private String TryFindHex(String value)
    {
        if(value.contains("#"))
        {
            int s =  value.indexOf("#");
            int e = value.indexOf(" ", s);
            if(e == -1)
            {
                e = value.indexOf(")", s);
                if(e == -1)
                {
                    e = value.length();
                }
            }
            return value.substring(s, e);
        }

        return "";
    }


    private String TryFindRgba(String value)
    {
        if(value.contains("rgba("))
        {
            int s =  value.indexOf("rgba(");
            int e = value.indexOf(")", s);
            return value.substring(s, e+1);
        }

        return "";
    }


    /**
     *
     * @param mixins
     * @return
     */
    private List<SassCloneMixin> ProcessAndFilterClones(List<SassCloneMixin> mixins)
    {
        List<SassCloneMixin> validMixins = new ArrayList<>();

        for (SassCloneMixin mixin : mixins)
        {
            SortMixinValues(mixin);

            List<MDeclaration> templateProps = mixin.GetDeclarations();
            int numberOfProps = templateProps.size();

            // if the total number of declarations exceeds minimum declaration threshold, continue
            if (numberOfProps >= _mixinMinPropCount)
            {
                // number of lines that mixin will add to output file
                int mixinSize = templateProps.size();

                int count = 0;
                int lineNumber = 0;
                int order = 0;
                for(MSelector mSelector : mixin.GetRelatedSelectors())
                {
                    if(mSelector.GetLineNumber() != lineNumber || mSelector.GetOrder() != order)
                    {
                        count += numberOfProps - 1;
                        lineNumber = mSelector.GetLineNumber();
                        order = mSelector.GetOrder();
                    }
                }

                // if the total number of declarations saved exceeds the number of declarations added to the mixin, include the mixin
                if(count >= mixinSize)
                {
                    validMixins.add(mixin);
                    _statistics.cloneSetCount++;
                    _statistics.declarationsTouchedByClones += mixinSize * mixin.GetRelatedSelectors().size();
                }
                else
                {
                    RestoreCloneMixin(mixin);
                }
            }
            else
            {
                RestoreCloneMixin(mixin);
            }
        }

        for (int i = 0; i < validMixins.size(); i++)
        {
            validMixins.get(i).SetNumber(i + 1);
        }

        return validMixins;
    }


    /**
     *
     * @param mixin
     */
    private void SortMixinValues(SassCloneMixin mixin)
    {
        mixin.GetRelatedSelectors().sort((s1, s2) ->
        {
            if(s1.GetLineNumber() == s2.GetLineNumber())
            {
                return Integer.compare(s1.GetOrder(), s2.GetOrder());
            }

            return Integer.compare(s1.GetLineNumber(), s2.GetLineNumber());
        });
        mixin.GetDeclarations().sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));
    }


    /**
     *
     * @param mixin
     */
    private void RestoreCloneMixin(SassCloneMixin mixin)
    {
        List<MDeclaration> mixinProps = mixin.GetDeclarations();

        for (MSelector mSelector : mixin.GetRelatedSelectors())
        {
            mixinProps.forEach(mixinProp -> mSelector.RestoreDevlaration(new MDeclaration(mixinProp.GetName(), mixinProp.GetValue(),
                    mixinProp.IsImportant(), mixinProp.IsEffective(),
                    mixin.getDeclarationOrderForSelector(mSelector, mixinProp))));
        }
    }


    /**
     *
     * @param sassSelectors
     * @return
     */
    private List<SassMixinBase> GenerateConvenienceMixins(List<SassSelector> sassSelectors)
    {
        List<SassMixinBase> mixins = new ArrayList<>();

        SassBoxMixin padding = new SassBoxMixin("padding");
        SassBoxMixin margin = new SassBoxMixin("margin");

        boolean pUsed = false;
        boolean mUsed = false;

        for(SassSelector sassSelector : sassSelectors)
        {
            List<MDeclaration> paddings = sassSelector.GetDeclarations().stream().filter(p -> !p.IsIgnored() && p.GetName().contains("padding-")).collect(Collectors.toList());
            List<MDeclaration> margins = sassSelector.GetDeclarations().stream().filter(p -> !p.IsIgnored() && p.GetName().contains("margin-")).collect(Collectors.toList());

            if(paddings.size() > 1)
            {
                sassSelector.AddInclude(padding.CreateMixinCall(paddings));
                sassSelector.RemoveDeclarations(paddings);

                pUsed = true;
                _statistics.mergeMixinCount++;
                _statistics.declarationsTouchedByMerges += paddings.size();
            }

            if(margins.size() > 1)
            {
                sassSelector.AddInclude(margin.CreateMixinCall(margins));
                sassSelector.RemoveDeclarations(margins);

                mUsed = true;
                _statistics.mergeMixinCount++;
                _statistics.declarationsTouchedByMerges += margins.size();
            }
        }

        if(pUsed)
        {
            mixins.add(padding);
        }

        if(mUsed)
        {
            mixins.add(margin);
        }

        return mixins;
    }


    /**
     *
     * @param selectors
     * @param extensions
     * @return
     */
    private List<SassSelector> GenerateSassSelectors(List<MSelector> selectors, List<SassCloneMixin> extensions)
    {
        List<SassSelector> results = new ArrayList<>();

        for(MSelector mSelector : selectors)
        {
            mSelector.GetDeclarations().sort((p1, p2) -> Integer.compare(p1.GetOrder(), p2.GetOrder()));
            SassSelector ss = new SassSelector(mSelector);

            for(SassCloneMixin st : extensions)
            {
                for(MSelector related : st.GetRelatedSelectors())
                {
                    if(related == mSelector)
                    {
                        ss.AddCloneInclude(st);
                        break;
                    }
                }
            }

            results.add(ss);
        }

        return results;
    }


    /**
     *
     * @param sassSelectors
     * @param mediaRules
     * @param ignoredRules
     * @return
     */
    private List<SassRuleBase> GenerateSassRules(List<SassSelector> sassSelectors, List<MCssMediaRule> mediaRules, List<MCssRuleBase> ignoredRules)
    {
        // group selectors by their line number, maintain order by using LinkedHashMap
        Map<Integer, List<SassSelector>> lineNoSelectorMap = new LinkedHashMap<>();
        sassSelectors.stream().filter(s -> s.GetMediaQueries().isEmpty())
                .sorted((s1, s2) ->
                {
                    if(s1.GetLineNumber() == s2.GetLineNumber())
                    {
                        return Integer.compare(s1.GetOrder(), s2.GetOrder());
                    }

                    return Integer.compare(s1.GetLineNumber(), s2.GetLineNumber());
                })
                .forEach((s) ->
                {
                    int lineNumber = s.GetLineNumber();
                    if (!lineNoSelectorMap.containsKey(lineNumber))
                    {
                        lineNoSelectorMap.put(lineNumber, new ArrayList<>());
                    }

                    lineNoSelectorMap.get(lineNumber).add(s);
                });

        List<SassRuleBase> sassRules = new ArrayList<>();

        // final merge, verify that declarations previously contained in the same rule (e.g. equal line number)
        // do not have the same declarations (e.g. all sass declarations and regular declarations
        // if they do, merge them into 1 SassRule
        for(int lineNumber : lineNoSelectorMap.keySet())
        {
            List<SassSelector> innerSelectors = lineNoSelectorMap.get(lineNumber);
            List<Integer> processedIdx = new ArrayList<>();
            for(int i = 0; i < innerSelectors.size(); i++)
            {
                if(processedIdx.contains(i))
                {
                    continue;
                }

                List<SassSelector> selectorsForRule = new ArrayList<>();
                SassSelector current = innerSelectors.get(i);
                processedIdx.add(i);
                selectorsForRule.add(current);

                for(int j = i + 1; j < innerSelectors.size(); j++)
                {
                    if(processedIdx.contains(j))
                    {
                        continue;
                    }

                    SassSelector other = innerSelectors.get(j);
                    if(current.HasEqualDeclarationsByText(other))
                    {
                        selectorsForRule.add(other);
                        processedIdx.add(j);
                    }
                }

                sassRules.add(new SassRule(lineNumber, selectorsForRule));
            }
        }

        for(MCssRuleBase ignoredRule : ignoredRules.stream().filter(r -> r.GetMediaQueries().isEmpty()).collect(Collectors.toList()))
        {
            sassRules.add(new SassIgnoredRule(ignoredRule.GetLineNumber(), ignoredRule.GetAbstractRule()));
        }

        // now process selectors held in media-queries
        List<SassSelector> mediaSelectors = sassSelectors.stream().filter(s -> s.GetMediaQueries().size() > 0).collect(Collectors.toList());

        for(MCssMediaRule mediaRule : mediaRules)
        {
            SassMediaRule sassMediaRule = RecursiveGenerateMediaRules(mediaRule, mediaSelectors, ignoredRules);
            if(sassMediaRule != null)
            {
                sassRules.add(sassMediaRule);
            }
        }

        return sassRules;
    }


    /**
     *
     * @param mediaRule
     * @param selectors
     * @param ignoredRules
     * @return
     */
    private SassMediaRule RecursiveGenerateMediaRules(MCssMediaRule mediaRule, List<SassSelector> selectors, List<MCssRuleBase> ignoredRules)
    {
        List<SassRuleBase> innerRules = new ArrayList<>();

        for(MCssRuleBase mRule : mediaRule.GetInnerRules())
        {
            if(mRule instanceof MCssMediaRule)
            {
                innerRules.add(RecursiveGenerateMediaRules((MCssMediaRule)mRule, selectors, ignoredRules));
            }
            else if (mRule instanceof MCssRule)
            {
                List<SassSelector> relatedSelectors = selectors.stream().filter(s -> s.GetParent().equals(mRule)).collect(Collectors.toList());
                if(!relatedSelectors.isEmpty())
                {
                    innerRules.add(new SassRule(mRule.GetLineNumber(), relatedSelectors));
                }
            }
            else
            {
                innerRules.add(new SassIgnoredRule(mRule.GetLineNumber(), mRule.GetAbstractRule()));
            }
        }

        if(innerRules.isEmpty())
        {
            return null;
        }

        return new SassMediaRule(mediaRule.GetLineNumber(), mediaRule.GetMediaQueries(), innerRules);
    }
}