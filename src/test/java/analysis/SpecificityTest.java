package analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.crawljax.plugins.cilla.util.specificity.SpecificityHelper;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import junit.framework.Assert;

import org.junit.Test;

import com.crawljax.plugins.cilla.data.MSelector;
import com.crawljax.plugins.cilla.util.specificity.Specificity;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;

public class SpecificityTest {

	@Test
	public void TestSpecificity() throws IOException
    {
		//single element type
		AssertSpecificity("div", 1);

		//single id
		AssertSpecificity("#red", 10000);

		//single class
		AssertSpecificity(".red", 100);

		// http://www.w3.org/TR/css3-selectors/#specificity
		AssertSpecificity("*", 0);
		AssertSpecificity("li", 1);
		AssertSpecificity("ul li", 2);
		AssertSpecificity("ul ol+li", 3);
		AssertSpecificity("h1 + *[REL=up]", 101);
		AssertSpecificity("ul ol li.red", 103);
		AssertSpecificity("li.red.level", 201);
		AssertSpecificity("#x34y", 10000);
		AssertSpecificity("#s12:not(li)", 10001);

		//multiple attributes
		AssertSpecificity("span[hello=\"Cleveland\"][goodbye=\"Columbus\"][after=\"Orleans\"]", 301);

		//multiple attributes - chained
		AssertSpecificity("span[hello=\"Cleveland\"][goodbye=\"Columbus\"][after=\"Orleans\"] p[hello=\"Cleveland\"]", 402);

		//combination
		AssertSpecificity("div span#news .item", 10102);

		// structural pseudo
		AssertSpecificity("div:first-child",101);
		AssertSpecificity("#test:last-child", 10100);

		// non-structural pseudo
		AssertSpecificity("#test:hover", 10100);
		AssertSpecificity(".test:hover", 200);

		// pseudo-element
		AssertSpecificity("#test::before", 10001);
		AssertSpecificity(".test::after", 101);

		// complex selector
		AssertSpecificity("#test:hover div.class:first-child span[attr=\"test\"]::first-line", 10503);

		System.out.println("[TestSpecificity] Individual specificity computations passed!");
	}

	@Test
	public void TestOrderSpecificity() throws IOException {
		List<MSelector> list = new ArrayList<>();

		list.add(CreateSelector("p#242", 1));
		list.add(CreateSelector("p p#news", 2));
		list.add(CreateSelector("p.algo", 3));
		list.add(CreateSelector("span div#aha #cal1", 4));
		list.add(CreateSelector("a", 5) );
		list.add(CreateSelector("span div#aha #cal2", 6));
		list.add(CreateSelector("span", 7));
		list.add(CreateSelector("A", 8));

		SpecificityHelper.OrderSpecificity(list);

		Assert.assertEquals("span div#aha #cal2", list.get(0).GetSelectorText()); 	// defined later than #cal1
		Assert.assertEquals("span div#aha #cal1", list.get(1).GetSelectorText());
		Assert.assertEquals("span", list.get(list.size() - 2).GetSelectorText());
		Assert.assertEquals("a", list.get(list.size() - 1).GetSelectorText()); 		// defined later than span

		System.out.println("[TestOrderSpecificity] Ordering selectors by their specificity passed:");
		for (MSelector s : list) {
			System.out.println("Selector: " + s.GetSelectorText());
		}
	}

	private void AssertSpecificity(String selector, int expectedSpecificity) throws IOException
	{
		MSelector mSelector = CreateSelector(selector);
		Specificity sp = mSelector.GetSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(expectedSpecificity, sp.GetValue());
	}

    private MSelector CreateSelector(String selector) throws IOException
    {
        return new MSelector(ParseSelector(selector), 1);
    }

	private MSelector CreateSelector(String selector, int ruleNumber) throws IOException
	{
		return new MSelector(ParseSelector(selector), ruleNumber);
	}

	private Selector ParseSelector(String selector) throws IOException
	{
		InputSource source = new InputSource(new StringReader(selector));
		CSSOMParser cssomParser = new CSSOMParser(new SACParserCSS3());
		return cssomParser.parseSelectors(source).item(0);
	}
}
