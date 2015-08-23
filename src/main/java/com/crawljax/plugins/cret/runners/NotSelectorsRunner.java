package com.crawljax.plugins.cret.runners;

import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.plugins.cret.FindNotSelectorsPlugin;
import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.util.CrawljaxHelper;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by axel on 6/17/2015.
 */
public class NotSelectorsRunner
{
    private final static String outputFile = "./output/notselectors/not_selectors_analysis.xml";
    private final static String outputFileDetails = "./output/notselectors/not_selectors_details.xml";

    public static void main(String[] args)
    {
        DOMConfigurator.configure("log4notselectors.xml");

        try
        {
            FileWriter writer = new FileWriter(outputFile);
            writer.write("<sites>");
            writer.flush();

            FileWriter detailWriter = new FileWriter(outputFileDetails);
            detailWriter.write("<sites>");
            detailWriter.flush();

            List<String> lines = Files.readAllLines(Paths.get("./src/main/resources/random-50.txt"));

            for(int i = 0; i < lines.size(); i++)
            {

                if(i !=49)
                    continue;

                CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(lines.get(i));
                CrawljaxHelper.configureCrawljax(builder, 1);

                FindNotSelectorsPlugin notSelectorsPlugin = new FindNotSelectorsPlugin(getNameForUrl(lines.get(i)), lines.get(i), writer, detailWriter);

                builder.addPlugin(notSelectorsPlugin);

                CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());

                try
                {
                    crawljax.call();
                }
                catch(Exception ex)
                {
                    LogHandler.error(ex, "Error occurred while crawling for site '%s'", lines.get(i));
                }
            }

            writer.append("</sites>");
            writer.flush();
            writer.close();

            detailWriter.append("</sites>");
            detailWriter.flush();
            detailWriter.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private static String getNameForUrl(String url)
    {
        try
        {
            URI uri = new URI(url);
            return uri.getHost();
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }

        return url;
    }
}
