package com.crawljax.plugins.csssuite.sass.clonedetection;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;

import com.crawljax.plugins.csssuite.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.csssuite.sass.clonedetection.fpgrowth.FPGrowth;
import com.crawljax.plugins.csssuite.sass.clonedetection.items.Item;
import com.crawljax.plugins.csssuite.sass.clonedetection.items.ItemSet;
import com.crawljax.plugins.csssuite.sass.clonedetection.items.ItemSetList;

/**
 * Created by axel on 5/5/2015.
 *
 * Implementation of detecting clones based on FP-growth solution in https://github.com/dmazinanian/css-analyser
 */
public class CloneDetector
{
    //clones per property(name-value)
    //private final Map<String, List<CloneSet>> _allClones;

    //clones per set of rules, possibly more than 1 shared MProperty per ruleset
    //private final Map<String, Map<List<MSelector>, List<MProperty>>> _mixinClones;

//    public CloneDetector()
//    {
//        //_allClones = new HashMap<>();
//        _mixinClones = new HashMap<>();
//    }


    /**
     *
     * @param selectors
     * @return
     */
    public List<SassCloneMixin> GenerateMixins(List<MSelector> selectors)
    {
        List<SassCloneMixin> templates = new ArrayList<>();
        List<MSelector> allSelectors = new ArrayList<>(selectors);

        List<ItemSetList> results = FindDuplicationsAndFpGrowth(allSelectors);

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


            if (todo != null)
            {
                List<SassCloneMixin> innerTemplates = new ArrayList<>();

                for (Item i : todo)
                {
                    List<MSelector> newSelectors = new ArrayList<>();

                    for (Declaration d : i)
                    {
                        newSelectors.add(d.getSelector());
                    }

                    Optional<SassCloneMixin> existing = innerTemplates.stream().filter(t -> t.sameSelectors(newSelectors)).findFirst();

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
                        }
                    }
                    else
                    {
                        SassCloneMixin template = new SassCloneMixin();

                        newSelectors.forEach(s -> template.addSelector(s));

                        boolean pSet = false;
                        for (Declaration d : i)
                        {
                            if (!pSet)
                            {
                                pSet = true;
                                template.addProperty(d.getProperty());
                            }
                        }

                        innerTemplates.add(template);
                    }
                }

                templates.addAll(innerTemplates);

                for(SassCloneMixin template : innerTemplates)
                {
                    for(MSelector mSel : allSelectors.stream().filter(s -> template.GetRelatedSelectors().contains(s)).collect(Collectors.toList()))
                    {
                        mSel.RemovePropertiesByText(template.GetProperties());
                    }
                }

                results = FindDuplicationsAndFpGrowth(allSelectors);
            }
            else
            {
                break;
            }
        }

        return templates;
    }


    /**
     * Implementation taken from https://github.com/dmazinanian/css-analyser
     * and adapted to be applied on MSelectors and MProperties
     * @param selectors
     * @return
     */
    private static List<ItemSetList> FindDuplicationsAndFpGrowth(List<MSelector> selectors)
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
            List<Declaration> currentTypeIDuplicatedDeclarations = new ArrayList<>();

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

            Item newItem = declarationItemMap.get(currentDeclaration);
            if (newItem == null)
            {
                newItem = new Item(currentDeclaration);
                declarationItemMap.put(currentDeclaration, newItem);
            }

            while (++checkingDecIndex < declarations.size())
            {
                Declaration checkingDeclaration = declarations.get(checkingDecIndex);

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
                    typeOneDuplication.addAllDeclarations(currentTypeIDuplicatedDeclarations);
                    duplicationInstanceList.add(typeOneDuplication);
                }
            }
        }

        // Create a treeset of items for each selector with declarations found that occur in at least one other selector
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



//    /**
//     *
//     * @param selectors
//     * @return
//     */
//    public List<MSelector> MergeSelectors(List<MSelector> selectors)
//    {
//        Map<String, List<MSelector>> selectorClones = new HashMap<>();
//        List<MSelector> newSelectors = new ArrayList<>();
//
//        for(MSelector mSelector : selectors)
//        {
//            String selectorText = mSelector.GetSelectorText();
//
//            if(!selectorClones.containsKey(selectorText))
//            {
//                selectorClones.put(selectorText, new ArrayList<>());
//            }
//
//            selectorClones.get(selectorText).add(mSelector);
//        }
//
//        for(String selectorText : selectorClones.keySet())
//        {
//            List<MSelector> mSelectors = selectorClones.get(selectorText);
//            MSelector newSelector = new MSelector(mSelectors.get(0));
//
//            for(int i = 1; i < mSelectors.size(); i++)
//            {
//                newSelector.MergeProperties(mSelectors.get(i));
//            }
//
//            newSelectors.add(newSelector);
//        }
//
//        return newSelectors;
//    }

//    /**
//     *
//     * @param origSelectors
//     * @return
//     */
//    public List<SassTemplate> FindClonesAsSassTemplates(List<MSelector> origSelectors)
//    {
//        //allows for fast detection of clones (using hash compare on the keys)
//        //HashMap<String, CloneSet> clones = new HashMap<>();
//

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
//    }


//    /**
//     *
//     * @return
//     */
//    public String PrintClones()
//    {
//        SuiteStringBuilder result = new SuiteStringBuilder();
//
//        for(String fileName : _allClones.keySet())
//        {
//            if(_allClones.get(fileName).size() > 0)
//            {
//                result.append("Clones detected in file '%s'", fileName);
//
//                for (CloneSet cloneSet : _allClones.get(fileName))
//                {
//                    result.append("Clone: " + PropertyToString(cloneSet.GetProperty()) + "\n");
//                    for (MSelector mRule : cloneSet.GetSelectors().stream().sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber())).collect(Collectors.toList()))
//                    {
//                        result.append("Selector: " + mRule.toString() + "\n");
//                    }
//                    result.appendLine("\n");
//                }
//
//                for (List<MSelector> key : _mixinClones.get(fileName).keySet())
//                {
//                    final String[] s = {""};
//                    key.stream().sorted((s1, s2) -> Integer.compare(s1.GetRuleNumber(), s2.GetRuleNumber())).forEach(ms -> s[0] += "in selector: " + ms.toString() + "\n");
//
//                    result.append("Shared Properties Found!\n");
//                    result.append(s[0]);
//                    for (MProperty mProp : _mixinClones.get(fileName).get(key))
//                    {
//                        result.append("on property: " + mProp.toString() + "\n");
//                    }
//                    result.appendLine("\n");
//                }
//            }
//        }
//
//        return result.toString();
//    }
//

//    /**
//     * Wrapper for a MProperty that is shared accross multple css rules
//     */
//    private class CloneSet{
//        private MProperty _mProperty;
//        private List<MSelector> _mSelectors;
//
//        public CloneSet(MProperty mProp){
//            _mProperty = mProp;
//            _mSelectors = new ArrayList<>();
//        }
//
//        public List<MSelector> GetSelectors() {
//            return _mSelectors;
//        }
//
//        public void AddRuleToSet(MSelector mRule)
//        {
//            _mSelectors.add(mRule);
//        }
//
//        public MProperty GetProperty() {
//            return _mProperty;
//        }
//    }
//
//    /**
//
//     * @param mProp
//     * @return
//     */
//    private static String PropertyToString(MProperty mProp){
//        return mProp.GetName() + ":" + mProp.GetValue();
//    }
}