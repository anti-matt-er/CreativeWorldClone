package github.antimatter.creativeworldclone;

import fi.dy.masa.malilib.util.FileUtils;

public abstract class StringUtils {
    public static String normalize(String string) {
        return FileUtils.generateSafeFileName(FileUtils.generateSimpleSafeFileName(string));
    }

    public static String removeSuffix(String string, String suffix) {
        if (string.endsWith(suffix))
            string = string.substring(0, string.length() - suffix.length());

        return string;
    }
}
