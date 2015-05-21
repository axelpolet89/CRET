package com.crawljax.plugins.csssuite.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.crawljax.plugins.csssuite.data.ElementWithClass;
import com.crawljax.util.UrlUtils;
import com.crawljax.util.XPathHelper;

public class CSSDOMHelper
{
	/**
	 * Extract all css file names from the link elements in the head of the document
	 * @param dom
	 * @return a list of css file names
	 */
	public static List<String> ExtractCssFileNames(Document dom)
	{
		List<String> cssFileNames = new ArrayList<>();

		NodeList linkTags = dom.getElementsByTagName("link");
		if (linkTags != null)
		{
			for (int i = 0; i < linkTags.getLength(); i++)
			{
				Node linkNode = linkTags.item(i);
				Node rel = linkNode.getAttributes().getNamedItem("rel");

				if (rel != null && rel.getNodeValue().toString().equalsIgnoreCase("stylesheet")) {
					Node href = linkNode.getAttributes().getNamedItem("href");

					if (href != null)
					{
						cssFileNames.add(href.getNodeValue().toString());
					}
				}

			}
		}

		return cssFileNames;
	}


	/**
	 * @param location
	 *            the URL location of the page (http://www.global.com).
	 * @param relUrl
	 *            the (relative) URL of the file (e.g ../../world/news.css).
	 * @return the absolute path of the file.
	 */
	public static String GetAbsPath(String location, String relUrl) {

		if (relUrl.startsWith("http")) {
			return relUrl;
		}

		// Example: /default.css
		if (relUrl.startsWith("/")) {
			return UrlUtils.getBaseUrl(location) + relUrl;
		}

		// it is relative, example: ../../default.css
		String loc = location.substring(0, location.lastIndexOf('/'));

		while (relUrl.contains("../")) {
			relUrl = relUrl.substring(3);
			loc = loc.substring(0, loc.lastIndexOf('/'));
		}

		return loc + '/' + relUrl;
	}


	/**
	 * @param url The URL.
	 * @return the content (string) of resource.
	 * @throws IOException
	 */
	public static String GetUrlContent(String url) throws IOException {

		GetMethod method = new GetMethod(url);

		int returnCode = new HttpClient().executeMethod(method);
		if (returnCode == 200)
		{
			return method.getResponseBodyAsString();
		}

		return "";
	}


	/**
	 * @param dom the document object.
	 * @return the content of all the embedded css rules that are defined inside <STYLE> elements.
	 */
	public static String ParseEmbeddedStyles(Document dom)
	{
		NodeList styles = dom.getElementsByTagName("style");

		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < styles.getLength(); i++) {
			Node style = styles.item(i);

			buffer.append(style.getTextContent() + "\n");
		}

		return buffer.toString();
	}


	/**
	 *
	 * @param stateName
	 * @param dom
	 * @return a list of elements inside the dom that have a class attribute
	 * @throws XPathExpressionException
	 */
	public static List<ElementWithClass> GetElementWithClass(String stateName, Document dom)
	        throws XPathExpressionException {

		final List<ElementWithClass> results = new ArrayList<>();
		final NodeList nodes = XPathHelper.evaluateXpathExpression(dom, "./descendant::*[@class != '']");

		for (int i = 0; i < nodes.getLength(); i++)
		{
			Element node = (Element) nodes.item(i);
			String classValue = node.getAttributeNode("class").getValue();

			results.add(new ElementWithClass(stateName, node, Arrays.asList(classValue.split(" "))));
		}

		return results;
	}
}
