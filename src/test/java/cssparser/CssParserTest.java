package cssparser;

import java.util.List;

import com.crawljax.plugins.csssuite.data.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;

import org.junit.Assert;
import org.junit.Test;

import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.parser.CssParser;

public class CssParserTest {

	@Test
	public void TestParseRules()
	{
		CssParser parser = new CssParser();

		List<MCssRule> mRules = parser.ParseCssIntoMCssRules("h p { color: red; } " +
																	"div, a, span { font: 20px } " +
																	"#id, .class, span[attr=\"test\"], a:hover, span::before { color: black; } " +
																	"#id div.class span { color: pink; }");

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());
		Assert.assertEquals(4, mRules.size());

		MCssRule mRule = mRules.get(2);
		Assert.assertEquals(5, mRule.GetSelectors().size());

		mRule = mRules.get(3);
		Assert.assertEquals(1, mRule.GetSelectors().size());

		parser = new CssParser();

		mRules = parser.ParseCssIntoMCssRules("h p[att=\"test\" { color: red; } " + //syntax-error, should ignore
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
				"div, a, span { font: black } " + //incorrect property value, should not ignore
				"#id, .class, span[attr=\"test\"], a:hover, span::before { color: 11px; } " + //incorrect property value, should not ignore
				"#id div.class span { color: pink; }");
		Assert.assertEquals(4, mRules.size());

		parseErrors = parser.GetParseErrors();
		Assert.assertEquals(parseErrors.size(), 0);
	}

	@Test
	public void TestParseLocator()
	{
		CssParser parser = new CssParser();

		List<MCssRule> rules =  parser.ParseCssIntoMCssRules("h p { color: red;} \n\n div { font: black} \n span {color: white}");

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		MCssRule mRule = rules.get(0);
		Assert.assertEquals(1, mRule.GetLocator().getLineNumber());

		mRule = rules.get(1);
		Assert.assertEquals(3, mRule.GetLocator().getLineNumber());

		mRule = rules.get(2);
		Assert.assertEquals(4, mRule.GetLocator().getLineNumber());
	}

	@Test
	public void TestParseSelector()
	{
		CssParser parser = new CssParser();
		List<MCssRule> rules = parser.ParseCssIntoMCssRules("h p { color: red;}\n" +
				"div, a, span { font: black}\n" +
				".class:hover {font-size:20px;}\n" +
				".class:first-child {font-size:20px;}\n" +
				"div .class::before {color:white;}\n" +
				"div > .class::before {color:white;}\n" +
				"div + .class::before {color:white;}\n" +
				"div ~ .class::before {color:white;}\n" +
				"div .class:not(:hover) { color:pink;}" +
				".class:hover div:focus #id:visited { color: purple; }\n" );

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = rules.get(0);

		List<MSelector> selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		MSelector selector = selectors.get(0);
		Assert.assertEquals("h p", selector.GetSelectorText());
		Assert.assertFalse(selector.IsMatched());
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertFalse(selector.IsIgnored());

		//second rule
		mRule = rules.get(1);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(3, selectors.size());

		selector = selectors.get(1);
		Assert.assertEquals("a", selector.GetSelectorText());

		//third rule
		mRule = rules.get(2);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.IsNonStructuralPseudo());
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertEquals(".class:hover", selector.GetSelectorText());
		Assert.assertEquals(".class", selector.GetFilteredSelectorText()); // special variant for querying DOM

		//fourth rule
		mRule = rules.get(3);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo()); // this is structural
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertEquals(".class:first-child", selector.GetSelectorText());
		Assert.assertEquals(".class:first-child", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//fifth rule
		mRule = rules.get(4);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div .class:before", selector.GetSelectorText());
		Assert.assertEquals("div .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//sixth rule
		mRule = rules.get(5);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div > .class:before", selector.GetSelectorText());
		Assert.assertEquals("div > .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//seventh rule
		mRule = rules.get(6);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div + .class:before", selector.GetSelectorText());
		Assert.assertEquals("div + .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//eight rule
		mRule = rules.get(7);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div ~ .class:before", selector.GetSelectorText());
		Assert.assertEquals("div ~ .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//ninth rule
		mRule = rules.get(8);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.IsIgnored()); //should be ignored because of :not

		//tenth rule
		mRule = rules.get(9);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.IsNonStructuralPseudo());
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertEquals(".class:hover div:focus #id:visited", selector.GetSelectorText());
		Assert.assertEquals(":visited", selector.GetPseudoClass());
	}


	@Test
	public void TestParseProperties()
	{
		CssParser parser = new CssParser();
		List<MCssRule> rules = parser.ParseCssIntoMCssRules (
				"div .class { color:white; border: 1px solid black; font-size:10px !important;}\n" +
				"div, a, span { font-size: black; display:block; }\n" +
				"div {color: #000; background-color: #ffffff; }");

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = rules.get(0);

		MSelector mSelector = mRule.GetSelectors().get(0);

		List<MProperty> mProperties = mSelector.GetProperties();
		Assert.assertEquals(3, mProperties.size());

		Assert.assertEquals("color", mProperties.get(0).GetName());
		Assert.assertEquals("white", mProperties.get(0).GetValue());
		Assert.assertFalse(mProperties.get(0).IsImportant());

		Assert.assertEquals("border", mProperties.get(1).GetName());
		Assert.assertEquals("1px solid black", mProperties.get(1).GetValue());
		Assert.assertFalse(mProperties.get(1).IsImportant());

		Assert.assertEquals("font-size", mProperties.get(2).GetName());
		Assert.assertTrue(mProperties.get(2).IsImportant());

		for(MProperty mProp : mProperties)
			Assert.assertFalse(mProp.IsEffective());


		//second rule
		mRule = rules.get(1);

		for(MSelector mSel : mRule.GetSelectors())
			Assert.assertEquals(2, mSel.GetProperties().size());

		mSelector = mRule.GetSelectors().get(0);
		mProperties = mSelector.GetProperties();

		Assert.assertEquals("font-size", mProperties.get(0).GetName());
		Assert.assertEquals("black", mProperties.get(0).GetValue());
		Assert.assertFalse(mProperties.get(0).IsImportant());


		//third rule
		mRule = rules.get(2);
		mSelector = mRule.GetSelectors().get(0);
		mProperties = mSelector.GetProperties();

		Assert.assertEquals("color", mProperties.get(0).GetName());
		Assert.assertEquals("rgb(0, 0, 0)", mProperties.get(0).GetValue());

		Assert.assertEquals("background-color", mProperties.get(1).GetName());
		Assert.assertEquals("rgb(255, 255, 255)", mProperties.get(1).GetValue());
	}
}
