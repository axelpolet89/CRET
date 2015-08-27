package sass;

import com.crawljax.plugins.cret.CssSuiteException;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.transformation.NormalizeAndSplitPlugin;
import com.crawljax.plugins.cret.transformation.matcher.MatchedElements;
import com.crawljax.plugins.cret.transformation.merge.NormalizeAndMergePlugin;
import com.crawljax.plugins.cret.sass.*;
import com.crawljax.plugins.cret.sass.mixins.SassCloneMixin;
import com.crawljax.plugins.cret.sass.variables.SassVariable;
import helpers.TestHelper;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/9/2015.
 */
public class SassTest
{
    public SassTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestVariables() throws CssSuiteException
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/sass_variable_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap<>();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin splitPlugin = new NormalizeAndSplitPlugin();
        NormalizeAndMergePlugin mergePlugin = new NormalizeAndMergePlugin();
        splitPlugin.transform(files, new MatchedElements());
        mergePlugin.transform(files, new MatchedElements());

        SassBuilder sassBuilder = new SassBuilder(externalFile);
        SassFile sassFile = sassBuilder.generateSass();

        List<SassVariable> vars = sassFile.getVariables();
        Assert.assertEquals(5, vars.size());
        Assert.assertEquals("$font-stack", vars.get(0).toString());
        Assert.assertEquals("\"Abel\", \"Open Sans\", \"Helvetica Neue\", Helvetica, sans-serif", vars.get(0).getValue());
        Assert.assertEquals("$color_red", vars.get(1).toString());
        Assert.assertEquals("#ff0000", vars.get(1).getValue());
        Assert.assertEquals("$color_black", vars.get(2).toString());
        Assert.assertEquals("#000000", vars.get(2).getValue());
        Assert.assertEquals("$alpha_color_white", vars.get(3).toString());
        Assert.assertEquals("rgba(255,255,255,.7)", vars.get(3).getValue());
        Assert.assertEquals("$url", vars.get(4).toString());
        Assert.assertEquals("url(/test/test.html)", vars.get(4).getValue());
    }

    @Test
    public void TestMixins() throws CssSuiteException
    {
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/resources/sass_mixin_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap<>();
        files.put("external", externalFile);

        NormalizeAndSplitPlugin splitPlugin = new NormalizeAndSplitPlugin();
        NormalizeAndMergePlugin mergePlugin = new NormalizeAndMergePlugin();
        splitPlugin.transform(files, new MatchedElements());
        mergePlugin.transform(files, new MatchedElements());

        SassBuilder sassBuilder = new SassBuilder(externalFile);
        SassFile sassFile = sassBuilder.generateSass();

        List<SassVariable> vars = sassFile.getVariables();
        List<SassCloneMixin> mixins = sassFile.getCloneMixins();
        List<SassSelector> selectors = sassFile.getRules().stream().map(r -> ((SassRule)r).getSassSelectors().get(0)).collect(Collectors.toList());

        Assert.assertEquals(4, vars.size());
        Assert.assertEquals(3, mixins.size());
        Assert.assertEquals(8, selectors.size());

        Assert.assertEquals(1, selectors.get(0).getIncludes().size());
        Assert.assertEquals(1, selectors.get(1).getIncludes().size());
        Assert.assertEquals(2, selectors.get(2).getIncludes().size());
        Assert.assertEquals(2, selectors.get(3).getIncludes().size());
        Assert.assertEquals(0, selectors.get(4).getIncludes().size());
        Assert.assertEquals(0, selectors.get(5).getIncludes().size());
        Assert.assertEquals(1, selectors.get(6).getOtherIncludes().size());
        Assert.assertEquals(1, selectors.get(7).getOtherIncludes().size());

        // convenience mixin test
        Assert.assertEquals("padding($left:20px, $right:10px, $bottom:100px)", selectors.get(6).getOtherIncludes().get(0));
        Assert.assertEquals("margin($left:20px, $right:10px, $bottom:100px)", selectors.get(7).getOtherIncludes().get(0));

        //clone mixin test is harder, due to randomness of FP-growth algoritm, different mixin names may be created in different runes
    }
}
