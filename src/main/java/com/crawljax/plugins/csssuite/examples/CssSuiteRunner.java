package com.crawljax.plugins.csssuite.examples;

import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.plugins.csssuite.CRET;
import com.crawljax.plugins.csssuite.util.CrawljaxHelper;

public class CssSuiteRunner
{
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
		CRET cretPlugin = new CRET("localhost", INDEX);
		cretPlugin.enableDebug();

		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(INDEX);
		CrawljaxHelper.configureCrawljax(builder, 1);

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
	}
}
