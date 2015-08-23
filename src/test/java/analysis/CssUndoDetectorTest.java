package analysis;

import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;

import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.plugins.effectiveness.EffectivenessPlugin;
import com.crawljax.plugins.cret.plugins.matcher.ElementSelectorMatcher;
import com.crawljax.plugins.cret.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.cret.plugins.DefaultStylesPlugin;
import com.crawljax.plugins.cret.plugins.matcher.MatchedElements;
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
public class CssUndoDetectorTest
{
    public CssUndoDetectorTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestDetectUndoStyles()
    {
        Document dom = TestHelper.GetDocumentFromFile("./src/test/resources/cssanalyzer_test_index.html");
        Assert.assertNotNull(dom);

        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/cssundodetector_test.css");
        Assert.assertNotNull(externalFile);

        HashMap files = new HashMap();
        files.put("external", externalFile);

        LinkedHashMap order = new LinkedHashMap();
        order.put("external", 0);


        NormalizeAndSplitPlugin normalizer = new NormalizeAndSplitPlugin();
        EffectivenessPlugin effectivenessPlugin = new EffectivenessPlugin();
        DefaultStylesPlugin undoDetector = new DefaultStylesPlugin();

        // crawl dom
        MatchedElements matchedElements = new MatchedElements();
        ElementSelectorMatcher.matchElementsToDocument("", dom, files, order, matchedElements);

        // post crawling
        Map<String, MCssFile> postResult =  undoDetector.transform(effectivenessPlugin.transform(normalizer.transform(files, matchedElements), matchedElements), matchedElements);

        List<MSelector> validSelectors = new ArrayList<>();
        for(MCssRule rule : postResult.get("external").getRules())
        {
            validSelectors.addAll(rule.getSelectors());
        }

        //assert correct amount of valid selectors
        Assert.assertEquals(4, validSelectors.size());
        Assert.assertArrayEquals(Arrays.asList("ul li", "#wishlist1 li", "h3", "h3").toArray(),
                validSelectors.stream().map((ms) -> ms.getSelectorText()).collect(Collectors.toList()).toArray());

        for(int i = 0; i < validSelectors.size(); i++)
        {
            MSelector sel = validSelectors.get(i);

            if (i == 2)
            {
                Assert.assertEquals(1, sel.getDeclarations().size());
                Assert.assertEquals("color", sel.getDeclarations().get(0).getName());
            }
            else
            {
                for (MDeclaration prop : sel.getDeclarations())
                {
                    Assert.assertTrue(prop.isEffective());
                    Assert.assertFalse(prop.isInvalidUndo());
                }
            }
        }
    }
}
