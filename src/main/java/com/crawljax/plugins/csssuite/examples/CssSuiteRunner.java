package com.crawljax.plugins.csssuite.examples;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.plugins.csssuite.CRET;

import java.util.concurrent.TimeUnit;

public class CssSuiteRunner {

	private static final int waitAfterEvent = 500;
	private static final int waitAfterReload = 500;

	//private static final String INDEX = "http://www.beckerelectric.com";
	private static final String INDEX = "http://localhost/test/index.html";

	public static void main(String[] args)
	{
		Run();
	}

	private static void Run()
	{
		CRET cillaPlugin = new CRET("localhost", INDEX);
		cillaPlugin.enableDebug();

		CrawljaxRunner crawljax = new CrawljaxRunner(createCrawljaxConfig(10, cillaPlugin));
		crawljax.call();
	}

	private static CrawljaxConfiguration createCrawljaxConfig(int maxRunTime, CRET plugin){
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(INDEX);

		//set global properties, depth of crawl, max states and max crawl-time
		builder.setMaximumDepth(3);
		builder.setMaximumStates(50);
		builder.setMaximumRunTime(maxRunTime, TimeUnit.MINUTES);
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.FIREFOX, 1));

		//crawlrules
		builder.crawlRules().clickOnce(true);
		builder.crawlRules().insertRandomDataInInputForms(false);
		builder.crawlRules().waitAfterReloadUrl(waitAfterReload, TimeUnit.MILLISECONDS);
		builder.crawlRules().waitAfterEvent(waitAfterEvent, TimeUnit.MILLISECONDS);

		//click a, input, button, div with onclick, span with onclick
		builder.crawlRules().clickDefaultElements();
		builder.crawlRules().click("div").withAttribute("onclick","*");
		builder.crawlRules().click("span").withAttribute("onclick","*");


		builder.addPlugin(plugin);

		return builder.build();
	}
}
