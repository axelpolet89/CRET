package analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.util.specificity.Specificity;
import com.crawljax.plugins.csssuite.util.specificity.SpecificityHelper;

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
		List<Pair<MSelector, Integer>> list = new ArrayList<>();

		list.add(new ImmutablePair<>(TestHelper.CreateSelector("p#242", 1), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("p p#news", 2), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("p.algo", 3), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("span div#aha #cal1", 4), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("a", 5), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("span div#aha #cal2", 6), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("span", 7), 1));
		list.add(new ImmutablePair<>(TestHelper.CreateSelector("A", 8), 1));

		//SpecificityHelper.SortBySpecificity(list);

		Assert.assertEquals("span div#aha #cal2", list.get(0).getLeft().GetSelectorText()); 	// defined later than #cal1
		Assert.assertEquals("span div#aha #cal1", list.get(1).getLeft().GetSelectorText());
		Assert.assertEquals("span", list.get(list.size() - 2).getLeft().GetSelectorText());
		Assert.assertEquals("a", list.get(list.size() - 1).getLeft().GetSelectorText()); 		// defined later than span

		System.out.println("[TestOrderSpecificity] Ordering selectors by their specificity passed:");
		for (Pair<MSelector, Integer> s : list) {
		System.out.println("Selector: " + s.getLeft().GetSelectorText());
	}
	}

	private void AssertSpecificity(String selector, int expectedSpecificity) throws IOException
	{
		MSelector mSelector = TestHelper.CreateSelector(selector);
		Specificity sp = mSelector.GetSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(expectedSpecificity, sp.GetValue());
	}
}
