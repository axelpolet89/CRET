package sass;

import helpers.TestHelper;
import com.cathive.sass.SassContext;
import com.cathive.sass.SassFileContext;
import com.cathive.sass.SassOutputStyle;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.cssmodel.MCssFile;
import com.crawljax.plugins.cret.cssmodel.MCssRule;
import com.crawljax.plugins.cret.cssmodel.MSelector;
import com.crawljax.plugins.cret.cssmodel.declarations.MDeclaration;
import com.crawljax.plugins.cret.util.FileHelper;
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

            FileOutputStream outputStream = new FileOutputStream(FileHelper.createFileAndDirs(cssTarget));
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

        List<MCssRule> refRules = refFile.getRules();
        List<MCssRule> libSassRules = libSassFile.getRules();

        Assert.assertEquals(refRules.size(), libSassRules.size());

        for(int i = 0; i < refRules.size(); i++)
        {
            List<MSelector> refSelectors = refRules.get(i).getSelectors();
            List<MSelector> libSassSelectors = libSassRules.get(i).getSelectors();

            Assert.assertEquals(refSelectors.size(), libSassSelectors.size());

            for(int j = 0; j < refSelectors.size(); j++)
            {
                List<MDeclaration> refDeclarations = refSelectors.get(j).getDeclarations();
                List<MDeclaration> libSassDeclarations = libSassSelectors.get(j).getDeclarations();

                List<MediaQuery> refMedia = refSelectors.get(j).getMediaQueries();
                List<MediaQuery> libSassMedia = libSassSelectors.get(j).getMediaQueries();

                LogHandler.debug("Checking selector %s on selector %s", refSelectors.get(j), libSassSelectors.get(j));
                Assert.assertEquals(refSelectors.get(j).getSelectorText(), libSassSelectors.get(j).getSelectorText());
                Assert.assertEquals(refDeclarations.size(), libSassDeclarations.size());
                Assert.assertEquals(refMedia.size(), libSassMedia.size());

                for(int k = 0; k < refDeclarations.size(); k++)
                {
                    Assert.assertEquals(refDeclarations.get(k).getName(), libSassDeclarations.get(k).getName());
                    if(!refDeclarations.get(k).getValue().equals(libSassDeclarations.get(k).getValue()))
                    {
                        LogHandler.warn("Mismatch declaration value! In selector %s and selector %s, declaration %s and declaration %s", refSelectors.get(j), libSassSelectors.get(j), refDeclarations.get(k), libSassDeclarations.get(k));
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
