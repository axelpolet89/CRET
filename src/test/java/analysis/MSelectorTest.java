package analysis;

import java.io.IOException;
import java.io.StringReader;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import junit.framework.Assert;

import org.junit.Test;

import com.crawljax.plugins.cilla.data.MSelector;
import com.crawljax.plugins.cilla.util.specificity.Specificity;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;

public class MSelectorTest {

	//TODO: resolve test
//	@Test
//	public void testGetXpath() {
//		MSelector selectorPojo = new MSelector("p#242", null);
//
//		Assert.assertEquals("./descendant::P[@id = '242']", selectorPojo.getXpathSelector());
//
//		selectorPojo = new MSelector("div.news p", null);
//
//		Assert.assertEquals(
//		        "./descendant::DIV[contains(concat(' ', @class, ' '), ' news ')]/descendant::P",
//		        selectorPojo.getXpathSelector());
//
//		Assert.assertFalse(selectorPojo.isMatched());
//
//		selectorPojo.setMatched(true);
//
//		Assert.assertTrue(selectorPojo.isMatched());
//
//		selectorPojo = new MSelector("#UbcMainContent ul", null);
//
//		Assert.assertEquals("./descendant::*[@id = 'UbcMainContent']/descendant::UL",
//		        selectorPojo.getXpathSelector());
//	}

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
	}

	private void AssertSpecificity(String selector, int expectedSpecificity) throws IOException
	{
		MSelector mSelector = new MSelector(CreateSelector(selector));
		Specificity sp = mSelector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(expectedSpecificity, sp.GetValue());
	}

    private Selector CreateSelector(String selector) throws IOException
    {
        InputSource source = new InputSource(new StringReader(selector));
        CSSOMParser cssomParser = new CSSOMParser(new SACParserCSS3());

        return cssomParser.parseSelectors(source).item(0);
    }

//	@Test
//	public void testorderSpecificity() {
//		List<MSelector> list = new ArrayList<MSelector>();
//		list.add(new MSelector("p#242", null));
//		list.add(new MSelector("p p#news", null));
//		list.add(new MSelector("p.algo", null));
//		list.add(new MSelector("span div#aha #cal", null));
//		list.add(new MSelector("a", null));
//		list.add(new MSelector("span span", null));
//		list.add(new MSelector("A", null));
//
//		MSelector.orderSpecificity(list);
//
//		Assert.assertEquals("span div#aha #cal", list.get(0).getSelectorText());
//		Assert.assertEquals("A", list.get(list.size() - 1).getSelectorText());
//
//		for (MSelector s : list) {
//			System.out.println("Selector: " + s.getSelectorText());
//		}
//
//	}
}
