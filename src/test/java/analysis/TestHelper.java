package analysis;

import com.crawljax.plugins.cilla.data.MSelector;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by axel on 5/20/2015.
 */
public class TestHelper
{
    public static MSelector CreateSelector(String selector) throws IOException
    {
        return new MSelector(ParseSelector(selector), 1);
    }

    public static MSelector CreateSelector(String selector, int ruleNumber) throws IOException
    {
        return new MSelector(ParseSelector(selector), ruleNumber);
    }

    public static Selector ParseSelector(String selector) throws IOException
    {
        InputSource source = new InputSource(new StringReader(selector));
        CSSOMParser cssomParser = new CSSOMParser(new SACParserCSS3());
        return cssomParser.parseSelectors(source).item(0);
    }
}
