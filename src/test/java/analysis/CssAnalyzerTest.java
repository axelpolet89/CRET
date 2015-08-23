package analysis;

import com.crawljax.plugins.cret.plugins.effectiveness.EffectivenessPlugin;
import com.crawljax.plugins.cret.plugins.matcher.ElementSelectorMatcher;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.plugins.matcher.MatchedElements;
import com.crawljax.plugins.cret.util.CSSDOMHelper;
import helpers.TestHelper;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;
import java.util.stream.Collectors;

public class CssAnalyzerTest
{
	public CssAnalyzerTest()
	{
		DOMConfigurator.configure("log4j.xml");
		LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
	}


	@Test
	public void TestCssAnalyzer()
	{
		Document dom = TestHelper.GetDocumentFromFile("./src/test/resources/cssanalyzer_test_index.html");
		Assert.assertNotNull(dom);

		MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/cssanalyzer_test_styles.css");
		Assert.assertNotNull(externalFile);

		MCssFile internalFile = TestHelper.GetCssFromString("internal", CSSDOMHelper.parseEmbeddedStyles(dom));
		Assert.assertNotNull(internalFile);

		HashMap files = new HashMap();
		files.put("external", externalFile);
		files.put("internal", internalFile);

		LinkedHashMap order = new LinkedHashMap();
		order.put("external", 0);
		order.put("internal", 1);

		MatchedElements matchedElements = new MatchedElements();
		ElementSelectorMatcher.matchElementsToDocument("", dom, files, order, matchedElements);

		List<MSelector> matchedExternal = new ArrayList<>();
		List<MSelector> matchedInternal = new ArrayList<>();

		for(MCssRule rule : externalFile.getRules())
		{
			matchedExternal.addAll(rule.getSelectors().stream().filter(selector -> selector.isMatched()).collect(Collectors.toList()));
		}

		for(MCssRule rule : internalFile.getRules())
		{
			matchedInternal.addAll(rule.getSelectors().stream().filter(selector -> selector.isMatched()).collect(Collectors.toList()));
		}

		// assert correct amount of internal and external matched rules
		Assert.assertEquals(12, matchedExternal.size());
		Assert.assertEquals(2, matchedInternal.size());
		Assert.assertArrayEquals(Arrays.asList("div#footer", "#footer", "#footer", "#footer", "#wishlist2 li:first-child", "#wishlist2 li:last-child",	"#wishlist3 li a:hover",
													"#wishlist3 li a:hover", "#wishlist3 li a:link", "#wishlist3 li a:visited", "ul li a", "div.input-content").toArray(),
								matchedExternal.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

		Assert.assertArrayEquals(Arrays.asList("ul li a", ".input-content").toArray(),
				matchedInternal.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

		Map<String, MCssFile> postResult = new EffectivenessPlugin().transform(files, matchedElements);

		List<MSelector> effectiveExternal = new ArrayList<>();
		List<MSelector> effectiveInternal = new ArrayList<>();

		for(MCssRule rule : postResult.get("external").getRules())
		{
			effectiveExternal.addAll(rule.getSelectors().stream().filter(selector -> selector.hasEffectiveDeclarations()).collect(Collectors.toList()));
		}

		for(MCssRule rule : postResult.get("internal").getRules())
		{
			effectiveInternal.addAll(rule.getSelectors().stream().filter(selector -> selector.hasEffectiveDeclarations()).collect(Collectors.toList()));
		}

		//assert correct amount of internal and external effective selectors
		Assert.assertEquals(8, effectiveExternal.size());
		Assert.assertEquals(1, effectiveInternal.size());

		Assert.assertArrayEquals(Arrays.asList("div#footer", "#footer", "#footer", "#wishlist2 li:first-child",	"#wishlist3 li a:hover",
						 						"#wishlist3 li a:link", "ul li a", "div.input-content").toArray(),
				effectiveExternal.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

		Assert.assertArrayEquals(Arrays.asList("ul li a").toArray(),
				effectiveInternal.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

		effectiveExternal.clear();
		effectiveInternal.clear();

		for(MCssRule rule : postResult.get("external").getRules())
		{
			effectiveExternal.addAll(rule.getSelectors());
		}

		for(MCssRule rule : postResult.get("internal").getRules())
		{
			effectiveInternal.addAll(rule.getSelectors());
		}

		// we only have effective selectors, others were filtered by the CssAnalyzer
		Assert.assertEquals(8, effectiveExternal.size());
		Assert.assertEquals(1, effectiveInternal.size());

		List<MDeclaration> mDeclarations = effectiveExternal.get(0).getDeclarations();

		// div#footer
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "color");

		// #footer
		mDeclarations = effectiveExternal.get(1).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "background");

		// #footer
		mDeclarations = effectiveExternal.get(2).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 2);
		Assert.assertEquals(mDeclarations.get(0).getName(), "margin");
		Assert.assertEquals(mDeclarations.get(1).getName(), "padding");

		// #wishlist2 li:first-child
		mDeclarations = effectiveExternal.get(3).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "background-color");

		// #wishlist3 li a:hover
		mDeclarations = effectiveExternal.get(4).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "border");

		// #wishlist3 li a:link
		mDeclarations = effectiveExternal.get(5).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "border");

		// ul li a
		mDeclarations = effectiveExternal.get(6).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "color");

		// ul li a
		mDeclarations = effectiveExternal.get(7).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 1);
		Assert.assertEquals(mDeclarations.get(0).getName(), "color");

		// INTERNAL ul li a
		mDeclarations = effectiveInternal.get(0).getDeclarations();
		Assert.assertEquals(mDeclarations.size(), 2);
		Assert.assertEquals(mDeclarations.get(0).getName(), "font-size");
		Assert.assertEquals(mDeclarations.get(1).getName(), "display");
	}


	@Test
	public void TestMediaQueries()
	{
		Document dom = TestHelper.GetDocumentFromFile("./src/test/resources/cssanalyzer_test_index.html");
		Assert.assertNotNull(dom);

		MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/cssanalyzer_test_media.css");
		Assert.assertNotNull(externalFile);

		HashMap files = new HashMap();
		files.put("external", externalFile);

		LinkedHashMap order = new LinkedHashMap();
		order.put("external", 0);

		MatchedElements matchedElements = new MatchedElements();
		ElementSelectorMatcher.matchElementsToDocument("", dom, files, order, matchedElements);

		List<MSelector> matchedExternal = new ArrayList<>();

		for(MCssRule rule : externalFile.getRules())
		{
			matchedExternal.addAll(rule.getSelectors().stream().filter(selector -> selector.isMatched()).collect(Collectors.toList()));
		}

		// assert correct amount of external matched rules
		Assert.assertEquals(7, matchedExternal.size());

		Assert.assertArrayEquals(Arrays.asList("div#footer", "div#footer", "div#footer", "body .extra-content", ".extra-content", "body .extra-content", ".extra-content").toArray(),
				matchedExternal.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

		Map<String, MCssFile> postResult = new EffectivenessPlugin().transform(files, matchedElements);

		List<MSelector> effectiveExternal = new ArrayList<>();
		for(MCssRule rule : postResult.get("external").getRules())
		{
			effectiveExternal.addAll(rule.getSelectors().stream().filter(selector -> selector.hasEffectiveDeclarations()).collect(Collectors.toList()));
		}

		//assert correct amount of internal and external effective selectors
		Assert.assertEquals(6, effectiveExternal.size());

		Assert.assertArrayEquals(Arrays.asList("div#footer", "div#footer", "div#footer", "body .extra-content", "body .extra-content", ".extra-content").toArray(),
				effectiveExternal.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

		for(int i = 0; i < effectiveExternal.size(); i++)
		{
			MSelector sel = effectiveExternal.get(i);

			if (i == 5)
			{
				Assert.assertEquals(1, sel.getDeclarations().size());
				Assert.assertEquals("color", sel.getDeclarations().get(0).getName());
			}
			else
			{
				for (MDeclaration prop : sel.getDeclarations())
				{
					Assert.assertTrue(prop.isEffective());
				}
			}
		}
	}
}
