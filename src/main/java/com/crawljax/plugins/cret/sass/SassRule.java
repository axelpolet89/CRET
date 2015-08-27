package com.crawljax.plugins.cret.sass;

import com.crawljax.plugins.cret.LogHandler;
import com.crawljax.plugins.cret.util.CretStringBuilder;
import com.steadystate.css.parser.media.MediaQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by axel on 6/9/2015.
 */
public class SassRule extends SassRuleBase
{
    private final List<SassSelector> _sassSelectors;
    private final List<MediaQuery> _mediaQueries;

    public SassRule(int lineNumber, List<SassSelector> sassSelectors)
    {
        super(lineNumber);

        _sassSelectors = sassSelectors;
        _mediaQueries = new ArrayList<>();

        boolean isSet = false;
        for(SassSelector ss : _sassSelectors)
        {
            if(isSet)
            {
                if(ss.getMediaQueries().containsAll(_mediaQueries) && _mediaQueries.containsAll(ss.getMediaQueries()))
                    continue;

                LogHandler.error("Critical: found a SassRule that holds selectors with different media-queries. Original line: %d, mismatch on selector %s", lineNumber, ss.getSelectorText());
            }

            _mediaQueries.addAll(ss.getMediaQueries());
            isSet = true;
        }
    }


    /** Getter */
    public List<SassSelector> getSassSelectors()
    {
        return _sassSelectors;
    }


    public void print(CretStringBuilder builder, String prefix)
    {
        String selectorGroup = "";

        for(int i = 0; i < _sassSelectors.size(); i++)
        {
            SassSelector sassSelector = _sassSelectors.get(i);
            selectorGroup += sassSelector.getSelectorText();
            if(i < _sassSelectors.size() - 1)
                selectorGroup += ", ";
        }

        builder.append("%s%s", prefix, selectorGroup);
        builder.append("{", prefix);
        _sassSelectors.get(0).printContents(builder, prefix);
        builder.appendLine("%s}", prefix);
    }
}
