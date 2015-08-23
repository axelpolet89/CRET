package com.crawljax.plugins.cret.parser;

import com.crawljax.plugins.cret.LogHandler;
import com.jcabi.w3c.ValidationResponse;
import com.jcabi.w3c.ValidatorBuilder;

import java.io.IOException;

/**
 * Created by axel on 5/29/2015.
 *
 * W3C validation service wrapper
 */
public class CssValidator
{
    public static ValidationResponse validateW3C(String cssCode) throws IOException
    {
        LogHandler.info("[W3C Validator] start css validation");

        long startTime = System.currentTimeMillis();
        ValidationResponse response = new ValidatorBuilder().css().validate(cssCode);
        long estimatedTime = System.currentTimeMillis() - startTime;

        LogHandler.info("[W3C Validator] elapsed time: %d", estimatedTime);

        return response;
    }
}
