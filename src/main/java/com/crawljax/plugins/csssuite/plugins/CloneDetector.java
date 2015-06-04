package com.crawljax.plugins.csssuite.plugins;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import org.apache.log4j.Logger;

/**
 * Created by axel on 5/5/2015.
 * Initial clonedetector, differs from css-analyser tool, less bloated
 * This class is able to find duplicates by matching properties and values inside the css rules from a given file
 * For each property a CloneSet is created, which holds that property and two or more rules in which it prevails
 * Based on the rules inside any cloneset, we check if those rules exist in other clonesets.
 * If they do, then more than one property is shared by those rules
 */
public class CloneDetector {

    private static final Logger LOGGER = Logger.getLogger(CloneDetector.class.getName());

    private String _ccsFile;

    //clones per property(name-value)
    private List<CloneSet> _allClones;

    //clones per set of rules, possibly more than 1 shared MProperty per ruleset
    private HashMap<List<MCssRule>, List<MProperty>> _mixinClones;

    public CloneDetector(String cssFile)
    {
        _ccsFile = cssFile;
    }


    /**
     *
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
    public String GetFile(){
        return _ccsFile;
    }


    /**
     *
     * @param rules
     */
    public void Detect(List<MCssRule> rules)
    {
        //allows for fast detection of clones (using hash compare on the keys)
        HashMap<String, CloneSet> clones = new HashMap<>();

        for(MCssRule mRule : rules)
        {
//            for(MProperty mProp : mRule.ParseProperties())
//            {
//                String key = PropertyToString(mProp);
//                if(clones.containsKey(key)) {
//                    clones.get(key).AddRuleToSet(mRule);
//                }
//                else
//                {
//                    clones.put(key, new CloneSet(mProp));
//                    clones.get(key).AddRuleToSet(mRule);
//                }
//            }
        }

        _allClones = clones.values().stream().filter(cloneSet -> cloneSet.GetRules().size() > 1).collect(Collectors.toList());

        HashMap<List<MCssRule>, List<MProperty>> result = new HashMap<>();

        int idx = 1;
        for(CloneSet cloneSet : _allClones){

            List<MCssRule> group = cloneSet.GetRules();
            if(result.containsKey(group)) {
                idx++;
                continue;
            }

            List<MProperty> props = new ArrayList<>();
            props.add(cloneSet.GetProperty());

            for(int i = idx; i < _allClones.size(); i++)
            {
                CloneSet otherCloneSet = _allClones.get(i);
                //todo: still need to develop way by which some(at least 2) selectors match between clonesets
                if(otherCloneSet.GetSelectors().containsAll(cloneSet.GetSelectors()))
                {
                    props.add(otherCloneSet.GetProperty());
                }
            }

            result.put(group, props);

            idx++;
        }

        _mixinClones = result;
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
        StringBuilder result = new StringBuilder();

        for(CloneSet cloneSet : _allClones)
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Clone: " + PropertyToString(cloneSet.GetProperty()) + "\n");
            for(MCssRule mRule : cloneSet.GetRules())
            {
                builder.append("Rule: " + mRule.toString() + "\n");
            }
            result.append(builder.toString() + "\n\n");
        }

        for (List<MCssRule> key : _mixinClones.keySet()) {
            StringBuilder builder = new StringBuilder();
            final String[] s = {""};
            key.forEach((MCssRule) -> s[0] += "in rule: " + MCssRule.toString() + "\n");

            builder.append("Shared Properties Found!\n");
            builder.append(s[0]);
            for(MProperty mProp : _mixinClones.get(key))
            {
                builder.append("on property: " + mProp.toString() + "\n");
            }
            result.append(builder.toString() + "\n\n");
        }

        return result.toString();
    }


    /**
     * Wrapper for a MProperty that is shared accross multple css rules
     */
    private class CloneSet{
        private MProperty _mProperty;
        private List<MCssRule> _mRules;

        public CloneSet(MProperty mProp){
            _mProperty = mProp;
            _mRules = new ArrayList<>();
        }

        public List<MSelector> GetSelectors() {
            List<MSelector> result = new ArrayList<>();
            for(MCssRule mRule : _mRules)
                result.addAll(mRule.GetSelectors());
            return result;
        }

        public List<MCssRule> GetRules(){
            return _mRules;
        }

        public void AddRuleToSet(MCssRule mRule)
        {
            _mRules.add(mRule);
        }

        public MProperty GetProperty() {
            return _mProperty;
        }
    }
}