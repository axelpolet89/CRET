package com.crawljax.plugins.cret.sass.mixins;

import com.crawljax.plugins.cret.data.declarations.MDeclaration;
import com.crawljax.plugins.cret.util.CretStringBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Created by axel on 6/10/2015.
 */
public class SassBoxMixin extends SassMixinBase
{
    public SassBoxMixin(String name)
    {
        super(name,
                Arrays.asList("$top:null", "$bottom:null", "$right:null", "$left:null"),
                Arrays.asList(String.format("%s-top: $top;", name), String.format("%s-bottom: $bottom;", name), String.format("%s-right: $right;", name), String.format("%s-left: $left;", name)));
    }

    public String createMixinCall(List<MDeclaration> declarations)
    {
        CretStringBuilder builder = new CretStringBuilder();
        builder.append("%s(", _name);

        boolean prev = false;

        for(MDeclaration mDeclaration : declarations)
        {
            String name = mDeclaration.getName();
            String value = mDeclaration.getFullValue();

            if(name.contains("top"))
            {
                if(prev)
                    builder.append(", ");

                builder.append("$top:%s", value);
                prev = true;
            }
            else if(name.contains("bottom"))
            {
                if(prev)
                    builder.append(", ");

                builder.append("$bottom:%s", value);
                prev = true;

            }
            else if(name.contains("right"))
            {
                if(prev)
                    builder.append(", ");

                builder.append("$right:%s", value);
                prev = true;
            }
            else
            {
                if(prev)
                    builder.append(", ");

                builder.append("$left:%s", value);
                prev = true;
            }
        }

        builder.append(")");
        return builder.toString();
    }
}