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

		List <MCssRule> mRules = mCssFile.getRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());
		Assert.assertEquals(4, mRules.size());

		MCssRule mRule = mRules.get(2);
		Assert.assertEquals(5, mRule.getSelectors().size());

		mRule = mRules.get(3);
		Assert.assertEquals(1, mRule.getSelectors().size());

		/* filtering rules and properties with incorrect syntax */
		parser = new CssParser(false);
		mCssFile = parser.ParseCssIntoMCssRules("test",
				"h p[att=\"test\" { color: red; } " + //syntax-error, should ignore entire rule
				"div, h p[att=\"test\", span { color: red; }" + //syntax-error, should ignore entire rule including other selectors
				"div, a, span { font  20px; margin: 10px; }" + //syntax-error, should ignore declaration
				"#id, .class, span[attr=\"test\"], a:hover, span::before { color: black; }" +
				"#id div.class span { color: pink; }");

		mRules = mCssFile.getRules();
		Assert.assertEquals(3, mRules.size());

		// only 1 declaration parsed for third rule (first rule in list)
		Assert.assertEquals(1, mRules.get(0).getSelectors().get(0).getDeclarations().size());

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
		mRules = mCssFile.getRules();
		Assert.assertEquals(4, mRules.size());

		parseErrors = parser.GetParseErrors();
		Assert.assertEquals(parseErrors.size(), 0);
	}

	@Test
	public void TestParseLocator()
	{
		CssParser parser = new CssParser();
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test", "h p { color: red;} \n\n div { font: black} \n span {color: white}");
		List<MCssRule> rules = mCssFile.getRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		MCssRule mRule = rules.get(0);
		Assert.assertEquals(1, mRule.getLineNumber());

		mRule = rules.get(1);
		Assert.assertEquals(3, mRule.getLineNumber());

		mRule = rules.get(2);
		Assert.assertEquals(4, mRule.getLineNumber());
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

		List<MCssRule> mRules = mCssFile.getRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = mRules.get(0);

		List<MSelector> selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		MSelector selector = selectors.get(0);
		Assert.assertEquals("h p", selector.getSelectorText());
		Assert.assertFalse(selector.isMatched());
		Assert.assertFalse(selector.isNonStructuralPseudo());
		Assert.assertFalse(selector.hasPseudoElement());
		Assert.assertFalse(selector.isIgnored());

		//second rule
		mRule = mRules.get(1);

		selectors = mRule.getSelectors();
		Assert.assertEquals(3, selectors.size());

		selector = selectors.get(1);
		Assert.assertEquals("a", selector.getSelectorText());

		//third rule
		mRule = mRules.get(2);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.isNonStructuralPseudo());
		Assert.assertFalse(selector.hasPseudoElement());
		Assert.assertEquals(".class:hover", selector.getSelectorText());
		Assert.assertEquals(".class", selector.getFilteredSelectorText()); // special variant for querying DOM

		//fourth rule
		mRule = mRules.get(3);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.isNonStructuralPseudo()); // this is structural
		Assert.assertFalse(selector.hasPseudoElement());
		Assert.assertEquals(".class:first-child", selector.getSelectorText());
		Assert.assertEquals(".class:first-child", selector.getFilteredSelectorText()); // no special query required for querying DOM

		//fifth rule
		mRule = mRules.get(4);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.isNonStructuralPseudo());
		Assert.assertTrue(selector.hasPseudoElement());
		Assert.assertEquals("div .class:before", selector.getSelectorText());
		Assert.assertEquals("div .class:before", selector.getFilteredSelectorText()); // no special query required for querying DOM

		//sixth rule
		mRule = mRules.get(5);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.isNonStructuralPseudo());
		Assert.assertTrue(selector.hasPseudoElement());
		Assert.assertEquals("div > .class:before", selector.getSelectorText());
		Assert.assertEquals("div > .class:before", selector.getFilteredSelectorText()); // no special query required for querying DOM

		//seventh rule
		mRule = mRules.get(6);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.isNonStructuralPseudo());
		Assert.assertTrue(selector.hasPseudoElement());
		Assert.assertEquals("div + .class:before", selector.getSelectorText());
		Assert.assertEquals("div + .class:before", selector.getFilteredSelectorText()); // no special query required for querying DOM

		//eight rule
		mRule = mRules.get(7);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertFalse(selector.isNonStructuralPseudo());
		Assert.assertTrue(selector.hasPseudoElement());
		Assert.assertEquals("div ~ .class:before", selector.getSelectorText());
		Assert.assertEquals("div ~ .class:before", selector.getFilteredSelectorText()); // no special query required for querying DOM

		//ninth rule
		mRule = mRules.get(8);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.isIgnored()); //should be ignored because of :not

		//tenth rule
		mRule = mRules.get(9);

		selectors = mRule.getSelectors();
		Assert.assertEquals(1, selectors.size());

		selector = selectors.get(0);
		Assert.assertTrue(selector.isNonStructuralPseudo());
		Assert.assertFalse(selector.hasPseudoElement());
		Assert.assertEquals(".class:hover div:focus #id:visited", selector.getSelectorText());
		Assert.assertEquals(":visited", selector.getPseudoClass());
	}


	@Test
	public void testParseDeclarations()
	{
		CssParser parser = new CssParser();
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test",
				"div .class { color:white; border: 1px solid black; font-size:10px !important;}\n" +
						"div, a, span { font-size:20px; display:block; }\n" +
						"div {color: #000; background-color: #ffffff; }\n");	//colors transformed in rgb

		List<MCssRule> mRules = mCssFile.getRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = mRules.get(0);

		MSelector mSelector = mRule.getSelectors().get(0);

		List<MDeclaration> mDeclarations = mSelector.getDeclarations();
		Assert.assertEquals(3, mDeclarations.size());

		Assert.assertEquals("color", mDeclarations.get(0).getName());
		Assert.assertEquals("white", mDeclarations.get(0).getValue());
		Assert.assertFalse(mDeclarations.get(0).isImportant());

		Assert.assertEquals("border", mDeclarations.get(1).getName());
		Assert.assertEquals("1px solid black", mDeclarations.get(1).getValue());
		Assert.assertFalse(mDeclarations.get(1).isImportant());

		Assert.assertEquals("font-size", mDeclarations.get(2).getName());
		Assert.assertTrue(mDeclarations.get(2).isImportant());

		for(MDeclaration mProp : mDeclarations)
			Assert.assertFalse(mProp.isEffective());

		//second rule
		mRule = mRules.get(1);

		for(MSelector mSel : mRule.getSelectors())
			Assert.assertEquals(2, mSel.getDeclarations().size());

		mSelector = mRule.getSelectors().get(0);
		mDeclarations = mSelector.getDeclarations();

		Assert.assertEquals("font-size", mDeclarations.get(0).getName());
		Assert.assertEquals("20px", mDeclarations.get(0).getValue());
		Assert.assertFalse(mDeclarations.get(0).isImportant());


		//third rule
		mRule = mRules.get(2);
		mSelector = mRule.getSelectors().get(0);
		mDeclarations = mSelector.getDeclarations();

		Assert.assertEquals("color", mDeclarations.get(0).getName());
		Assert.assertEquals("rgb(0, 0, 0)", mDeclarations.get(0).getValue());

		Assert.assertEquals("background-color", mDeclarations.get(1).getName());
		Assert.assertEquals("rgb(255, 255, 255)", mDeclarations.get(1).getValue());
	}

	@Test
	public void testParseIncorrectDeclarations()
	{
		CssParser parser = new CssParser(true);
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test",
						"div {\nbackground: solid; \n" +  // incorrect value for background
						"margin: 10 px;}\n" + // incorrect value for margin
						"div {-moz-box-shadow:10px 5px 5px black; hyphenate: none;}"); //first declaration warning (unknown vendor), second declaration error

		List<MCssRule> mRules = mCssFile.getRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//first rule
		MCssRule mRule = mRules.get(0);
		MSelector mSelector = mRule.getSelectors().get(0);
		List<MDeclaration> mDeclarations = mSelector.getDeclarations();

		Assert.assertEquals("background", mDeclarations.get(0).getName());
		Assert.assertTrue(mDeclarations.get(0).isIgnored());

		Assert.assertEquals("margin", mDeclarations.get(1).getName());
		Assert.assertTrue(mDeclarations.get(1).isIgnored());

		//second rule
		mRule = mRules.get(1);
		mSelector = mRule.getSelectors().get(0);
		mDeclarations = mSelector.getDeclarations();

		Assert.assertEquals("-moz-box-shadow", mDeclarations.get(0).getName());
		Assert.assertFalse(mDeclarations.get(0).isIgnored()); 					// not ignored, because vendor prefix is warning

		Assert.assertEquals("hyphenate", mDeclarations.get(1).getName());
		Assert.assertTrue(mDeclarations.get(1).isIgnored());
	}

	@Test
	public void TestParseIncorrectSelectors()
	{
		CssParser parser = new CssParser(true);
		MCssFile mCssFile = parser.ParseCssIntoMCssRules("test", "span:invalid, div, a:valid { color: black; }");

		List<MCssRule> mRules = mCssFile.getRules();

		//make sure no parse errors occurred
		Assert.assertEquals(0, parser.GetParseErrors().size());

		//verify that the correct selectors have w3c error messages
		MCssRule mRule = mRules.get(0);
		Assert.assertFalse(mRule.getSelectors().get(0).getW3CError().isEmpty());
		Assert.assertTrue(mRule.getSelectors().get(1).getW3CError().isEmpty());
		Assert.assertFalse(mRule.getSelectors().get(2).getW3CError().isEmpty());
	}
}
