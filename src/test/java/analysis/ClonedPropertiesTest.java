package analysis;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.properties.MProperty;
import com.crawljax.plugins.csssuite.plugins.DetectClonedPropertiesPlugin;
import com.crawljax.plugins.csssuite.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.csssuite.plugins.analysis.MatchedElements;
import helpers.TestHelper;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Created by axel on 6/2/2015.
 */
public class ClonedPropertiesTest
{
    public ClonedPropertiesTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestDuplicateProperties()
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/css-duplicate-properties_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap<>();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin splitPlugin = new NormalizeAndSplitPlugin();
        DetectClonedPropertiesPlugin clonedProperties = new DetectClonedPropertiesPlugin();

        splitPlugin.Transform(files, new MatchedElements());
        clonedProperties.Transform(files, new MatchedElements());

        List<MSelector> selectors = new ArrayList<>();
        for(MCssRule rule : files.get("external").GetRules())
        {
            selectors.addAll(rule.GetSelectors());
        }

        List<MProperty> properties = selectors.get(0).GetProperties();
        Assert.assertEquals(5,properties.size());
        Assert.assertEquals("border-width: 1px;", properties.get(2).toString());
        Assert.assertEquals("border-style: solid;", properties.get(3).toString());
        Assert.assertEquals("border-color: #ff0000;", properties.get(4).toString());


        properties = selectors.get(1).GetProperties();
        Assert.assertEquals(4,properties.size());
        Assert.assertEquals("padding-top: 40px;", properties.get(0).toString());
        Assert.assertEquals("padding-right: 40px;", properties.get(1).toString());
        Assert.assertEquals("padding-bottom: 40px;", properties.get(2).toString());
        Assert.assertEquals("padding-left: 40px;", properties.get(3).toString());

        properties = selectors.get(2).GetProperties();
        Assert.assertEquals(4,properties.size());
        Assert.assertEquals("padding-top: 30px !important;", properties.get(0).toString());
        Assert.assertEquals("padding-right: 30px !important;", properties.get(1).toString());
        Assert.assertEquals("padding-bottom: 30px !important;", properties.get(2).toString());
        Assert.assertEquals("padding-left: 30px !important;", properties.get(3).toString());
    }
}
