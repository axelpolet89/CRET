package sass;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.sass.SassBuilder;
import com.crawljax.plugins.csssuite.plugins.merge.NormalizeAndMergePlugin;

import helpers.TestHelper;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by axel on 6/5/2015.
 */
public class CloneDetectorTest
{
    public CloneDetectorTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestCloneDetector()
    {
        MCssFile externalFile1 = TestHelper.GetCssFileFromFile("./src/test/resources/clonedetector_2_test.css");
        Assert.assertNotNull(externalFile1);

        MCssFile externalFile2 = TestHelper.GetCssFileFromFile("./src/test/resources/clonedetector_test.css");
        Assert.assertNotNull(externalFile2);

        MCssFile externalFile3 = TestHelper.GetCssFileFromFile("./src/test/resources/clonedetector_3_test.css");
        Assert.assertNotNull(externalFile3);

        Map<String, MCssFile> files = new HashMap();
        files.put("clonedetector_2_test.css", externalFile1);
        files.put("clonedetector_test.css", externalFile2);
        files.put("clonedetector_3_test.css", externalFile3);

        NormalizeAndMergePlugin normalizer = new NormalizeAndMergePlugin();
        normalizer.Transform(files);

        SassBuilder gen = new SassBuilder();
        gen.CssToSass(files);
    }
}
