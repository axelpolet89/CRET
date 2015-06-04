package analysis;

import com.crawljax.plugins.csssuite.data.*;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.plugins.*;

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
        Document dom = TestHelper.GetDocumentFromFile("./src/test/test_files/cssanalyzer_test_index.html");
        Assert.assertNotNull(dom);

        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/test_files/css-descendant-to-child_test.css");
        Assert.assertNotNull(externalFile);

        HashMap files = new HashMap();
        files.put("external", externalFile);

        LinkedHashMap order = new LinkedHashMap();
        order.put("external", 0);

        // depends on cssanalyzer...
        CssAnalyzer analyzer = new CssAnalyzer();
        CssDescendantToChild dtoc = new CssDescendantToChild();

        // crawl dom
        analyzer.Transform("", dom, files, order);

        // for later verification
        Set<String> matches = new HashSet<>();
        for(String s : MatchedElements.GetMatchedElements())
                matches.add(s);

        List<MProperty> matchedProperties = new ArrayList<>();
        for(String s : matches)
        {
            for(MSelector m : MatchedElements.SortSelectorsForMatchedElem(s))
                matchedProperties.addAll(m.GetProperties());
        }

        // post crawling
        Map<String, MCssFile> postResult =  dtoc.Transform(analyzer.Transform(files));

        List<MSelector> validSelectors = new ArrayList<>();
        for(MCssRule rule : postResult.get("external").GetRules())
        {
            validSelectors.addAll(rule.GetSelectors());
        }

        // assert correct amount of valid selectors
        Assert.assertEquals(10, validSelectors.size());
        Assert.assertArrayEquals(Arrays.asList("div#footer", ".input-content > a", "#page h3", "body > .extra-content:after", "body.page > .content",
                        "*[class=\"input-content\"] > a:hover", ".input-content > *[checked]", "body > #page h3",
                        ".sibling-content > span + a > .image", ".sibling-content > span ~ a.url > .image").toArray(),
                validSelectors.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

        // verify re-match of selectors by performing run-time analysis again
        MatchedElements.Clear();
        analyzer.Transform("", dom, postResult, order);

        Set<String> matches2 = MatchedElements.GetMatchedElements();
        Assert.assertArrayEquals(matches.toArray(), matches2.toArray());

        List<MProperty> matchedProperties2 = new ArrayList<>();
        for(String s : matches2)
        {
            for(MSelector m : MatchedElements.SortSelectorsForMatchedElem(s))
                matchedProperties2.addAll(m.GetProperties());
        }
        Assert.assertArrayEquals(matchedProperties.toArray(), matchedProperties2.toArray());
    }
}
