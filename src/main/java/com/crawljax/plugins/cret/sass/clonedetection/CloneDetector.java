package com.crawljax.plugins.cret.sass.clonedetection;

import java.util.*;
import java.util.stream.Collectors;

import com.crawljax.plugins.cret.cssmodel.MSelector;

import com.crawljax.plugins.cret.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.cret.sass.clonedetection.fpgrowth.FPGrowth;
import com.crawljax.plugins.cret.sass.clonedetection.items.Item;
import com.crawljax.plugins.cret.sass.clonedetection.items.ItemSet;
import com.crawljax.plugins.cret.sass.clonedetection.items.ItemSetList;

/**
 * Created by axel on 5/5/2015.
 *
 * Implementation of detecting clones in CSS declarations,
 * based on FP-growth solution in https://github.com/dmazinanian/css-analyser
 */
public class CloneDetector
{
    /**
     * Implementation taken from https://github.com/dmazinanian/css-analyser
     * and adapted to generate SASS mixins from groups of cloned declarations
     * which are detected using an FP-growth algorithm to find associations
     *
     * 1) Takes the largest cloned declaration group, and generate a mixin from it
     * 2) Removes the declarations contained in the mixin from the CSS selectors it was taken from
     * 3) Rerun clone detection until no more groups of cloned declarations are found
     *
     * @return list of SASS mixins extraced from groups of cloned declarations
     */
    public List<SassCloneMixin> generateMixinsFromClones(List<MSelector> selectors)
    {
        List<SassCloneMixin> templates = new ArrayList<>();
        List<MSelector> allSelectors = new ArrayList<>(selectors);

        List<ItemSetList> results = findDuplicationsAndFpGrowth(allSelectors);

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

                    for (ClonedDeclaration d : i)
                    {
                        newSelectors.add(d.getSelector());
                    }

                    Optional<SassCloneMixin> existing = innerTemplates.stream().filter(t -> t.sameSelectors(newSelectors)).findFirst();

                    if (existing.isPresent())
                    {
                        boolean pSet = false;
                        for (ClonedDeclaration d : i)
                        {
                            if (!pSet)
                            {
                                pSet = true;
                                existing.get().addDeclaration(d.getProperty());
                            }
                        }
                    }
                    else
                    {
                        SassCloneMixin template = new SassCloneMixin();

                        newSelectors.forEach(s -> template.addSelector(s));

                        boolean pSet = false;
                        for (ClonedDeclaration d : i)
                        {
                            if (!pSet)
                            {
                                pSet = true;
                                template.addDeclaration(d.getProperty());
                            }
                        }

                        innerTemplates.add(template);
                    }
                }

                templates.addAll(innerTemplates);

                for(SassCloneMixin template : innerTemplates)
                {
                    for(MSelector mSel : allSelectors.stream().filter(s -> template.getRelatedSelectors().contains(s)).collect(Collectors.toList()))
                    {
                        mSel.removeDeclarationsByText(template.getDeclarations());
                    }
                }

                results = findDuplicationsAndFpGrowth(allSelectors);
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
     * and adapted to be applied on MSelectors and MDeclarations
     */
    private List<ItemSetList> findDuplicationsAndFpGrowth(List<MSelector> selectors)
    {
        Map<ClonedDeclaration, Item> declarationItemMap = new HashMap<>();

        List<ClonedDeclaration> declarations = new ArrayList<>();

        selectors.stream().forEach(s -> s.getDeclarations().forEach(p -> declarations.add(new ClonedDeclaration(p, s))));

        Set<Integer> visitedDeclarations = new HashSet<>();

        int currentDeclarationIndex = -1;

        TypeOneDuplicationInstance typeOneDuplication = new TypeOneDuplicationInstance();
        List<TypeOneDuplicationInstance> duplicationInstanceList = new ArrayList<>();

        while (++currentDeclarationIndex < declarations.size())
        {
            ClonedDeclaration currentDeclaration = declarations.get(currentDeclarationIndex);

            int checkingDecIndex = currentDeclarationIndex;

            /*
             * We want to keep all current identical declarations together, and
             * then add them to the duplications list when we found all
             * identical declarations of current declaration.
             */
            List<ClonedDeclaration> currentTypeIDuplicatedDeclarations = new ArrayList<>();

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
                ClonedDeclaration checkingDeclaration = declarations.get(checkingDecIndex);

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
            for (ClonedDeclaration decl : declarations.stream().filter(d -> d.getSelector() == s).collect(Collectors.toList()))
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