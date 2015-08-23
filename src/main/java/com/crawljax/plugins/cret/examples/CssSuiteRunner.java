package com.crawljax.plugins.cret.examples;

import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.plugins.cret.CRET;
import com.crawljax.plugins.cret.util.CrawljaxHelper;
import org.apache.log4j.xml.DOMConfigurator;

public class CssSuiteRunner
{
	private static final String INDEX = "http://www.beckerelectric.com";

	public static void main(String[] args)
	{
		DOMConfigurator.configure("log4j.xml");

		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(INDEX);
		CrawljaxHelper.configureCrawljax(builder, 1);

		CRET cretPlugin = new CRET("beckerelectric", INDEX);
		cretPlugin._enableSassGeneration = true;

		builder.addPlugin(cretPlugin);

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
	}
}
