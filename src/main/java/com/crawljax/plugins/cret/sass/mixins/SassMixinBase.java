package com.crawljax.plugins.cret.sass.mixins;

import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.List;

/**
 * Created by axel on 6/10/2015.
 */
public abstract class SassMixinBase
{
    protected String _name;
    private List<String> _parameters;
    private List<String> _contents;

    protected SassMixinBase(String name, List<String> parameters, List<String> contents)
    {
        _name = name;
        _parameters = parameters;
        _contents = contents;
    }

    public void print(CretStringBuilder builder)
    {
        builder.append("@mixin %s(", _name);

        for(int i = 0; i < _parameters.size(); i++)
        {
            if(i < _parameters.size() - 1)
            {
                builder.append("%s, ", _parameters.get(i));
            }
            else
            {
                builder.append(_parameters.get(i));
            }
        }

        builder.append("){");

        for(String line : _contents)
        {
            builder.appendLine("\t%s", line);
        }

        builder.appendLine("}");
    }
}