package analysis;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;

import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.plugins.CssAnalyzer;
import com.crawljax.plugins.csssuite.plugins.CssNormalizer;
import com.crawljax.plugins.csssuite.plugins.CssUndoDetector;
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
        Document dom = TestHelper.GetDocumentFromFile("./src/test/test_files/cssanalyzer_test_index.html");
        Assert.assertNotNull(dom);

        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/test_files/cssundodetector_test.css");
        Assert.assertNotNull(externalFile);

        HashMap files = new HashMap();
        files.put("external", externalFile);

        LinkedHashMap order = new LinkedHashMap();
        order.put("external", 0);

        // depends on cssanalyzer...
        CssAnalyzer analyzer = new CssAnalyzer();
        CssNormalizer normalizer = new CssNormalizer();
        CssUndoDetector undoDetector = new CssUndoDetector();

        // crawl dom
        analyzer.Transform("", dom, files, order);

        // post crawling
        Map<String, MCssFile> postResult =  undoDetector.Transform(analyzer.Transform(normalizer.Transform(files)));

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
