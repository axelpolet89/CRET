package com.crawljax.plugins.cret.examples;

import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.plugins.cret.CRET;
import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.util.CrawljaxHelper;
import com.crawljax.plugins.cret.util.FileHelper;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
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
public class VerificationRunner
{
    public static void main(String[] args)
    {
        DOMConfigurator.configure("log4verification.xml");

        try
        {
            List<String> lines = Files.readAllLines(Paths.get("./src/main/resources/random-50.txt"));

            File verificationOutput = FileHelper.createFileAndDirs("./output/verification/verification_summary.xml");
            FileWriter writer = new FileWriter(verificationOutput);
            writer.write("<sites>\n");
            writer.flush();
            writer.close();

            for(int i = 0; i < lines.size(); i++)
            {
                if (i != 49)
                    continue;

                CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(lines.get(i));
                CrawljaxHelper.configureCrawljax(builder, 1);

                CRET cretPlugin = new CRET(getNameForUrl(lines.get(i)), lines.get(i));
                cretPlugin._enableSassGeneration = true;
                cretPlugin._enableVerification = true;
                //cssSuitePlugin._enableStatistics = true;
               //cssSuitePlugin._enableW3cValidation = true;
                cretPlugin._clonePropsUpperLimit = 20;

                builder.addPlugin(cretPlugin);
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

            writer = new FileWriter(verificationOutput, true);
            writer.append("\n</sites>");
            writer.flush();
            writer.close();
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
