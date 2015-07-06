package analysis;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import com.crawljax.plugins.csssuite.plugins.merge.NormalizeAndMergePlugin;
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
        cssNormalizer.Transform(files, new MatchedElements());

        List<MSelector> selectors = new ArrayList<>();

        for(MCssRule rule : files.get("external").GetRules())
        {
            selectors.addAll(rule.GetSelectors());
        }

        List<MProperty> properties = selectors.get(0).GetProperties();
        Assert.assertEquals(8, properties.size());
        Assert.assertEquals("margin-top", properties.get(0).GetName());
        Assert.assertEquals("0", properties.get(0).GetValue());
        Assert.assertEquals("padding-top", properties.get(4).GetName());
        Assert.assertEquals("0", properties.get(4).GetValue());

        properties = selectors.get(1).GetProperties();
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("border-width", properties.get(0).GetName());
        Assert.assertEquals("1px", properties.get(0).GetValue());
        Assert.assertEquals("border-style", properties.get(1).GetName());
        Assert.assertEquals("solid", properties.get(1).GetValue());
        Assert.assertEquals("border-color", properties.get(2).GetName());
        Assert.assertEquals("#ececec", properties.get(2).GetValue());

        properties = selectors.get(2).GetProperties();
        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("border-style", properties.get(0).GetName());
        Assert.assertEquals("dotted", properties.get(0).GetValue());

        properties = selectors.get(3).GetProperties();
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("border-color", properties.get(2).GetName());
        Assert.assertEquals("rgba(255,255,255,.7)", properties.get(2).GetValue());

        properties = selectors.get(4).GetProperties();
        Assert.assertEquals(5, properties.size());
        Assert.assertEquals("background-color", properties.get(0).GetName());
        Assert.assertEquals("#ececec", properties.get(0).GetValue());
        Assert.assertEquals("background-position", properties.get(1).GetName());
        Assert.assertEquals("100px 100px", properties.get(1).GetValue());
        Assert.assertEquals("background-attachment", properties.get(2).GetName());
        Assert.assertEquals("fixed", properties.get(2).GetValue());
        Assert.assertEquals("background-image", properties.get(3).GetName());
        Assert.assertEquals("none", properties.get(3).GetValue());
        Assert.assertEquals("background-repeat", properties.get(4).GetName());
        Assert.assertEquals("repeat", properties.get(4).GetValue());

        properties = selectors.get(5).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("background-image", properties.get(0).GetName());
        Assert.assertEquals("url(www.google.nl)", properties.get(0).GetValue());
        Assert.assertEquals("background-color", properties.get(1).GetName());
        Assert.assertEquals("#ececec", properties.get(1).GetValue());
        Assert.assertEquals("background-repeat", properties.get(2).GetName());
        Assert.assertEquals("no-repeat", properties.get(2).GetValue());
        Assert.assertEquals("background-position", properties.get(3).GetName());
        Assert.assertEquals("left top", properties.get(3).GetValue());

        properties = selectors.get(6).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("background-image", properties.get(0).GetName());
        Assert.assertEquals("url(www.google.nl)", properties.get(0).GetValue());
        Assert.assertEquals("background-color", properties.get(1).GetName());
        Assert.assertEquals("#ececec", properties.get(1).GetValue());
        Assert.assertEquals("background-repeat", properties.get(2).GetName());
        Assert.assertEquals("no-repeat", properties.get(2).GetValue());
        Assert.assertEquals("background-position", properties.get(3).GetName());
        Assert.assertEquals("left", properties.get(3).GetValue());

        properties = selectors.get(7).GetProperties();
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("background-image", properties.get(0).GetName());
        Assert.assertEquals("url(test)", properties.get(0).GetValue());
        Assert.assertEquals("background-color", properties.get(1).GetName());
        Assert.assertEquals("#000000", properties.get(1).GetValue());
        Assert.assertEquals("background-position", properties.get(2).GetName());
        Assert.assertEquals("75% 75%", properties.get(2).GetValue());

        properties = selectors.get(8).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("margin-top", properties.get(0).GetName());
        Assert.assertEquals("5px", properties.get(0).GetValue());
        Assert.assertEquals("margin-right", properties.get(1).GetName());
        Assert.assertEquals("5px", properties.get(1).GetValue());
        Assert.assertEquals("margin-bottom", properties.get(2).GetName());
        Assert.assertEquals("5px", properties.get(2).GetValue());
        Assert.assertEquals("margin-left", properties.get(3).GetName());
        Assert.assertEquals("5px", properties.get(3).GetValue());

        properties = selectors.get(9).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("margin-top", properties.get(0).GetName());
        Assert.assertEquals("5px", properties.get(0).GetValue());
        Assert.assertEquals("margin-right", properties.get(1).GetName());
        Assert.assertEquals("10px", properties.get(1).GetValue());
        Assert.assertEquals("margin-bottom", properties.get(2).GetName());
        Assert.assertEquals("5px", properties.get(2).GetValue());
        Assert.assertEquals("margin-left", properties.get(3).GetName());
        Assert.assertEquals("10px", properties.get(3).GetValue());

        properties = selectors.get(10).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("margin-top", properties.get(0).GetName());
        Assert.assertEquals("5px", properties.get(0).GetValue());
        Assert.assertEquals("margin-right", properties.get(1).GetName());
        Assert.assertEquals("10px", properties.get(1).GetValue());
        Assert.assertEquals("margin-bottom", properties.get(2).GetName());
        Assert.assertEquals("20px", properties.get(2).GetValue());
        Assert.assertEquals("margin-left", properties.get(3).GetName());
        Assert.assertEquals("10px", properties.get(3).GetValue());

        properties = selectors.get(11).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("margin-top", properties.get(0).GetName());
        Assert.assertEquals("5px", properties.get(0).GetValue());
        Assert.assertEquals("margin-right", properties.get(1).GetName());
        Assert.assertEquals("10px", properties.get(1).GetValue());
        Assert.assertEquals("margin-bottom", properties.get(2).GetName());
        Assert.assertEquals("20px", properties.get(2).GetValue());
        Assert.assertEquals("margin-left", properties.get(3).GetName());
        Assert.assertEquals("30px", properties.get(3).GetValue());

        properties = selectors.get(12).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("border-top-style", properties.get(0).GetName());
        Assert.assertEquals("solid", properties.get(0).GetValue());
        Assert.assertEquals("border-right-style", properties.get(1).GetName());
        Assert.assertEquals("dotted", properties.get(1).GetValue());
        Assert.assertEquals("border-bottom-style", properties.get(2).GetName());
        Assert.assertEquals("solid", properties.get(2).GetValue());
        Assert.assertEquals("border-left-style", properties.get(3).GetName());
        Assert.assertEquals("dotted", properties.get(3).GetValue());

        properties = selectors.get(13).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("border-top-width", properties.get(0).GetName());
        Assert.assertEquals("5px", properties.get(0).GetValue());
        Assert.assertEquals("border-right-width", properties.get(1).GetName());
        Assert.assertEquals("10px", properties.get(1).GetValue());
        Assert.assertEquals("border-bottom-width", properties.get(2).GetName());
        Assert.assertEquals("20px", properties.get(2).GetValue());
        Assert.assertEquals("border-left-width", properties.get(3).GetName());
        Assert.assertEquals("30px", properties.get(3).GetValue());

        properties = selectors.get(14).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("border-top-color", properties.get(0).GetName());
        Assert.assertEquals("#000000", properties.get(0).GetValue());
        Assert.assertEquals("border-right-color", properties.get(1).GetName());
        Assert.assertEquals("rgba(136,136,136,.7)", properties.get(1).GetValue());
        Assert.assertEquals("border-bottom-color", properties.get(2).GetName());
        Assert.assertEquals("#ffffff", properties.get(2).GetValue());
        Assert.assertEquals("border-left-color", properties.get(3).GetName());
        Assert.assertEquals("#ececec", properties.get(3).GetValue());


        properties = selectors.get(15).GetProperties();
        Assert.assertEquals(5, properties.size());
        Assert.assertEquals("border-width", properties.get(0).GetName());
        Assert.assertEquals("0", properties.get(0).GetValue());
        Assert.assertEquals("border-top-left-radius", properties.get(1).GetName());
        Assert.assertEquals("0", properties.get(1).GetValue());
        Assert.assertEquals("border-top-right-radius", properties.get(2).GetName());
        Assert.assertEquals("0", properties.get(2).GetValue());
        Assert.assertEquals("border-bottom-right-radius", properties.get(3).GetName());
        Assert.assertEquals("0", properties.get(3).GetValue());
        Assert.assertEquals("border-bottom-left-radius", properties.get(4).GetName());
        Assert.assertEquals("0", properties.get(4).GetValue());

        properties = selectors.get(16).GetProperties();
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("border-top-width", properties.get(0).GetName());
        Assert.assertEquals("0", properties.get(0).GetValue());
        Assert.assertEquals("border-top-style", properties.get(1).GetName());
        Assert.assertEquals("solid", properties.get(1).GetValue());
        Assert.assertEquals("border-top-color", properties.get(2).GetName());
        Assert.assertEquals("#ffffff", properties.get(2).GetValue());

        properties = selectors.get(17).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("background-image", properties.get(0).GetName());
        Assert.assertEquals("url(google.nl)", properties.get(0).GetValue());
        Assert.assertEquals("background-position", properties.get(1).GetName());
        Assert.assertEquals("100px", properties.get(1).GetValue());
        Assert.assertEquals("background-size", properties.get(2).GetName());
        Assert.assertEquals("50% 50%", properties.get(2).GetValue());
        Assert.assertEquals("background-repeat", properties.get(3).GetName());
        Assert.assertEquals("repeat", properties.get(3).GetValue());

        properties = selectors.get(18).GetProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("background-image", properties.get(0).GetName());
        Assert.assertEquals("url(google.nl)", properties.get(0).GetValue());
        Assert.assertEquals("background-position", properties.get(1).GetName());
        Assert.assertEquals("left top", properties.get(1).GetValue());
        Assert.assertEquals("background-size", properties.get(2).GetName());
        Assert.assertEquals("100px", properties.get(2).GetValue());
        Assert.assertEquals("background-repeat", properties.get(3).GetName());
        Assert.assertEquals("repeat", properties.get(3).GetValue());

        properties = selectors.get(19).GetProperties();
        Assert.assertEquals(5, properties.size());
        Assert.assertEquals("background-image", properties.get(0).GetName());
        Assert.assertEquals("url(google.nl)", properties.get(0).GetValue());
        Assert.assertEquals("background-position", properties.get(1).GetName());
        Assert.assertEquals("100px", properties.get(1).GetValue());
        Assert.assertEquals("background-size", properties.get(2).GetName());
        Assert.assertEquals("contain", properties.get(2).GetValue());
        Assert.assertEquals("background-repeat", properties.get(3).GetName());
        Assert.assertEquals("no-repeat", properties.get(3).GetValue());
        Assert.assertEquals("background-attachment", properties.get(4).GetName());
        Assert.assertEquals("fixed", properties.get(4).GetValue());

        // merge test
        NormalizeAndMergePlugin pmp = new NormalizeAndMergePlugin();
        pmp.Transform(files, new MatchedElements());

        properties = selectors.get(0).GetProperties();
        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("margin", properties.get(0).GetName());
        Assert.assertEquals("0 px", properties.get(0).GetValue());
        Assert.assertEquals("padding", properties.get(1).GetName());
        Assert.assertEquals("0", properties.get(1).GetValue());

        properties = selectors.get(1).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border", properties.get(0).GetName());
        Assert.assertEquals("1px solid #ececec", properties.get(0).GetValue());

        properties = selectors.get(2).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border", properties.get(0).GetName());
        Assert.assertEquals("2px dotted", properties.get(0).GetValue());

        properties = selectors.get(3).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border", properties.get(0).GetName());
        Assert.assertEquals("2px solid rgba(255,255,255,.7)", properties.get(0).GetValue());

        properties = selectors.get(4).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("#ececec none 100px 100px repeat fixed", properties.get(0).GetValue());

        properties = selectors.get(5).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("#ececec url(www.google.nl) left top no-repeat", properties.get(0).GetValue());

        properties = selectors.get(6).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("#ececec url(www.google.nl) left no-repeat", properties.get(0).GetValue());

        properties = selectors.get(7).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("#000000 url(test) 75% 75%", properties.get(0).GetValue());

        properties = selectors.get(8).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("margin", properties.get(0).GetName());
        Assert.assertEquals("5px", properties.get(0).GetValue());

        properties = selectors.get(9).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("margin", properties.get(0).GetName());
        Assert.assertEquals("5px 10px", properties.get(0).GetValue());

        properties = selectors.get(10).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("margin", properties.get(0).GetName());
        Assert.assertEquals("5px 10px 20px", properties.get(0).GetValue());

        properties = selectors.get(11).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("margin", properties.get(0).GetName());
        Assert.assertEquals("5px 10px 20px 30px", properties.get(0).GetValue());

        properties = selectors.get(12).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border-style", properties.get(0).GetName());
        Assert.assertEquals("solid dotted", properties.get(0).GetValue());

        properties = selectors.get(13).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border-width", properties.get(0).GetName());
        Assert.assertEquals("5px 10px 20px 30px", properties.get(0).GetValue());

        properties = selectors.get(14).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border-color", properties.get(0).GetName());
        Assert.assertEquals("#000000 rgba(136,136,136,.7) #ffffff #ececec", properties.get(0).GetValue());

        properties = selectors.get(15).GetProperties();
        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("border", properties.get(0).GetName());
        Assert.assertEquals("0", properties.get(0).GetValue());
        Assert.assertEquals("border-radius", properties.get(1).GetName());
        Assert.assertEquals("0", properties.get(1).GetValue());

        properties = selectors.get(16).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("border-top", properties.get(0).GetName());
        Assert.assertEquals("0 solid #ffffff", properties.get(0).GetValue());

        properties = selectors.get(17).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("url(google.nl) 100px/50% 50% repeat", properties.get(0).GetValue());

        properties = selectors.get(18).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("url(google.nl) left top/100px repeat", properties.get(0).GetValue());

        properties = selectors.get(19).GetProperties();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("background", properties.get(0).GetName());
        Assert.assertEquals("url(google.nl) 100px/contain no-repeat fixed", properties.get(0).GetValue());
    }

    @Test
    public void TestPropertyOrder()
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/cssnormalizer_property_order_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin cssSplitter = new NormalizeAndSplitPlugin();
        cssSplitter.Transform(files, new MatchedElements());

        List<MSelector> selectors = new ArrayList<>();
        for(MCssRule rule : files.get("external").GetRules())
        {
            selectors.addAll(rule.GetSelectors());
        }

        List<MProperty> props = selectors.get(0).GetProperties();
        Assert.assertEquals("border-width", props.get(0).GetName());
        Assert.assertEquals("border-bottom-width", props.get(1).GetName());
        Assert.assertEquals("border-bottom-style", props.get(2).GetName());
        Assert.assertEquals("border-bottom-color", props.get(3).GetName());

        NormalizeAndMergePlugin cssMerger = new NormalizeAndMergePlugin();
        cssMerger.Transform(files, new MatchedElements());

        props = selectors.get(0).GetProperties();
        Assert.assertEquals("border", props.get(0).GetName());
        Assert.assertEquals("border-bottom", props.get(1).GetName());
    }
}
