package cssparser;

import com.crawljax.plugins.cret.parser.CssValidator;
import com.crawljax.plugins.cret.util.CretStringBuilder;
import com.jcabi.w3c.ValidationResponse;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by axel on 5/29/2015.
 */
public class CssValidatorTest
{
    public CssValidatorTest()
    {
        DOMConfigurator.configure("log4j.xml");
    }

    @Test
    public void TestCssValidator() throws IOException
    {
        CretStringBuilder builder = new CretStringBuilder();

        builder.append("div, a, span{\nbackground: solid; }"); //incorrect background value --> error
        builder.appendLine("#id, .class, span[attr=\"test\"], a:hover, span::before{\ncolor: black;\nmargin 10 px;} "); //second declaration syntax-error
        builder.appendLine("h p[att=\"test\"{\ncolor: red; }"); //syntax-error, unclosed attribute --> error
        builder.appendLine("div {\n color black;}"); // no colon --> parse error
        builder.appendLine("div {\n hyphenate: none;}"); // no such declaration --> error
        builder.appendLine("div {\n-moz-box-shadow:10px 5px 5px black;}"); //unknown vendor specification --> warning
        builder.appendLine("span:invalid, div, a:valid { color: black; }"); //first and third selector are errors, because invalid pseudo-classes

        ValidationResponse response = CssValidator.validateW3C(builder.toString());

        Assert.assertEquals(7, response.errors().size());
        Assert.assertEquals(1, response.warnings().size());
    }
}
