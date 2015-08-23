package analysis;

import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.cret.plugins.analysis.MatchedElements;
import com.crawljax.plugins.cret.plugins.merge.NormalizeAndMergePlugin;
import helpers.TestHelper;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by axel on 5/27/2015.
 */
public class NormalizeTest
{
    public NormalizeTest()
    {
        DOMConfigurator.configure("log4j.xml");
    }

    @Test
    public void TestNormalization()
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/cssnormalizer_test_styles.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap<>();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin cssNormalizer = new NormalizeAndSplitPlugin();
        cssNormalizer.transform(files, new MatchedElements());

        List<MSelector> selectors = new ArrayList<>();

        for(MCssRule rule : files.get("external").getRules())
        {
            selectors.addAll(rule.getSelectors());
        }

        List<MDeclaration> mDeclarations = selectors.get(0).getDeclarations();
        Assert.assertEquals(8, mDeclarations.size());
        Assert.assertEquals("margin-top", mDeclarations.get(0).getName());
        Assert.assertEquals("0", mDeclarations.get(0).getValue());
        Assert.assertEquals("padding-top", mDeclarations.get(4).getName());
        Assert.assertEquals("0", mDeclarations.get(4).getValue());

        mDeclarations = selectors.get(1).getDeclarations();
        Assert.assertEquals(3, mDeclarations.size());
        Assert.assertEquals("border-width", mDeclarations.get(0).getName());
        Assert.assertEquals("1px", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-style", mDeclarations.get(1).getName());
        Assert.assertEquals("solid", mDeclarations.get(1).getValue());
        Assert.assertEquals("border-color", mDeclarations.get(2).getName());
        Assert.assertEquals("#ececec", mDeclarations.get(2).getValue());

        mDeclarations = selectors.get(2).getDeclarations();
        Assert.assertEquals(2, mDeclarations.size());
        Assert.assertEquals("border-style", mDeclarations.get(0).getName());
        Assert.assertEquals("dotted", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(3).getDeclarations();
        Assert.assertEquals(3, mDeclarations.size());
        Assert.assertEquals("border-color", mDeclarations.get(2).getName());
        Assert.assertEquals("rgba(255,255,255,.7)", mDeclarations.get(2).getValue());

        mDeclarations = selectors.get(4).getDeclarations();
        Assert.assertEquals(5, mDeclarations.size());
        Assert.assertEquals("background-color", mDeclarations.get(0).getName());
        Assert.assertEquals("#ececec", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(1).getName());
        Assert.assertEquals("100px 100px", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-attachment", mDeclarations.get(2).getName());
        Assert.assertEquals("fixed", mDeclarations.get(2).getValue());
        Assert.assertEquals("background-image", mDeclarations.get(3).getName());
        Assert.assertEquals("none", mDeclarations.get(3).getValue());
        Assert.assertEquals("background-repeat", mDeclarations.get(4).getName());
        Assert.assertEquals("repeat", mDeclarations.get(4).getValue());

        mDeclarations = selectors.get(5).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("background-image", mDeclarations.get(0).getName());
        Assert.assertEquals("url(www.google.nl)", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-color", mDeclarations.get(1).getName());
        Assert.assertEquals("#ececec", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-repeat", mDeclarations.get(2).getName());
        Assert.assertEquals("no-repeat", mDeclarations.get(2).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(3).getName());
        Assert.assertEquals("left top", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(6).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("background-image", mDeclarations.get(0).getName());
        Assert.assertEquals("url(www.google.nl)", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-color", mDeclarations.get(1).getName());
        Assert.assertEquals("#ececec", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-repeat", mDeclarations.get(2).getName());
        Assert.assertEquals("no-repeat", mDeclarations.get(2).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(3).getName());
        Assert.assertEquals("left", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(7).getDeclarations();
        Assert.assertEquals(3, mDeclarations.size());
        Assert.assertEquals("background-image", mDeclarations.get(0).getName());
        Assert.assertEquals("url(test)", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-color", mDeclarations.get(1).getName());
        Assert.assertEquals("#000000", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(2).getName());
        Assert.assertEquals("75% 75%", mDeclarations.get(2).getValue());

        mDeclarations = selectors.get(8).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("margin-top", mDeclarations.get(0).getName());
        Assert.assertEquals("5px", mDeclarations.get(0).getValue());
        Assert.assertEquals("margin-right", mDeclarations.get(1).getName());
        Assert.assertEquals("5px", mDeclarations.get(1).getValue());
        Assert.assertEquals("margin-bottom", mDeclarations.get(2).getName());
        Assert.assertEquals("5px", mDeclarations.get(2).getValue());
        Assert.assertEquals("margin-left", mDeclarations.get(3).getName());
        Assert.assertEquals("5px", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(9).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("margin-top", mDeclarations.get(0).getName());
        Assert.assertEquals("5px", mDeclarations.get(0).getValue());
        Assert.assertEquals("margin-right", mDeclarations.get(1).getName());
        Assert.assertEquals("10px", mDeclarations.get(1).getValue());
        Assert.assertEquals("margin-bottom", mDeclarations.get(2).getName());
        Assert.assertEquals("5px", mDeclarations.get(2).getValue());
        Assert.assertEquals("margin-left", mDeclarations.get(3).getName());
        Assert.assertEquals("10px", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(10).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("margin-top", mDeclarations.get(0).getName());
        Assert.assertEquals("5px", mDeclarations.get(0).getValue());
        Assert.assertEquals("margin-right", mDeclarations.get(1).getName());
        Assert.assertEquals("10px", mDeclarations.get(1).getValue());
        Assert.assertEquals("margin-bottom", mDeclarations.get(2).getName());
        Assert.assertEquals("20px", mDeclarations.get(2).getValue());
        Assert.assertEquals("margin-left", mDeclarations.get(3).getName());
        Assert.assertEquals("10px", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(11).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("margin-top", mDeclarations.get(0).getName());
        Assert.assertEquals("5px", mDeclarations.get(0).getValue());
        Assert.assertEquals("margin-right", mDeclarations.get(1).getName());
        Assert.assertEquals("10px", mDeclarations.get(1).getValue());
        Assert.assertEquals("margin-bottom", mDeclarations.get(2).getName());
        Assert.assertEquals("20px", mDeclarations.get(2).getValue());
        Assert.assertEquals("margin-left", mDeclarations.get(3).getName());
        Assert.assertEquals("30px", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(12).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("border-top-style", mDeclarations.get(0).getName());
        Assert.assertEquals("solid", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-right-style", mDeclarations.get(1).getName());
        Assert.assertEquals("dotted", mDeclarations.get(1).getValue());
        Assert.assertEquals("border-bottom-style", mDeclarations.get(2).getName());
        Assert.assertEquals("solid", mDeclarations.get(2).getValue());
        Assert.assertEquals("border-left-style", mDeclarations.get(3).getName());
        Assert.assertEquals("dotted", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(13).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("border-top-width", mDeclarations.get(0).getName());
        Assert.assertEquals("5px", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-right-width", mDeclarations.get(1).getName());
        Assert.assertEquals("10px", mDeclarations.get(1).getValue());
        Assert.assertEquals("border-bottom-width", mDeclarations.get(2).getName());
        Assert.assertEquals("20px", mDeclarations.get(2).getValue());
        Assert.assertEquals("border-left-width", mDeclarations.get(3).getName());
        Assert.assertEquals("30px", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(14).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("border-top-color", mDeclarations.get(0).getName());
        Assert.assertEquals("#000000", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-right-color", mDeclarations.get(1).getName());
        Assert.assertEquals("rgba(136,136,136,.7)", mDeclarations.get(1).getValue());
        Assert.assertEquals("border-bottom-color", mDeclarations.get(2).getName());
        Assert.assertEquals("#ffffff", mDeclarations.get(2).getValue());
        Assert.assertEquals("border-left-color", mDeclarations.get(3).getName());
        Assert.assertEquals("#ececec", mDeclarations.get(3).getValue());


        mDeclarations = selectors.get(15).getDeclarations();
        Assert.assertEquals(5, mDeclarations.size());
        Assert.assertEquals("border-width", mDeclarations.get(0).getName());
        Assert.assertEquals("0", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-top-left-radius", mDeclarations.get(1).getName());
        Assert.assertEquals("0", mDeclarations.get(1).getValue());
        Assert.assertEquals("border-top-right-radius", mDeclarations.get(2).getName());
        Assert.assertEquals("0", mDeclarations.get(2).getValue());
        Assert.assertEquals("border-bottom-right-radius", mDeclarations.get(3).getName());
        Assert.assertEquals("0", mDeclarations.get(3).getValue());
        Assert.assertEquals("border-bottom-left-radius", mDeclarations.get(4).getName());
        Assert.assertEquals("0", mDeclarations.get(4).getValue());

        mDeclarations = selectors.get(16).getDeclarations();
        Assert.assertEquals(3, mDeclarations.size());
        Assert.assertEquals("border-top-width", mDeclarations.get(0).getName());
        Assert.assertEquals("0", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-top-style", mDeclarations.get(1).getName());
        Assert.assertEquals("solid", mDeclarations.get(1).getValue());
        Assert.assertEquals("border-top-color", mDeclarations.get(2).getName());
        Assert.assertEquals("#ffffff", mDeclarations.get(2).getValue());

        mDeclarations = selectors.get(17).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("background-image", mDeclarations.get(0).getName());
        Assert.assertEquals("url(google.nl)", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(1).getName());
        Assert.assertEquals("100px", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-size", mDeclarations.get(2).getName());
        Assert.assertEquals("50% 50%", mDeclarations.get(2).getValue());
        Assert.assertEquals("background-repeat", mDeclarations.get(3).getName());
        Assert.assertEquals("repeat", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(18).getDeclarations();
        Assert.assertEquals(4, mDeclarations.size());
        Assert.assertEquals("background-image", mDeclarations.get(0).getName());
        Assert.assertEquals("url(google.nl)", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(1).getName());
        Assert.assertEquals("left top", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-size", mDeclarations.get(2).getName());
        Assert.assertEquals("100px", mDeclarations.get(2).getValue());
        Assert.assertEquals("background-repeat", mDeclarations.get(3).getName());
        Assert.assertEquals("repeat", mDeclarations.get(3).getValue());

        mDeclarations = selectors.get(19).getDeclarations();
        Assert.assertEquals(5, mDeclarations.size());
        Assert.assertEquals("background-image", mDeclarations.get(0).getName());
        Assert.assertEquals("url(google.nl)", mDeclarations.get(0).getValue());
        Assert.assertEquals("background-position", mDeclarations.get(1).getName());
        Assert.assertEquals("100px", mDeclarations.get(1).getValue());
        Assert.assertEquals("background-size", mDeclarations.get(2).getName());
        Assert.assertEquals("contain", mDeclarations.get(2).getValue());
        Assert.assertEquals("background-repeat", mDeclarations.get(3).getName());
        Assert.assertEquals("no-repeat", mDeclarations.get(3).getValue());
        Assert.assertEquals("background-attachment", mDeclarations.get(4).getName());
        Assert.assertEquals("fixed", mDeclarations.get(4).getValue());

        // merge test
        NormalizeAndMergePlugin pmp = new NormalizeAndMergePlugin();
        pmp.transform(files, new MatchedElements());

        mDeclarations = selectors.get(0).getDeclarations();
        Assert.assertEquals(2, mDeclarations.size());
        Assert.assertEquals("margin", mDeclarations.get(0).getName());
        Assert.assertEquals("0 px", mDeclarations.get(0).getValue());
        Assert.assertEquals("padding", mDeclarations.get(1).getName());
        Assert.assertEquals("0", mDeclarations.get(1).getValue());

        mDeclarations = selectors.get(1).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border", mDeclarations.get(0).getName());
        Assert.assertEquals("1px solid #ececec", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(2).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border", mDeclarations.get(0).getName());
        Assert.assertEquals("2px dotted", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(3).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border", mDeclarations.get(0).getName());
        Assert.assertEquals("2px solid rgba(255,255,255,.7)", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(4).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("#ececec none 100px 100px repeat fixed", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(5).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("#ececec url(www.google.nl) left top no-repeat", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(6).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("#ececec url(www.google.nl) left no-repeat", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(7).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("#000000 url(test) 75% 75%", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(8).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("margin", mDeclarations.get(0).getName());
        Assert.assertEquals("5px", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(9).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("margin", mDeclarations.get(0).getName());
        Assert.assertEquals("5px 10px", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(10).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("margin", mDeclarations.get(0).getName());
        Assert.assertEquals("5px 10px 20px", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(11).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("margin", mDeclarations.get(0).getName());
        Assert.assertEquals("5px 10px 20px 30px", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(12).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border-style", mDeclarations.get(0).getName());
        Assert.assertEquals("solid dotted", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(13).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border-width", mDeclarations.get(0).getName());
        Assert.assertEquals("5px 10px 20px 30px", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(14).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border-color", mDeclarations.get(0).getName());
        Assert.assertEquals("#000000 rgba(136,136,136,.7) #ffffff #ececec", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(15).getDeclarations();
        Assert.assertEquals(2, mDeclarations.size());
        Assert.assertEquals("border", mDeclarations.get(0).getName());
        Assert.assertEquals("0", mDeclarations.get(0).getValue());
        Assert.assertEquals("border-radius", mDeclarations.get(1).getName());
        Assert.assertEquals("0", mDeclarations.get(1).getValue());

        mDeclarations = selectors.get(16).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("border-top", mDeclarations.get(0).getName());
        Assert.assertEquals("0 solid #ffffff", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(17).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("url(google.nl) 100px/50% 50% repeat", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(18).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("url(google.nl) left top/100px repeat", mDeclarations.get(0).getValue());

        mDeclarations = selectors.get(19).getDeclarations();
        Assert.assertEquals(1, mDeclarations.size());
        Assert.assertEquals("background", mDeclarations.get(0).getName());
        Assert.assertEquals("url(google.nl) 100px/contain no-repeat fixed", mDeclarations.get(0).getValue());
    }

    @Test
    public void TestPropertyOrder()
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/cssnormalizer_property_order_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin cssSplitter = new NormalizeAndSplitPlugin();
        cssSplitter.transform(files, new MatchedElements());

        List<MSelector> selectors = new ArrayList<>();
        for(MCssRule rule : files.get("external").getRules())
        {
            selectors.addAll(rule.getSelectors());
        }

        List<MDeclaration> props = selectors.get(0).getDeclarations();
        Assert.assertEquals("border-width", props.get(0).getName());
        Assert.assertEquals("border-bottom-width", props.get(1).getName());
        Assert.assertEquals("border-bottom-style", props.get(2).getName());
        Assert.assertEquals("border-bottom-color", props.get(3).getName());

        NormalizeAndMergePlugin cssMerger = new NormalizeAndMergePlugin();
        cssMerger.transform(files, new MatchedElements());

        props = selectors.get(0).getDeclarations();
        Assert.assertEquals("border", props.get(0).getName());
        Assert.assertEquals("border-bottom", props.get(1).getName());
    }
}
