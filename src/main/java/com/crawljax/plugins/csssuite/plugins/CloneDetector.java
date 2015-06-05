package com.crawljax.plugins.csssuite.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.interfaces.ICssPostCrawlPlugin;
import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import org.apache.log4j.Logger;

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


//    /**
//     *
//     * @param rules
//     */
//    public void Detect(List<MCssRule> rules)
//    {
//        //allows for fast detection of clones (using hash compare on the keys)
//        HashMap<String, CloneSet> clones = new HashMap<>();
//
//        for(MCssRule mRule : rules)
//        {
//            for(MSelector mSelector : mRule.GetSelectors())
//            {
//                for (MProperty mProp : mSelector.GetProperties())
//                {
//                    String key = PropertyToString(mProp);
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
//        }
//
//        _allClonesclones.values().stream().filter(cloneSet -> cloneSet.GetSelectors().size() > 1).collect(Collectors.toList());
//
//        HashMap<List<MSelector>, List<MProperty>> result = new HashMap<>();
//
//        int idx = 1;
//        for(CloneSet cloneSet : _allClones){
//
//            List<MSelector> group = cloneSet.GetSelectors();
//            if(result.containsKey(group)) {
//                idx++;
//                continue;
//            }
//
//            List<MProperty> props = new ArrayList<>();
//            props.add(cloneSet.GetProperty());
//
//            for(int i = idx; i < _allClones.size(); i++)
//            {
//                CloneSet otherCloneSet = _allClones.get(i);
//                //todo: still need to develop way by which some(at least 2) selectors match between clonesets
//                if(otherCloneSet.GetSelectors().containsAll(cloneSet.GetSelectors()))
//                {
//                    props.add(otherCloneSet.GetProperty());
//                }
//            }
//
//            result.put(group, props);
//
//            idx++;
//        }
//
//        _mixinClones = result;
//    }


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
                    for (MSelector mRule : cloneSet.GetSelectors())
                    {
                        result.append("Selector: " + mRule.toString() + "\n");
                    }
                    result.appendLine("\n");
                }

                for (List<MSelector> key : _mixinClones.get(fileName).keySet())
                {
                    final String[] s = {""};
                    key.forEach(ms -> s[0] += "in selector: " + ms.toString() + "\n");

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

            for (MCssRule mRule : rules)
            {
                for (MSelector mSelector : mRule.GetSelectors())
                {
                    for (MProperty mProp : mSelector.GetProperties())
                    {
                        String key = PropertyToString(mProp);
                        if (clones.containsKey(key))
                        {
                            clones.get(key).AddRuleToSet(mSelector);
                        }
                        else
                        {
                            clones.put(key, new CloneSet(mProp));
                            clones.get(key).AddRuleToSet(mSelector);
                        }
                    }
                }
            }

            _allClones.put(fileName, clones.values().stream().filter(cloneSet -> cloneSet.GetSelectors().size() > 1).collect(Collectors.toList()));

            HashMap<List<MSelector>, List<MProperty>> result = new HashMap<>();

            int idx = 1;
            for (CloneSet cloneSet : _allClones.get(fileName))
            {

                List<MSelector> group = cloneSet.GetSelectors();
                if (result.containsKey(group))
                {
                    idx++;
                    continue;
                }

                List<MProperty> props = new ArrayList<>();
                props.add(cloneSet.GetProperty());

                for (int i = idx; i < _allClones.size(); i++)
                {
                    CloneSet otherCloneSet = _allClones.get(fileName).get(i);
                    //todo: still need to develop way by which some(at least 2) selectors match between clonesets
                    if (otherCloneSet.GetSelectors().containsAll(cloneSet.GetSelectors()))
                    {
                        props.add(otherCloneSet.GetProperty());
                    }
                }

                result.put(group, props);

                idx++;
            }

            _mixinClones.put(fileName, result);
        }

        return cssRules;
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
}