package com.crawljax.plugins.cret.util;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * Created by axel on 6/18/2015.
 */
public class CrawljaxHelper
{
    public static void configureCrawljax(CrawljaxConfiguration.CrawljaxConfigurationBuilder builder, int minutes)
    {
        //set global properties, depth of crawl, max states and max crawl-time
        builder.setMaximumDepth(3);
        builder.setMaximumStates(50);
        builder.setMaximumRunTime(minutes, TimeUnit.MINUTES);
        builder.setBrowserConfig(new BrowserConfiguration(EmbeddedBrowser.BrowserType.FIREFOX, 1));

        //crawlrules
        builder.crawlRules().clickDefaultElements();
        builder.crawlRules().clickOnce(true);
        builder.crawlRules().insertRandomDataInInputForms(false);
        builder.crawlRules().waitAfterReloadUrl(500, TimeUnit.MILLISECONDS);
        builder.crawlRules().waitAfterEvent(500, TimeUnit.MILLISECONDS);
    }
}
