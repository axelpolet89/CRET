package analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.crawljax.plugins.cret.plugins.analysis.MatchedElements;
import com.crawljax.plugins.cret.util.specificity.SpecificitySelector;
import helpers.TestHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import com.crawljax.plugins.cret.data.MSelector;
import com.crawljax.plugins.cret.util.specificity.Specificity;

public class SpecificityTest
{
	public SpecificityTest()
	{
		DOMConfigurator.configure("log4j.xml");
	}

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

		// direct-child combinator
		AssertSpecificity("div:hover > img:focus", 202);

		// sibling combinator
		AssertSpecificity("div:hover ~ span:focus", 202);

		// direct sibling combinator
		AssertSpecificity("div:hover + span:focus", 202);


		// complex selector
		AssertSpecificity("#test:hover div.class:first-child span[attr=\"test\"]::first-line", 10503);

		System.out.println("[TestSpecificity] Individual specificity computations passed!");
	}

	@Test
	public void TestOrderSpecificity() throws IOException
	{
		List<SpecificitySelector> list = new ArrayList<>();

		list.add(TestHelper.CreateSpecificitySelector("p#242", 1));
		list.add(TestHelper.CreateSpecificitySelector("p p#news", 2));
		list.add(TestHelper.CreateSpecificitySelector("p.algo", 3));
		list.add(TestHelper.CreateSpecificitySelector("span div#aha #cal1", 4));
		list.add(TestHelper.CreateSpecificitySelector("a", 5));
		list.add(TestHelper.CreateSpecificitySelector("span div#aha #cal2", 6));
		list.add(TestHelper.CreateSpecificitySelector("span", 7));
		list.add(TestHelper.CreateSpecificitySelector("A", 8));

		MatchedElements.sortBySpecificity(list);

		Assert.assertEquals("span div#aha #cal2", list.get(0).getSelector().getSelectorText()); 	// defined later than #cal1
		Assert.assertEquals("span div#aha #cal1", list.get(1).getSelector().getSelectorText());
		Assert.assertEquals("span", list.get(list.size() - 2).getSelector().getSelectorText());
		Assert.assertEquals("a", list.get(list.size() - 1).getSelector().getSelectorText()); 		// defined later than span

		System.out.println("[TestOrderSpecificity] Ordering selectors by their specificity passed:");
		for (MSelector s : list.stream().map((ss) -> ss.getSelector()).collect(Collectors.toList()))
		{
			System.out.println("Selector: " + s.getSelectorText());
		}
	}

	private void AssertSpecificity(String selector, int expectedSpecificity) throws IOException
	{
		MSelector mSelector = TestHelper.CreateEmptySelector(selector);
		Specificity sp = mSelector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(expectedSpecificity, sp.getValue());
	}
}
