package com.crawljax.plugins.csssuite.parser;

import com.crawljax.plugins.csssuite.util.SuiteStringBuilder;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by axel on 6/15/2015.
 *
 * Wrapper that implements SAC parser error handling
 * Used in CssParser
 */
public class ParserErrorHandler implements ErrorHandler
{
    private List<CSSParseException> _warnings;
    private List<CSSParseException> _errors;
    private List<CSSParseException> _fatalErrors;

    public ParserErrorHandler()
    {
        _warnings = new ArrayList<>();
        _errors = new ArrayList<>();
        _fatalErrors = new ArrayList<>();
    }

    public List<String> PrintParseErrors()
    {
        List<String> result = new ArrayList<>();

        List<CSSParseException> allMatchedErrors = new ArrayList<>();
        for(CSSParseException warning : _warnings)
        {
            SuiteStringBuilder builder = new SuiteStringBuilder();
            builder.append("[Parse Warning] at line " + warning.getLineNumber() + ":");
            builder.append("\t" + warning.getMessage());

            List<CSSParseException> matchedErrors = _errors.stream()
                                                            .filter(error -> error.getLineNumber() == warning.getLineNumber() && error.getColumnNumber() == warning.getColumnNumber())
                                                            .collect(Collectors.toList());

            for(CSSParseException error : matchedErrors)
            {
                builder.appendLine("\t-related error- " + error.getMessage());
            }
            allMatchedErrors.addAll(matchedErrors);

            result.add(builder.toString());
        }

        for(CSSParseException extraError : _errors.stream().filter(error -> !allMatchedErrors.contains(error)).collect(Collectors.toList()))
        {
            SuiteStringBuilder builder = new SuiteStringBuilder();
            builder.append("[Parse Error] at line " + extraError.getLineNumber());
            builder.appendLine("\t" + extraError.getMessage());
            result.add(builder.toString());
        }

        return result;
    }

    @Override
    public void warning(CSSParseException e) throws CSSException
    {
        _warnings.add(e);
    }

    @Override
    public void error(CSSParseException e) throws CSSException
    {
        _errors.add(e);
    }

    @Override
    public void fatalError(CSSParseException e) throws CSSException
    {
        _fatalErrors.add(e);
    }
}