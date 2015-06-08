package com.crawljax.plugins.csssuite.plugins.sass;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.generator.CssWriter;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.plugins.sass.fpgrowth.FPGrowth;
import com.crawljax.plugins.csssuite.plugins.sass.items.Item;
import com.crawljax.plugins.csssuite.plugins.sass.items.ItemSet;
import com.crawljax.plugins.csssuite.plugins.sass.items.ItemSetList;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import sun.rmi.runtime.Log;

/**
 * Created by axel on 5/5/2015.
 * Initial clonedetector, differs from css-analyser tool, less bloated
 * This class is able to find duplicates by matching properties and values inside the css rules from a given file
 * For each property a CloneSet is created, which holds that property and two or more rules in which it prevails
 * Based on the rules inside any cloneset, we check if those rules exist in other clonesets.
 * If they do, then more than one property is shared by those rules
 */
public class CloneDetector implements ICssPostCrawlPlugin
{
    //clones per property(name-value)
    private final Map<String, List<CloneSet>> _allClones;

    //clones per set of rules, possibly more than 1 shared MProperty per ruleset
    private final Map<String, Map<List<MSelector>, List<MProperty>>> _mixinClones;

    public CloneDetector()
    {
        _allClones = new HashMap<>();
        _mixinClones = new HashMap<>();
    }


    /**

     * @param mProp
     * @return
     */
    private static String PropertyToString(MProperty mProp){
        return mProp.GetName() + ":" + mProp.GetValue();
    }


    /**
     *
     * @return
     */
    public int CountClones() { return _allClones.size(); }


    /**
     *
     * @return
     */
    public String PrintClones()
    {
        SuiteStringBuilder result = new SuiteStringBuilder();

        for(String fileName : _allClones.keySet())
        {
            if(_allClones.get(fileName).size() > 0)
            {
                result.append("Clones detected in file '%s'", fileName);

                for (CloneSet cloneSet : _allClones.get(fileName))
                {
                    result.append("Clone: " + PropertyToString(cloneSet.GetProperty()) + "\n");
                    for (MSelector mRule : cloneSet.GetSelectors().stream().sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber())).collect(Collectors.toList()))
                    {
                        result.append("Selector: " + mRule.toString() + "\n");
                    }
                    result.appendLine("\n");
                }

                for (List<MSelector> key : _mixinClones.get(fileName).keySet())
                {
                    final String[] s = {""};
                    key.stream().sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber())).forEach(ms -> s[0] += "in selector: " + ms.toString() + "\n");

                    result.append("Shared Properties Found!\n");
                    result.append(s[0]);
                    for (MProperty mProp : _mixinClones.get(fileName).get(key))
                    {
                        result.append("on property: " + mProp.toString() + "\n");
                    }
                    result.appendLine("\n");
                }
            }
        }

        return result.toString();
    }

    @Override
    public Map<String, MCssFile> Transform(Map<String, MCssFile> cssRules)
    {

        for(String fileName : cssRules.keySet())
        {
            //allows for fast detection of clones (using hash compare on the keys)
            HashMap<String, CloneSet> clones = new HashMap<>();
            List<MCssRule> rules = cssRules.get(fileName).GetRules();

            List<MSelector> origSelectors = new ArrayList<>();
            Map<String, List<MSelector>> selectorClones = new HashMap<>();
            List<MSelector> newSelectors = new ArrayList<>();

            for(MCssRule rule : rules)
            {
                origSelectors.addAll(rule.GetSelectors());
            }

            for(MSelector mSelector : origSelectors)
            {
                String selectorText = mSelector.GetSelectorText();

                if(!selectorClones.containsKey(selectorText))
                {
                    selectorClones.put(selectorText, new ArrayList<>());
                }

                selectorClones.get(selectorText).add(mSelector);
            }

            for(String selectorText : selectorClones.keySet())
            {
                List<MSelector> mSelectors = selectorClones.get(selectorText);
                MSelector newSelector = new MSelector(mSelectors.get(0));

                for(int i = 1; i < mSelectors.size(); i++)
                {
                    newSelector.MergeProperties(mSelectors.get(i));
                }

                newSelectors.add(newSelector);
            }

            //FindDuplicationsAndFpGrowth(newSelectors);
            List<SassTemplate> templates = GenerateSassTemplates(newSelectors);

            for(SassTemplate t : templates)
            {
                List<MProperty> properties = t.GetProperties();
                //restore properties
                if(properties.size() == 1 || properties.size() == 2)
                {
                    for(MSelector mSelector : t.GetRelatedSelectors())
                    {
                        for(MProperty mProperty : properties)
                        {
                            mSelector.AddProperty(mProperty);
                        }
                    }
                }

//                String templateText = "";
//                for(MSelector ms : t.GetRelatedSelectors())
//                {
//                    templateText += ms.toString();
//                }
//
//                LogHandler.debug("Generate SASS template for selectors: %s", templateText);
//                for(MProperty p : t.GetProperties())
//                {
//                    LogHandler.debug("MProperty: %s", p);
//                }
            }

            templates = templates.stream().filter(t -> t.GetProperties().size() >= 3).collect(Collectors.toList());
            LogHandler.debug("Found %d templates that apply to more than 3 or more properties", templates.size());

            for(int i = 0; i < templates.size(); i++)
            {
                templates.get(i).SetNumber(i+1);
            }

            List<SassSelector> sSelectors = GenerateSass(newSelectors, templates);

            CssWriter cssWriter = new CssWriter();
            try
            {
                cssWriter.GenerateSassFile(fileName, sSelectors, templates);
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
//
//            for (MSelector mSelector :newSelectors)
//            {
//                for (MProperty mProp : mSelector.GetProperties())
//                {
//                    String key = mProp.toString();
//                    if (clones.containsKey(key))
//                    {
//                        clones.get(key).AddRuleToSet(mSelector);
//                    }
//                    else
//                    {
//                        clones.put(key, new CloneSet(mProp));
//                        clones.get(key).AddRuleToSet(mSelector);
//                    }
//                }
//            }
//
//            _allClones.put(fileName, clones.values().stream().filter(cloneSet -> cloneSet.GetSelectors().size() > 1).collect(Collectors.toList()));
//
//            HashMap<List<MSelector>, List<MProperty>> result = new HashMap<>();
//
//            int idx = 1;
//            for (CloneSet cloneSet : _allClones.get(fileName))
//            {
//
//                List<MSelector> group = cloneSet.GetSelectors();
//                if (result.containsKey(group))
//                {
//                    idx++;
//                    continue;
//                }
//
//                List<MProperty> props = new ArrayList<>();
//                props.add(cloneSet.GetProperty());
//
//                for (int i = idx; i < _allClones.size(); i++)
//                {
//                    CloneSet otherCloneSet = _allClones.get(fileName).get(i);
//                    //todo: still need to develop way by which some(at least 2) selectors match between clonesets
//                    if (otherCloneSet.GetSelectors().containsAll(cloneSet.GetSelectors()))
//                    {
//                        props.add(otherCloneSet.GetProperty());
//                    }
//                }
//
//                result.put(group, props);
//
//                idx++;
//            }
//
//            _mixinClones.put(fileName, result);
        }

        return cssRules;

    }


    private List<SassSelector> GenerateSass(List<MSelector> selectors, List<SassTemplate> extensions)
    {
        List<SassSelector> results = new ArrayList<>();

        for(MSelector mSelector : selectors)
        {
            SassSelector ss = new SassSelector(mSelector);

            for(SassTemplate st : extensions)
            {
                boolean applies = false;
                for(MSelector related : st.GetRelatedSelectors())
                {
                    if(related == mSelector)
                    {
                        applies = true;
                        break;
                    }
                }

                if(applies)
                {
                    ss.AddExtend(st);
                }
            }

            results.add(ss);
        }

        return results;
    }


    /**
     * Wrapper for a MProperty that is shared accross multple css rules
     */
    private class CloneSet{
        private MProperty _mProperty;
        private List<MSelector> _mSelectors;

        public CloneSet(MProperty mProp){
            _mProperty = mProp;
            _mSelectors = new ArrayList<>();
        }

        public List<MSelector> GetSelectors() {
            return _mSelectors;
        }

        public void AddRuleToSet(MSelector mRule)
        {
            _mSelectors.add(mRule);
        }

        public MProperty GetProperty() {
            return _mProperty;
        }
    }


    private List<SassTemplate> GenerateSassTemplates(List<MSelector> selectors)
    {
        List<SassTemplate> templates = new ArrayList<>();
        List<MSelector> allSelectors = new ArrayList<>(selectors);

        List<ItemSetList> results = FindDuplicationsAndFpGrowth(allSelectors);

        int templateNo = 1;

        boolean notFeasible = false;
        while(true)
        {
            int largest = 0;
            ItemSet todo = null;

            for (ItemSetList isl : results)
            {
                for (ItemSet is : isl)
                {
                    int lines = 0;
                    for (Item i : is)
                    {
                        lines += i.size();
                    }

                    if (lines > largest)
                    {
                        largest = lines;
                        todo = is;
                    }
                }

                // remove the one we process now
                if(todo != null)
                    isl.remove(todo);
            }

            List<MSelector> origs = new ArrayList<>(selectors);
            List<List<MSelector>> news = new ArrayList<>();

            if (todo != null)
            {
                List<SassTemplate> innerTemplates = new ArrayList<>();

                for (Item i : todo)
                {
                    List<MProperty> newProps = new ArrayList<>();
                    List<MSelector> newSelectors = new ArrayList<>();

                    for (Declaration d : i)
                    {
                        newSelectors.add(d.getSelector());
                    }

                    Optional<SassTemplate> existing = innerTemplates.stream().filter(t -> t.sameSelectors(newSelectors)).findFirst();

                    if (existing.isPresent())
                    {
                        boolean pSet = false;
                        for (Declaration d : i)
                        {
                            if (!pSet)
                            {
                                pSet = true;
                                existing.get().addProperty(d.getProperty());
                            }

//                        MSelector m = d.getSelector();
//                        newSelectors.add(new MSelector(m.GetW3cSelector(), newProps, m.GetRuleNumber(), m.GetMediaQueries()));
//                        m.RemoveProperties(Arrays.asList(d.getProperty()));
                        }
                    }
                    else
                    {
                        SassTemplate template = new SassTemplate();
                        templateNo++;

                        newSelectors.forEach(s -> template.addSelector(s));

                        boolean pSet = false;
                        for (Declaration d : i)
                        {
                            if (!pSet)
                            {
                                pSet = true;
                                template.addProperty(d.getProperty());
                            }
//
//                        MSelector m = d.getSelector();
//                        newSelectors.add(new MSelector(m.GetW3cSelector(), newProps, m.GetRuleNumber(), m.GetMediaQueries()));
//                        m.RemoveProperties(Arrays.asList(d.getProperty()));
                        }

                        innerTemplates.add(template);
                    }

//                news.add(newSelectors);
                }

                templates.addAll(innerTemplates);

                List<MSelector> selectorsToRemove = new ArrayList<>();

                for(SassTemplate template : innerTemplates)
                {
                    for(MSelector mSel : allSelectors.stream().filter(s -> template.GetRelatedSelectors().contains(s)).collect(Collectors.toList()))
                    {
                        mSel.RemovePropertiesByText(template.GetProperties());
//                        if(mSel.GetProperties().size() == 0)
//                        {
//                            selectorsToRemove.add(mSel);
//                        }
                    }
                }

                allSelectors.removeAll(selectorsToRemove);

                results = FindDuplicationsAndFpGrowth(allSelectors);

//
//            for(List<MSelector> mSelectors : news)
//            {
//                for(MSelector mSelector : mSelectors)
//                {
//                    MSelector toUpdate = origs.stream().filter(m -> m.toString().equals(mSelector.toString())).findFirst().get();
//                    if (toUpdate.GetProperties().size() == 0)
//                    {
//                        selectorsToRemove.add(mSelector);
//                    }
//                }
//            }
//
                //origs.removeAll(selectorsToRemove);
            }
            else
            {
                break;
            }
        }

        return templates;
    }


    private List<ItemSetList> FindDuplicationsAndFpGrowth(List<MSelector> selectors)
    {
        Map<Declaration, Item> declarationItemMap = new HashMap<>();

        List<Declaration> declarations = new ArrayList<>();

        selectors.stream().forEach(s -> s.GetProperties().forEach(p -> declarations.add(new Declaration(p, s))));

        Set<Integer> visitedDeclarations = new HashSet<>();

        int currentDeclarationIndex = -1;

        TypeOneDuplicationInstance typeOneDuplication = new TypeOneDuplicationInstance();
        List<TypeOneDuplicationInstance> duplicationInstanceList = new ArrayList<>();

        while (++currentDeclarationIndex < declarations.size())
        {
            Declaration currentDeclaration = declarations.get(currentDeclarationIndex);

            int checkingDecIndex = currentDeclarationIndex;

        /*
         * We want to keep all current identical declarations together, and
         * then add them to the duplications list when we found all
         * identical declarations of current declaration.
         */
            List<Declaration> currentTypeIDuplicatedDeclarations = new ArrayList<Declaration>();

        /*
         * So we add current declaration to this list and add all identical
         * declarations to this list
         */
            currentTypeIDuplicatedDeclarations.add(currentDeclaration);

        /*
         * Only when add the current duplication to the duplications list
         * that we have really found a duplication
         */
            boolean mustAddCurrentTypeIDuplication = false;

        /*
         * As we are going to find the duplications of type II, we repeat
         * three previous expressions for type II duplications
         */
            List<Declaration> currentTypeIIDuplicatedDeclarations = new ArrayList<Declaration>();
            currentTypeIIDuplicatedDeclarations.add(currentDeclaration);
            boolean mustAddCurrentTypeTwoDuplication = false;

            // for Apriori
            Item newItem = declarationItemMap.get(currentDeclaration);
            if (newItem == null)
            {
                newItem = new Item(currentDeclaration);
                declarationItemMap.put(currentDeclaration, newItem);
//                ItemSet itemSet = new ItemSet();
//                itemSet.add(newItem);
//                C1.add(itemSet);
            }

            while (++checkingDecIndex < declarations.size())
            {
                Declaration checkingDeclaration = declarations.get(checkingDecIndex);

//                boolean equals = currentDeclaration.declarationEquals(checkingDeclaration);

                boolean equals = currentDeclaration.getProperty().toString().equals(checkingDeclaration.getProperty().toString());

                if (equals && !visitedDeclarations.contains(currentDeclarationIndex))
                {

                    // We have found type I duplication
                    // We add the checkingDeclaration, it will add the Selector
                    // itself.
                    currentTypeIDuplicatedDeclarations.add(checkingDeclaration);
                    visitedDeclarations.add(checkingDecIndex);
                    mustAddCurrentTypeIDuplication = true;

                    // This only used in apriori and fpgrowth
                    newItem.add(checkingDeclaration);
                    declarationItemMap.put(checkingDeclaration, newItem);
                    newItem.addDuplicationType(1);
                }
            }

            // Only if we have at least one declaration in the list (at list one
            // duplication)
            if (mustAddCurrentTypeIDuplication)
            {
                if (typeOneDuplication.hasAllSelectorsForADuplication(currentTypeIDuplicatedDeclarations))
                {
                    typeOneDuplication.addAllDeclarations(currentTypeIDuplicatedDeclarations);
                }
                else
                {
                    typeOneDuplication = new TypeOneDuplicationInstance();
                    typeOneDuplication
                            .addAllDeclarations(currentTypeIDuplicatedDeclarations);
                    duplicationInstanceList.add(typeOneDuplication);
                }
            }
        }

        String test = "";

        List<TreeSet<Item>> itemSets = new ArrayList<>(selectors.size());

        for (MSelector s : selectors)
        {
            TreeSet<Item> currentItems = new TreeSet<>();
            for (Declaration decl : declarations.stream().filter(d -> d.getSelector() == s).collect(Collectors.toList()))
            {
                Item item = declarationItemMap.get(decl);
                if (item.getSupportSize() >= 2)
                {
                    currentItems.add(item);
                }
            }
            if (currentItems.size() > 0)
                itemSets.add(currentItems);
        }

        FPGrowth fpGrowth = new FPGrowth(false);
        return fpGrowth.mine(itemSets, 2);
    }
}