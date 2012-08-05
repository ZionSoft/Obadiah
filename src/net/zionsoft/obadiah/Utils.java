package net.zionsoft.obadiah;

import java.io.File;

public class Utils
{
    static public boolean removeDirectory(File directory)
    {
        if (directory == null)
            return true;
        File[] files = directory.listFiles();
        for (File file : files)
            file.delete();
        return directory.delete();
    }
}
