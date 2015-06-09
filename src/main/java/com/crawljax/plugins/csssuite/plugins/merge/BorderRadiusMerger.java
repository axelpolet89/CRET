package com.crawljax.plugins.csssuite.plugins.merge;

import com.crawljax.plugins.csssuite.data.properties.MProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Created by axel on 5/27/2015.
 */
public class BorderRadiusMerger extends MergerBase
{
    private String _topLeft;
    private String _topRight;
    private String _bottomRight;
    private String _bottomLeft;

    public BorderRadiusMerger(String name)
    {
        super(name);

        _topLeft = "";
        _topRight = "";
        _bottomRight = "";
        _bottomLeft = "";
    }


    /**
     *
     * @param name
     * @param value
     */
    protected void ParseFromSingle(String name, String value)
    {
        String[] parts = name.split("-");
        String vert = parts[1];
        String hor = parts[2];

        switch (vert)
        {
            case "top":
                switch(hor)
                {
                    case "left":
                        _topLeft = value;
                        break;
                    case "right":
                        _topRight = value;
                        break;
                }
                break;
            case "bottom":
                switch(hor)
                {
                    case "left":
                        _bottomLeft = value;
                        break;
                    case "right":
                        _bottomRight = value;
                        break;
                }

                break;
        }

        _isSet = true;
    }


    @Override
    public List<MProperty> BuildMProperties()
    {
        String value;

        String[] tl = _topLeft.split("\\s");
        String[] tr = _topRight.split("\\s");
        String[] br = _bottomRight.split("\\s");
        String[] bl = _bottomLeft.split("\\s");

        String tl2 = "";
        String tr2 = "";
        String br2 = "";
        String bl2 = "";

        if(tl.length == 2)
            tl2 = tl[1];
        if(tr.length == 2)
            tr2 = tr[1];
        if(br.length == 2)
            br2 = br[1];
        if(bl.length == 2)
            bl2 = bl[1];

        String part1 = "";
        String part2 = "";

        if(!tl2.isEmpty() && !tr2.isEmpty() && !br2.isEmpty() && !bl2.isEmpty())
        {
            part2 = BuildPart(tl2, tr2, br2, bl2);
        }

        part1 = BuildPart(tl[0], tr[0], br[0], bl[0]);

        if(!part2.isEmpty())
            value = String.format("%s / %s", part1, part2);
        else
            value = part1;

        return Arrays.asList(new MProperty("border-radius", value, _isImportant, true));
    }

    private static String BuildPart(String tl, String tr, String br, String bl)
    {
        String value;

        if(tl.equals(tr) && tl.equals(br) && tl.equals(bl))
        {
            value = tl;
        }
        else if(tl.equals(br) && tr.equals(bl))
        {
            value = String.format("%s %s", tl, tr);
        }
        else if(tr.equals(bl))
        {
            value = String.format("%s %s %s", tl, tr, br);
        }
        else
        {
            value = String.format("%s %s %s %s", tl, tr, br, bl);
        }

        return value;
    }
}
