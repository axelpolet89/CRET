package sass;

import com.crawljax.plugins.csssuite.CssSuiteException;
import com.crawljax.plugins.csssuite.plugins.sass.colors.ColorNameFinder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Test;

/**
 * Created by axel on 6/9/2015.
 */
public class SassVariableTest
{
    public SassVariableTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestColorToName() throws CssSuiteException
    {
        ColorNameFinder ctn = new ColorNameFinder();

        String test1 = ctn.TryGetNameForRgb(0, 0, 0);
        String test2 = ctn.TryGetNameForRgb(255, 255, 255);
        String test3 = ctn.TryGetNameForRgb(123, 23, 244);
        String test4 = ctn.TryGetNameForRgb(44, 34, 34);
    }
}
