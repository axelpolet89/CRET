package analysis;

import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.plugins.*;

import com.crawljax.plugins.csssuite.plugins.EffectivenessPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.ElementSelectorMatcher;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import helpers.TestHelper;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/2/2015.
 */
public class DescendantToChildTest
{
    public DescendantToChildTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestDescendantToChild()
    {
        Document dom = TestHelper.GetDocumentFromFile("./src/test/resources/cssanalyzer_test_index.html");
        Assert.assertNotNull(dom);

        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/css-descendant-to-child_test.css");
        Assert.assertNotNull(externalFile);

        HashMap files = new HashMap<>();
        files.put("external", externalFile);

        LinkedHashMap order = new LinkedHashMap<>();
        order.put("external", 0);

        EffectivenessPlugin eff = new EffectivenessPlugin();
        ChildCombinatorPlugin dtoc = new ChildCombinatorPlugin();

        // crawl dom
        MatchedElements matchedElements = new MatchedElements();
        ElementSelectorMatcher.MatchElementsToDocument("", dom, files, order, matchedElements);

        // for later verification
        Set<String> matches = new HashSet<>();
        for(String s : matchedElements.GetMatchedElements())
                matches.add(s);

        List<MDeclaration> matchedDeclarations = new ArrayList<>();
        for(String s : matches)
        {
            for(MSelector m : matchedElements.SortSelectorsForMatchedElem(s))
                matchedDeclarations.addAll(m.GetDeclarations());
        }

        // post crawling
        Map<String, MCssFile> postResult =  dtoc.transform(eff.transform(files, matchedElements), matchedElements);

        List<MSelector> validSelectors = new ArrayList<>();
        for(MCssRule rule : postResult.get("external").GetRules())
        {
            validSelectors.addAll(rule.GetSelectors());
        }

        // assert correct amount of valid selectors
        Assert.assertEquals(12, validSelectors.size());
        Assert.assertArrayEquals(Arrays.asList("div#footer", ".input-content > a", "#page h3", "body > .extra-content:after", "body.page > .content",
                        "*[class=\"input-content\"] > a:hover", ".input-content > *[checked]", "body > #page h3",
                        ".sibling-content > span + a > .image", ".sibling-content > span ~ a.url > .image", ".class1.class2.class3 > img", ".class4.class5.class6 span").toArray(),
                validSelectors.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

        // verify re-match of selectors by performing run-time analysis again
        MatchedElements matchedElements2 = new MatchedElements();
        ElementSelectorMatcher.MatchElementsToDocument("", dom, postResult, order, matchedElements2);

        Set<String> matches2 = matchedElements2.GetMatchedElements();
        Assert.assertArrayEquals(matches.toArray(), matches2.toArray());

        List<MDeclaration> matchedDeclarations2 = new ArrayList<>();
        for(String s : matches2)
        {
            for(MSelector m : matchedElements2.SortSelectorsForMatchedElem(s))
                matchedDeclarations2.addAll(m.GetDeclarations());
        }
        Assert.assertArrayEquals(matchedDeclarations.toArray(), matchedDeclarations2.toArray());
    }
}
