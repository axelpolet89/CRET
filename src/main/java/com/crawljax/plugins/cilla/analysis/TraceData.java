package com.crawljax.plugins.cilla.analysis;

import org.apache.log4j.Logger;

public class TraceData
{
	private static final Logger LOGGER = Logger.getLogger(TraceData.class.getName());
	private String _trace;
	private String _selector;
	private String _url;
	private int lineNumber;

	public TraceData(String trace) {

		_trace = trace;
		Parse();
	}

	private void Parse() {
		LOGGER.debug("PARSING: " + _trace);

		_selector = _trace.substring(_trace.indexOf('{') + 1, _trace.indexOf('}'));
		_selector = _selector.replaceAll("\\*", "");
		lineNumber = Integer.parseInt(_trace.substring(_trace.indexOf('[') + 1, _trace.indexOf(']')));
		_url = _trace.substring(0, _trace.indexOf('['));
	}

	public String GetSelector() {
		return _selector;
	}

	public String GetUrl() {
		return _url;
	}

	public int GetLineNumber() {
		return lineNumber;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("Selector " + _selector + "\n");
		buffer.append("Linenumber " + lineNumber + "\n");

		return buffer.toString();
	}
}
