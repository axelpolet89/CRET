package sass;

import helpers.TestHelper;
import com.cathive.sass.SassContext;
import com.cathive.sass.SassFileContext;
import com.cathive.sass.SassOutputStyle;

import com.crawljax.plugins.csssuite.LogHandler;
import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MCssRule;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.data.declarations.MDeclaration;
import com.crawljax.plugins.csssuite.util.FileHelper;
import com.steadystate.css.parser.media.MediaQuery;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;


import java.io.File;
import java.io.FileOutputStream;
import java.util.List;


/**
 * Created by axel on 6/5/2015.
 */
public class JavaLibSassTest
{
    public JavaLibSassTest()
    {
        DOMConfigurator.configure("log4j.xml");
        LogManager.getLogger("css.suite.logger").setLevel(Level.DEBUG);
    }

    @Test
    public void TestCloneDetector()
    {
        try
        {
            String scssSource = "./src/test/resources/becker-style.scss";
            String cssTarget = "./src/test/test_output/becker-style.css";

            SassContext ctx = SassFileContext.create(new File(scssSource).toPath());
            ctx.getOptions().setOutputStyle(SassOutputStyle.NESTED);

            FileOutputStream outputStream = new FileOutputStream(FileHelper.CreateFileAndDirs(cssTarget));
            ctx.compile(outputStream);

            outputStream.flush();
            outputStream.close();
        }
        catch (Exception e)
        {
            LogHandler.error(e);
        }

        MCssFile refFile = TestHelper.GetCssFileFromFile("./src/test/resources/becker-style-ruby-sass-ref.css");
        MCssFile libSassFile = TestHelper.GetCssFileFromFile("./src/test/test_output/becker-style.css");

        List<MCssRule> refRules = refFile.GetRules();
        List<MCssRule> libSassRules = libSassFile.GetRules();

        Assert.assertEquals(refRules.size(), libSassRules.size());

        for(int i = 0; i < refRules.size(); i++)
        {
            List<MSelector> refSelectors = refRules.get(i).GetSelectors();
            List<MSelector> libSassSelectors = libSassRules.get(i).GetSelectors();

            Assert.assertEquals(refSelectors.size(), libSassSelectors.size());

            for(int j = 0; j < refSelectors.size(); j++)
            {
                List<MDeclaration> refProperties = refSelectors.get(j).GetDeclarations();
                List<MDeclaration> libSassProperties = libSassSelectors.get(j).GetDeclarations();

                List<MediaQuery> refMedia = refSelectors.get(j).GetMediaQueries();
                List<MediaQuery> libSassMedia = libSassSelectors.get(j).GetMediaQueries();

                LogHandler.debug("Checking selector %s on selector %s", refSelectors.get(j), libSassSelectors.get(j));
                Assert.assertEquals(refSelectors.get(j).GetSelectorText(), libSassSelectors.get(j).GetSelectorText());
                Assert.assertEquals(refProperties.size(), libSassProperties.size());
                Assert.assertEquals(refMedia.size(), libSassMedia.size());

                for(int k = 0; k < refProperties.size(); k++)
                {
                    Assert.assertEquals(refProperties.get(k).GetName(), libSassProperties.get(k).GetName());
                    if(!refProperties.get(k).GetValue().equals(libSassProperties.get(k).GetValue()))
                    {
                        LogHandler.warn("Mismatch property value! In selector %s and selector %s, property %s and property %s", refSelectors.get(j), libSassSelectors.get(j), refProperties.get(k), libSassProperties.get(k));
                    }
                }

                for(int k = 0; k < refMedia.size(); k++)
                {
                    Assert.assertEquals(refMedia.get(k).getMedia(), libSassMedia.get(k).getMedia());
                }
            }
        }
    }
}
