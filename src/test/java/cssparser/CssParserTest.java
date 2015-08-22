package cssparser;

import java.util.List;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.data.MSelector;

import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.parser.CssParser;

public class CssParserTest
{
	public CssParserTest()
	{
		DOMConfigurator.configure("log4j.xml");
	}

	@Test
	public void TestParseRules()
	{
		CssParser parser = new CssParser();

		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test", "h p { color: red; } " +
			"div, a, span { font: 20px } " +
			"#id, .class, span[attr=\"test\"], a:hover, span::before { color: black; } " +
			"#id div.class span { color: pink; }");

		List <MCssRule> mRules = mCssFile.GetRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());
		Assert.assertEquals(4, mRules.size());

		MCssRule mRule = mRules.get(2);
		Assert.assertEquals(5, mRule.GetSelectors().size());

		mRule = mRules.get(3);
		Assert.assertEquals(1, mRule.GetSelectors().size());

		/* filtering rules and properties with incorrect syntax */
		parser = new CssParser(false);
		mCssFile = parser.ParseCssIntoMCssRules("test",
				"h p[att=\"test\" { color: red; } " + //syntax-error, should ignore entire rule
				"div, h p[att=\"test\", span { color: red; }" + //syntax-error, should ignore entire rule including other selectors
				"div, a, span { font  20px; margin: 10px; }" + //syntax-error, should ignore declaration
				"#id, .class, span[attr=\"test\"], a:hover, span::before { color: black; }" +
				"#id div.class span { color: pink; }");

		mRules = mCssFile.GetRules();
		Assert.assertEquals(3, mRules.size());

		// only 1 declaration parsed for third rule (first rule in list)
		Assert.assertEquals(1, mRules.get(0).GetSelectors().get(0).GetDeclarations().size());

		List<String> parseErrors = parser.GetParseErrors();
		Assert.assertEquals(parseErrors.size(), 3);
		System.out.println("Found expected parse errors, and parser filtered incorrect rule and incorrect declaration");
		System.out.println(parseErrors.get(0));
		System.out.println(parseErrors.get(1));
		System.out.println(parseErrors.get(2));


		/* SHOULD parse incorrect properties */
		parser = new CssParser();
		mCssFile = parser.ParseCssIntoMCssRules("test", "h p { color: red; } " +
				"div, a, span { margin: 5 px 10px 20 px 30px } " + //incorrect declaration value, should not ignore
				"#id, .class, span[attr=\"test\"], a:hover, span::before { color: 11px; } " + //incorrect declaration value, should not ignore
				"#id div.class span { color: pink; }");
		mRules = mCssFile.GetRules();
		Assert.assertEquals(4, mRules.size());

		parseErrors = parser.GetParseErrors();
		Assert.assertEquals(parseErrors.size(), 0);
	}

	@Test
	public void TestParseLocator()
	{
		CssParser parser = new CssParser();
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test", "h p { color: red;} \n\n div { font: black} \n span {color: white}");
		List<MCssRule> rules = mCssFile.GetRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		MCssRule mRule = rules.get(0);
		Assert.assertEquals(1, mRule.GetLineNumber());

		mRule = rules.get(1);
		Assert.assertEquals(3, mRule.GetLineNumber());

		mRule = rules.get(2);
		Assert.assertEquals(4, mRule.GetLineNumber());
	}

	@Test
	public void TestParseSelector()
	{
		CssParser parser = new CssParser();
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test", "h p { color: red;}\n" +
				"div, a, span { font: black}\n" +
				".class:hover {font-size:20px;}\n" +
				".class:first-child {font-size:20px;}\n" +
				"div .class::before {color:white;}\n" +
				"div > .class::before {color:white;}\n" +
				"div + .class::before {color:white;}\n" +
				"div ~ .class::before {color:white;}\n" +
				"div .class:not(:hover) { color:pink;}" +
				".class:hover div:focus #id:visited { color: purple; }\n");

		List<MCssRule> mRules = mCssFile.GetRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = mRules.get(0);

		List<MSelector> selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		MSelector selector = selectors.get(0);
		Assert.assertEquals("h p", selector.GetSelectorText());
		Assert.assertFalse(selector.IsMatched());
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertFalse(selector.IsIgnored());

		//second rule
		mRule = mRules.get(1);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(3, selectors.size());

		selector = selectors.get(1);
		Assert.assertEquals("a", selector.GetSelectorText());

		//third rule
		mRule = mRules.get(2);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.IsNonStructuralPseudo());
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertEquals(".class:hover", selector.GetSelectorText());
		Assert.assertEquals(".class", selector.GetFilteredSelectorText()); // special variant for querying DOM

		//fourth rule
		mRule = mRules.get(3);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo()); // this is structural
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertEquals(".class:first-child", selector.GetSelectorText());
		Assert.assertEquals(".class:first-child", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//fifth rule
		mRule = mRules.get(4);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div .class:before", selector.GetSelectorText());
		Assert.assertEquals("div .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//sixth rule
		mRule = mRules.get(5);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div > .class:before", selector.GetSelectorText());
		Assert.assertEquals("div > .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//seventh rule
		mRule = mRules.get(6);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div + .class:before", selector.GetSelectorText());
		Assert.assertEquals("div + .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//eight rule
		mRule = mRules.get(7);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.IsNonStructuralPseudo());
		Assert.assertTrue(selector.HasPseudoElement());
		Assert.assertEquals("div ~ .class:before", selector.GetSelectorText());
		Assert.assertEquals("div ~ .class:before", selector.GetFilteredSelectorText()); // no special query required for querying DOM

		//ninth rule
		mRule = mRules.get(8);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.IsIgnored()); //should be ignored because of :not

		//tenth rule
		mRule = mRules.get(9);

		selectors = mRule.GetSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.IsNonStructuralPseudo());
		Assert.assertFalse(selector.HasPseudoElement());
		Assert.assertEquals(".class:hover div:focus #id:visited", selector.GetSelectorText());
		Assert.assertEquals(":visited", selector.GetPseudoClass());
	}


	@Test
	public void testParseDeclarations()
	{
		CssParser parser = new CssParser();
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test",
				"div .class { color:white; border: 1px solid black; font-size:10px !important;}\n" +
						"div, a, span { font-size:20px; display:block; }\n" +
						"div {color: #000; background-color: #ffffff; }\n");	//colors transformed in rgb

		List<MCssRule> mRules = mCssFile.GetRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = mRules.get(0);

		MSelector mSelector = mRule.GetSelectors().get(0);

		List<MDeclaration> mDeclarations = mSelector.GetDeclarations();
		Assert.assertEquals(3, mDeclarations.size());

		Assert.assertEquals("color", mDeclarations.get(0).GetName());
		Assert.assertEquals("white", mDeclarations.get(0).GetValue());
		Assert.assertFalse(mDeclarations.get(0).IsImportant());

		Assert.assertEquals("border", mDeclarations.get(1).GetName());
		Assert.assertEquals("1px solid black", mDeclarations.get(1).GetValue());
		Assert.assertFalse(mDeclarations.get(1).IsImportant());

		Assert.assertEquals("font-size", mDeclarations.get(2).GetName());
		Assert.assertTrue(mDeclarations.get(2).IsImportant());

		for(MDeclaration mProp : mDeclarations)
			Assert.assertFalse(mProp.IsEffective());

		//second rule
		mRule = mRules.get(1);

		for(MSelector mSel : mRule.GetSelectors())
			Assert.assertEquals(2, mSel.GetDeclarations().size());

		mSelector = mRule.GetSelectors().get(0);
		mDeclarations = mSelector.GetDeclarations();

		Assert.assertEquals("font-size", mDeclarations.get(0).GetName());
		Assert.assertEquals("20px", mDeclarations.get(0).GetValue());
		Assert.assertFalse(mDeclarations.get(0).IsImportant());


		//third rule
		mRule = mRules.get(2);
		mSelector = mRule.GetSelectors().get(0);
		mDeclarations = mSelector.GetDeclarations();

		Assert.assertEquals("color", mDeclarations.get(0).GetName());
		Assert.assertEquals("rgb(0, 0, 0)", mDeclarations.get(0).GetValue());

		Assert.assertEquals("background-color", mDeclarations.get(1).GetName());
		Assert.assertEquals("rgb(255, 255, 255)", mDeclarations.get(1).GetValue());
	}

	@Test
	public void testParseIncorrectDeclarations()
	{
		CssParser parser = new CssParser(true);
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test",
						"div {\nbackground: solid; \n" +  // incorrect value for background
						"margin: 10 px;}\n" + // incorrect value for margin
						"div {-moz-box-shadow:10px 5px 5px black; hyphenate: none;}"); //first declaration warning (unknown vendor), second declaration error

		List<MCssRule> mRules = mCssFile.GetRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = mRules.get(0);
		MSelector mSelector = mRule.GetSelectors().get(0);
		List<MDeclaration> mDeclarations = mSelector.GetDeclarations();

		Assert.assertEquals("background", mDeclarations.get(0).GetName());
		Assert.assertTrue(mDeclarations.get(0).IsIgnored());

		Assert.assertEquals("margin", mDeclarations.get(1).GetName());
		Assert.assertTrue(mDeclarations.get(1).IsIgnored());

		//second rule
		mRule = mRules.get(1);
		mSelector = mRule.GetSelectors().get(0);
		mDeclarations = mSelector.GetDeclarations();

		Assert.assertEquals("-moz-box-shadow", mDeclarations.get(0).GetName());
		Assert.assertFalse(mDeclarations.get(0).IsIgnored()); 					// not ignored, because vendor prefix is warning

		Assert.assertEquals("hyphenate", mDeclarations.get(1).GetName());
		Assert.assertTrue(mDeclarations.get(1).IsIgnored());
	}

	@Test
	public void TestParseIncorrectSelectors()
	{
		CssParser parser = new CssParser(true);
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test", "span:invalid, div, a:valid { color: black; }");

		List<MCssRule> mRules = mCssFile.GetRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//verify that the correct selectors have w3c error messages
		MCssRule mRule = mRules.get(0);
		Assert.assertFalse(mRule.GetSelectors().get(0).GetW3cError().isEmpty());
		Assert.assertTrue(mRule.GetSelectors().get(1).GetW3cError().isEmpty());
		Assert.assertFalse(mRule.GetSelectors().get(2).GetW3cError().isEmpty());
	}
}
