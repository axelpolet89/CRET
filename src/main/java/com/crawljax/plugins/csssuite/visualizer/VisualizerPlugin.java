package com.crawljax.plugins.csssuite.visualizer;

import java.util.List;
import java.util.Map;

import com.crawljax.plugins.csssuite.data.ElementWithClass;
import com.crawljax.plugins.csssuite.data.MCssRule;
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
