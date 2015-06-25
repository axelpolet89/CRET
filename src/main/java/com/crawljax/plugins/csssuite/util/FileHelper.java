package com.crawljax.plugins.csssuite.util;

import com.crawljax.plugins.csssuite.LogHandler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by axel on 6/11/2015.
 */
public class FileHelper
{
    public static File CreateFileAndDirs(String fileName) throws IOException
    {
        File file = new File(fileName);
        File dir = new File(fileName.replace(file.getName(), ""));

        if(!dir.exists())
        {
            dir.mkdirs();
        }

        try
        {
            if (!file.exists())
            {
                file.createNewFile();
            }
        }
        catch(IOException ex)
        {
            LogHandler.error("Error in creating new file '%s'\nwith name '%s'", file, file.getName());
            throw(ex);
        }

        return file;
    }
}
