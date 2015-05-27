package analysis;

import com.crawljax.plugins.csssuite.analysis.CssAnalyzer;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.util.CSSDOMHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CssAnalyzerTest
{
	public CssAnalyzerTest()
	{
		DOMConfigurator.configure("log4j.xml");
	}


	@Test
	public void TestCssAnalyzer()
	{
		Document dom = TestHelper.GetDocumentFromFile("./src/test/test_files/cssanalyzer_test_index.html");
		Assert.assertNotNull(dom);

		MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/test_files/cssanalyzer_test_styles.css");
		Assert.assertNotNull(externalFile);

		MCssFile internalFile = TestHelper.GetCssFromString("internal", CSSDOMHelper.ParseEmbeddedStyles(dom));
		Assert.assertNotNull(internalFile);

		HashMap files = new HashMap();
		files.put("external", externalFile);
		files.put("internal", internalFile);

		LinkedHashMap order = new LinkedHashMap();
		order.put("external", 0);
		order.put("internal", 1);

		CssAnalyzer analyzer = new CssAnalyzer();
		analyzer.Transform("", dom, files, order);

		List<MSelector> matchedExternal = new ArrayList<>();
		List<MSelector> matchedInternal = new ArrayList<>();

		for(MCssRule rule : externalFile.GetRules())
		{
			matchedExternal.addAll(rule.GetSelectors().stream().filter(selector -> selector.IsMatched()).collect(Collectors.toList()));
		}

		for(MCssRule rule : internalFile.GetRules())
		{
			matchedInternal.addAll(rule.GetSelectors().stream().filter(selector -> selector.IsMatched()).collect(Collectors.toList()));
		}

		// assert correct amount of internal and external matched rules
		Assert.assertEquals(12, matchedExternal.size());
		Assert.assertEquals(2, matchedInternal.size());
		Assert.assertArrayEquals(Arrays.asList("div#footer", "#footer", "#footer", "#footer", "#wishlist2 li:first-child", "#wishlist2 li:last-child",	"#wishlist3 li a:hover",
													"#wishlist3 li a:hover", "#wishlist3 li a:link", "#wishlist3 li a:visited", "ul li a", "div.input-content").toArray(),
								matchedExternal.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

		Assert.assertArrayEquals(Arrays.asList("ul li a", ".input-content").toArray(),
				matchedInternal.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

		Map<String, MCssFile> postResult = analyzer.Transform(files);

		List<MSelector> effectiveExternal = new ArrayList<>();
		List<MSelector> effectiveInternal = new ArrayList<>();

		for(MCssRule rule : postResult.get("external").GetRules())
		{
			effectiveExternal.addAll(rule.GetSelectors().stream().filter(selector -> selector.HasEffectiveProperties()).collect(Collectors.toList()));
		}

		for(MCssRule rule : postResult.get("internal").GetRules())
		{
			effectiveInternal.addAll(rule.GetSelectors().stream().filter(selector -> selector.HasEffectiveProperties()).collect(Collectors.toList()));
		}

		//assert correct amount of internal and external effective selectors
		Assert.assertEquals(8, effectiveExternal.size());
		Assert.assertEquals(1, effectiveInternal.size());

		Assert.assertArrayEquals(Arrays.asList("div#footer", "#footer", "#footer", "#wishlist2 li:first-child",	"#wishlist3 li a:hover",
						 						"#wishlist3 li a:link", "ul li a", "div.input-content").toArray(),
				effectiveExternal.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

		Assert.assertArrayEquals(Arrays.asList("ul li a").toArray(),
				effectiveInternal.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

		effectiveExternal.clear();
		effectiveInternal.clear();

		for(MCssRule rule : postResult.get("external").GetRules())
		{
			effectiveExternal.addAll(rule.GetSelectors());
		}

		for(MCssRule rule : postResult.get("internal").GetRules())
		{
			effectiveInternal.addAll(rule.GetSelectors());
		}

		// we only have effective selectors, others were filtered by the CssAnalyzer
		Assert.assertEquals(8, effectiveExternal.size());
		Assert.assertEquals(1, effectiveInternal.size());

		List<MProperty> properties = effectiveExternal.get(0).GetProperties();

		// div#footer
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "color");

		// #footer
		properties = effectiveExternal.get(1).GetProperties();
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "background");

		// #footer
		properties = effectiveExternal.get(2).GetProperties();
		Assert.assertEquals(properties.size(), 2);
		Assert.assertEquals(properties.get(0).GetName(), "margin");
		Assert.assertEquals(properties.get(1).GetName(), "padding");

		// #wishlist2 li:first-child
		properties = effectiveExternal.get(3).GetProperties();
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "background-color");

		// #wishlist3 li a:hover
		properties = effectiveExternal.get(4).GetProperties();
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "border");

		// #wishlist3 li a:link
		properties = effectiveExternal.get(5).GetProperties();
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "border");

		// ul li a
		properties = effectiveExternal.get(6).GetProperties();
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "color");

		// ul li a
		properties = effectiveExternal.get(7).GetProperties();
		Assert.assertEquals(properties.size(), 1);
		Assert.assertEquals(properties.get(0).GetName(), "color");

		// INTERNAL ul li a
		properties = effectiveInternal.get(0).GetProperties();
		Assert.assertEquals(properties.size(), 2);
		Assert.assertEquals(properties.get(0).GetName(), "font-size");
		Assert.assertEquals(properties.get(1).GetName(), "display");
	}
}
