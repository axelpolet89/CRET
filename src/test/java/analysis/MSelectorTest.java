package analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.crawljax.plugins.cilla.util.CssParser;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import junit.framework.Assert;

import org.junit.Test;

import com.crawljax.plugins.cilla.analysis.MSelector;
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
		MSelector selector = new MSelector(CreateSelector("p#242"));
		Specificity sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(10001, sp.getValue());

		selector = new MSelector(CreateSelector("p.newsitem"));
		sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(101, sp.getValue());

		selector = new MSelector(CreateSelector(".newsitem"));
		sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(100, sp.getValue());

		selector = new MSelector(CreateSelector("#newsitem"));
		sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(10000, sp.getValue());

		selector = new MSelector(CreateSelector("div"));
		sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(1, sp.getValue());

		selector = new MSelector(CreateSelector("div div"));
		sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(2, sp.getValue());

		selector = new MSelector(CreateSelector("div span#news .item"));
		sp = selector.getSpecificity();

		Assert.assertNotNull(sp);
		Assert.assertEquals(10102, sp.getValue());
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
