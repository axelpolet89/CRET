package analysis;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.plugins.MatchedElements;
import com.crawljax.plugins.csssuite.plugins.sass.CloneDetector;
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
        MCssFile externalFile = TestHelper.GetCssFileFromFile("./src/test/test_files/clonedetector_3_test.css");
        Assert.assertNotNull(externalFile);

        Map<String, MCssFile> files = new HashMap();
        files.put("external", externalFile);

        CloneDetector cloneDetector = new CloneDetector();
        cloneDetector.Transform(files);


    }
}
