package analysis;

import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.plugins.ClonedDeclarationsPlugin;
import com.crawljax.plugins.cret.plugins.NormalizeAndSplitPlugin;
import com.crawljax.plugins.cret.plugins.matcher.MatchedElements;
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
public class ClonedDeclarationsTest
{
    public ClonedDeclarationsTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void testDuplicateDeclarations()
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/css-duplicate-properties_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap<>();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin splitPlugin = new NormalizeAndSplitPlugin();
        ClonedDeclarationsPlugin clonedDeclarations = new ClonedDeclarationsPlugin();

        splitPlugin.transform(files, new MatchedElements());
        clonedDeclarations.transform(files, new MatchedElements());

        List<MSelector> selectors = new ArrayList<>();
        for(MCssRule rule : files.get("external").getRules())
        {
            selectors.addAll(rule.getSelectors());
        }

        List<MDeclaration> mDeclarations = selectors.get(0).getDeclarations();
        Assert.assertEquals(5,mDeclarations.size());
        Assert.assertEquals("border-width: 1px;", mDeclarations.get(2).toString());
        Assert.assertEquals("border-style: solid;", mDeclarations.get(3).toString());
        Assert.assertEquals("border-color: #ff0000;", mDeclarations.get(4).toString());


        mDeclarations = selectors.get(1).getDeclarations();
        Assert.assertEquals(4,mDeclarations.size());
        Assert.assertEquals("padding-top: 40px;", mDeclarations.get(0).toString());
        Assert.assertEquals("padding-right: 40px;", mDeclarations.get(1).toString());
        Assert.assertEquals("padding-bottom: 40px;", mDeclarations.get(2).toString());
        Assert.assertEquals("padding-left: 40px;", mDeclarations.get(3).toString());

        mDeclarations = selectors.get(2).getDeclarations();
        Assert.assertEquals(4,mDeclarations.size());
        Assert.assertEquals("padding-top: 30px !important;", mDeclarations.get(0).toString());
        Assert.assertEquals("padding-right: 30px !important;", mDeclarations.get(1).toString());
        Assert.assertEquals("padding-bottom: 30px !important;", mDeclarations.get(2).toString());
        Assert.assertEquals("padding-left: 30px !important;", mDeclarations.get(3).toString());
    }
}
