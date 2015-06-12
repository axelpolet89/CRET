package com.crawljax.plugins.csssuite.sass.clonedetection.items;

/**
 * Created by axel on 6/5/2015.
 */
public class PropertyItemSetList extends ItemSetList {
    // Use template method design pattern
    @Override
    protected String getRepresentativeItemString(Item item) {
        return item.getFirstDeclaration().getProperty().toString();
    }
}
