package com.crawljax.plugins.csssuite.examples;

import java.util.concurrent.TimeUnit;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.plugins.csssuite.CssSuitePlugin;

public class Beckers {

	private static final int waitAfterEvent = 400;
	private static final int waitAfterReload = 400;

	private static final String INDEX = "http://www.beckerelectric.com";

	public static void main(String[] args) {
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(INDEX);
		builder.crawlRules().insertRandomDataInInputForms(false);

		// Set timeouts
		builder.crawlRules().waitAfterReloadUrl(waitAfterReload, TimeUnit.MILLISECONDS);
		builder.crawlRules().waitAfterEvent(waitAfterEvent, TimeUnit.MILLISECONDS);
		builder.setMaximumDepth(3);
		builder.crawlRules().clickOnce(true);

		builder.crawlRules().click("a");

		builder.setMaximumStates(2);

		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.FIREFOX, 1));

		builder.addPlugin(new CssSuitePlugin());

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();

	}

}
