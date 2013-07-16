package net.zionsoft.obadiah.support;

import java.io.File;

public class Utils {
    static public boolean removeDirectory(File directory) {
        if (directory == null)
            return true;
        final File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                removeDirectory(file);
            else
                file.delete();
        }
        return directory.delete();
    }
}
