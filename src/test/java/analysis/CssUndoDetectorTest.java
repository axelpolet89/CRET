package analysis;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.plugins.analysis.EffectivenessPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.ElementSelectorMatcher;
import com.crawljax.plugins.csssuite.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.csssuite.plugins.DetectUndoingPlugin;
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
        DetectUndoingPlugin undoDetector = new DetectUndoingPlugin();

        // crawl dom
        MatchedElements matchedElements = new MatchedElements();
        ElementSelectorMatcher.MatchElementsToDocument("", dom, files, order, matchedElements);

        // post crawling
        Map<String, MCssFile> postResult =  undoDetector.transform(effectivenessPlugin.transform(normalizer.transform(files, matchedElements), matchedElements), matchedElements);

        List<MSelector> validSelectors = new ArrayList<>();
        for(MCssRule rule : postResult.get("external").GetRules())
        {
            validSelectors.addAll(rule.GetSelectors());
        }

        //assert correct amount of valid selectors
        Assert.assertEquals(4, validSelectors.size());
        Assert.assertArrayEquals(Arrays.asList("ul li", "#wishlist1 li", "h3", "h3").toArray(),
                validSelectors.stream().map((ms) -> ms.GetSelectorText()).collect(Collectors.toList()).toArray());

        for(int i = 0; i < validSelectors.size(); i++)
        {
            MSelector sel = validSelectors.get(i);

            if (i == 2)
            {
                Assert.assertEquals(1, sel.GetProperties().size());
                Assert.assertEquals("color", sel.GetProperties().get(0).GetName());
            }
            else
            {
                for (MProperty prop : sel.GetProperties())
                {
                    Assert.assertTrue(prop.IsEffective());
                    Assert.assertFalse(prop.IsInvalidUndo());
                }
            }
        }
    }
}
