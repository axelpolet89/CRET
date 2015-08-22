package analysis;

import java.io.IOException;
import java.util.List;

import helpers.TestHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;
import se.fishtank.css.selectors.parser.ParserException;

import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.util.DomUtils;

public class MatchedElementsTest
{
	public MatchedElementsTest()
	{
		DOMConfigurator.configure("log4j.xml");
	}

	@Test
	public void testCssSelectors() throws IOException
	{
		String html =
		        "<html>"
		                + "<head><title>example</title> \n"
		                + "<link href='basic.css' rel='stylesheet' type='text/css'>"
		                + "<style> \n"
		                + ".newsletter { color: bla;} #world {dec: 234 }"
		                + "</style>"
		                + "<style>"
		                + ".nrc { font: bold;}"
		                + "</style>"
		                + "</head>"
		                +

		                "<body>"
		                + "  <div width='56' id='div1' class='votebutton medium green'>"
		                + "  <a href=`google.com'>googooli</a>"
		                + "  <p id='24' class=''>this is just a test</p>"
						+ "</div>"

						+ "<div id='div2'> <p>bla</p> <p>blabla</p> </div>"

						+ "<div id='div3'> <img>direct-child</img>  <span><img>indirect child (descendant)</img></span> </div>"
						+ "<span>direct sibling</span>"
						+ "<span>indirect sibling</span>"
						+ "</body>" +
				"</html>";

		//use crawljax utility class to get a Document object
		Document dom = DomUtils.asDocument(html);

		//selectors class, allows us to query DOM
		Selectors seSelectors = new Selectors(new W3CNode(dom));

		List result = seSelectors.querySelectorAll("div");
		Assert.assertEquals(3, result.size());

		result = seSelectors.querySelectorAll("#div1");
		Assert.assertEquals(1, result.size());

		result = seSelectors.querySelectorAll( ".votebutton.medium.green");
		Assert.assertEquals(1, result.size());

		result = seSelectors.querySelectorAll("#div1 a, #div1 p");
		Assert.assertEquals(2, result.size());

		// descendant combinator
		result = seSelectors.querySelectorAll("#div3 img");
		Assert.assertEquals(2, result.size());

		// direct-child combinator
		result = seSelectors.querySelectorAll("#div3 > img");
		Assert.assertEquals(1, result.size());

		// sibling combinator
		result = seSelectors.querySelectorAll("#div3 ~ span");
		Assert.assertEquals(2, result.size());

		// direct sibling combinator
		result = seSelectors.querySelectorAll("#div3 + span");
		Assert.assertEquals(1, result.size());

		// does not exist
		result = seSelectors.querySelectorAll("#div4");
		Assert.assertEquals(0, result.size());

		// structural pseudo-selector
		result = seSelectors.querySelectorAll("#div2 p:first-child");
		Assert.assertEquals(1, result.size());

		// pseudo-element
		result = seSelectors.querySelectorAll("#div2 p::first-letter");
		Assert.assertEquals(2, result.size());

		// non-structural pseudo-selector
		result = seSelectors.querySelectorAll("#div2:hover");
		Assert.assertEquals(0, result.size());

		// non-structural pseudo-selector via MSelector (used in CssAnalyzer)
		MSelector mSelector = TestHelper.CreateEmptySelector("#div2:hover");
		result = seSelectors.querySelectorAll(mSelector.getFilteredSelectorText());
		Assert.assertEquals(1, result.size());																					// it will match
		List<Node> nodes = (List<Node>)result;
		Assert.assertTrue(mSelector.checkPseudoCompatibility(nodes.get(0).getNodeName(), nodes.get(0).getAttributes()));		// it will be compatible

		// non-structural pseudo-selector via MSelector, but not compatible (div with visited)
		mSelector = TestHelper.CreateEmptySelector("#div2:visited");
		result = seSelectors.querySelectorAll(mSelector.getFilteredSelectorText());
		Assert.assertEquals(1, result.size());																					// it will match
		nodes = (List<Node>)result;
		Assert.assertFalse(mSelector.checkPseudoCompatibility(nodes.get(0).getNodeName(), nodes.get(0).getAttributes()));		// it will not be compatible

		try
		{
			result = seSelectors.querySelectorAll("div:not(#div2)");
		}
		catch (ParserException ex)
		{
			Assert.fail(ex.getMessage());
		}
	}
}
