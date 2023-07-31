package github.antimatter.creativeworldclone;

import fi.dy.masa.malilib.util.FileUtils;

public abstract class StringUtils {
    public static String normalize(String string) {
        return FileUtils.generateSafeFileName(FileUtils.generateSimpleSafeFileName(string));
    }
}
