package helpers;

import com.crawljax.plugins.csssuite.data.MCssFile;
import com.crawljax.plugins.csssuite.data.MSelector;
import com.crawljax.plugins.csssuite.parser.CssParser;
import com.crawljax.plugins.csssuite.util.specificity.SpecificitySelector;
import com.crawljax.util.DomUtils;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.StringReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by axel on 5/20/2015.
 */
public class TestHelper
{
    public TestHelper()
    {
        DOMConfigurator.configure("log4j.xml");
    }

    public static Document GetDocumentFromFile(String path)
    {
        String contents = GetStringFromFile(path);

        if(contents.isEmpty())
            return null;

        try
        {
            return DomUtils.asDocument(contents);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static MCssFile GetCssFileFromFile(String path)
    {
        String contents = GetStringFromFile(path);
        if(contents.isEmpty())
            return null;

        CssParser parser = new CssParser();
        MCssFile mCssFile = parser.ParseCssIntoMCssRules(path, contents);

        if(parser.GetParseErrors().size() > 0)
        {
            for(String parseError : parser.GetParseErrors())
                System.out.println(String.format("Incorrect CSS in file '%s' -> 's'", path, parseError));

            return null;
        }

        return mCssFile;
    }

    public static MCssFile GetCssFromString(String name, String cssCode)
    {
        CssParser parser = new CssParser();
        MCssFile mCssFile  = parser.ParseCssIntoMCssRules(name, cssCode);

        if(parser.GetParseErrors().size() > 0)
        {
            for(String parseError : parser.GetParseErrors())
                System.out.println(String.format("Incorrect CSS in file '%s' -> 's'", name, parseError));

            return null;
        }

        return mCssFile;
    }


    private static String GetStringFromFile(String path)
    {
        try
        {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded, StandardCharsets.UTF_8);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return "";
    }

    public static SpecificitySelector CreateSpecificitySelector(String selector, int ruleNumber) throws IOException
    {
        return CreateSpecificitySelector(selector, ruleNumber, 0);
    }

    public static SpecificitySelector CreateSpecificitySelector(String selector, int ruleNumber, int order) throws IOException
    {
        return new SpecificitySelector(CreateEmptySelector(selector, ruleNumber), order);
    }

    public static MSelector CreateEmptySelector(String selector) throws IOException
    {
        return new MSelector(ParseSelector(selector), new ArrayList<>(), 1, new ArrayList<>());
    }

    public static MSelector CreateEmptySelector(String selector, int ruleNumber) throws IOException
    {
        return new MSelector(ParseSelector(selector), new ArrayList<>(), ruleNumber, new ArrayList<>());
    }

    public static Selector ParseSelector(String selector) throws IOException
    {
        InputSource source = new InputSource(new StringReader(selector));
        CSSOMParser cssomParser = new CSSOMParser(new SACParserCSS3());
        return cssomParser.parseSelectors(source).item(0);
    }
}