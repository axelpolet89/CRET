package cssparser;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.crawljax.plugins.cilla.data.MCssRule;
import com.crawljax.plugins.cilla.parser.CssParser;


public class CssParserTest {

	@Test
	public void TestParseMCssRules()
	{
		CssParser parser = new CssParser();

		List<MCssRule> mRules = parser.ParseCssIntoMCssRules("h p { color: red; } " +
																	"div, a, span { font: 20px } " +
																	"#id, .class, span[attr=\"test\"], a:hover, span::before { color: black; } " +
																	"#id div.class span { color: pink; }");
		Assert.assertEquals(4, mRules.size());

		MCssRule mRule = mRules.get(2);
		Assert.assertEquals(5, mRule.GetSelectors().size());

		mRule = mRules.get(3);
		Assert.assertEquals(1, mRule.GetSelectors().size());

		parser = new CssParser();

		mRules = parser.ParseCssIntoMCssRules("h p[att=\"test\" { color: red; } " + //syntax-error
				"div, a, span { font: 20px } " +
				"#id, .class, span[attr=\"test\"], a:hover, span::before { color: black; } " +
				"#id div.class span { color: pink; }");
		Assert.assertEquals(3, mRules.size());

		List<String> parseErrors = parser.GetParseErrors();
		Assert.assertEquals(parseErrors.size(), 1);
		System.out.println("Found expected parse syntax error, and parser filtered incorrect rule");
		System.out.println(parseErrors.get(0));

		parser = new CssParser();

		mRules = parser.ParseCssIntoMCssRules("h p { color: red; } " +
				"div, a, span { font: black } " + //incorrect property value
				"#id, .class, span[attr=\"test\"], a:hover, span::before { color: 11px; } " + //incorrect property value
				"#id div.class span { color: pink; }");
		Assert.assertEquals(4, mRules.size());

		parseErrors = parser.GetParseErrors();
		Assert.assertEquals(parseErrors.size(), 0);
		System.out.println("Parser did not crash on incorrect properties, and did not filter them");
	}
}
