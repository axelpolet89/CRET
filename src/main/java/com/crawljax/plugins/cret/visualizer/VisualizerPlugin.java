package com.crawljax.plugins.cret.visualizer;

import java.util.List;
import java.util.Map;

import com.crawljax.plugins.cret.data.ElementWithClass;
import com.crawljax.plugins.cret.data.MCssRule;
import com.google.common.collect.SetMultimap;

/*
 * The com.crawljax.plugins.csssuite.visualizer plug-in
 */
public interface VisualizerPlugin {
	/**
	 * 
	 * @param summary
	 * @param cssRules
	 * @param elementsWithNoClassDef
	 */
	void openVisualizer(String url, String summary, Map<String, List<MCssRule>> cssRules,
	        SetMultimap<String, ElementWithClass> elementsWithNoClassDef);

}
