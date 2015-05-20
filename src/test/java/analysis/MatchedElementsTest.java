package analysis;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import com.crawljax.plugins.cilla.data.MSelector;
import com.crawljax.util.DomUtils;

import org.w3c.dom.Node;
import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;
import se.fishtank.css.selectors.parser.ParserException;

public class MatchedElementsTest {

	@Test
	public void testCssSelectors() throws IOException {

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
		                + "  <span id='span1' class='news'/>"
						+ "<div id='div2'> <p>bla</p> <p>blabla</p> </div>"
						+ "</body>" +
				"</html>";

		//use crawljax utility class to get a Document object
		Document dom = DomUtils.asDocument(html);

		//selectors class, allows us to query DOM
		Selectors seSelectors = new Selectors(new W3CNode(dom));

		List<Node> result = seSelectors.querySelectorAll("div");
		Assert.assertEquals(2, result.size());

		result = seSelectors.querySelectorAll("#div1");
		Assert.assertEquals(1, result.size());

		result = seSelectors.querySelectorAll( ".votebutton.medium.green");
		Assert.assertEquals(1, result.size());

		result = seSelectors.querySelectorAll("#div1 a, #div1 p");
		Assert.assertEquals(2, result.size());

		// does not exist
		result = seSelectors.querySelectorAll("#div3");
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
		MSelector mSelector = TestHelper.CreateSelector("#div2:hover");
		result = seSelectors.querySelectorAll(mSelector.GetFilteredSelectorText());
		Assert.assertEquals(1, result.size());																					// it will match
		Assert.assertTrue(mSelector.CheckPseudoCompatibility(result.get(0).getNodeName(), result.get(0).getAttributes()));		// it will be compatible

		// non-structural pseudo-selector via MSelector, but not compatible (div with visited)
		mSelector = TestHelper.CreateSelector("#div2:visited");
		result = seSelectors.querySelectorAll(mSelector.GetFilteredSelectorText());
		Assert.assertEquals(1, result.size());																					// it will match
		Assert.assertFalse(mSelector.CheckPseudoCompatibility(result.get(0).getNodeName(), result.get(0).getAttributes()));		// it will not be compatible

		//todo: fix :not handling
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
