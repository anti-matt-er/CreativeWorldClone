package github.antimatter.creativeworldclone;

import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ConfigHandler {
    private static final String PREFIX = "CC_";
    private static final github.antimatter.creativeworldclone.CloneConfig CONFIG = github.antimatter.creativeworldclone.CloneConfig.createAndLoad();

    static {
        if (CONFIG.clonedWorlds() == null)
            CONFIG.clonedWorlds(HashBiMap.create());
    }

    @Nullable
    private static Long parseIdString(String clonedWorldId) {
        if (!clonedWorldId.startsWith(PREFIX))
            return null;

        return Long.parseUnsignedLong(clonedWorldId.substring(PREFIX.length()), 16);
    }

    public static String addClone(String baseWorldId) {
        long id = UUID.randomUUID().getLeastSignificantBits();
        CONFIG.clonedWorlds().put(id, baseWorldId);
        CONFIG.save();

        return PREFIX + Long.toHexString(id);
    }

    public static void removeClone(String clonedWorldId) {
        Long id = parseIdString(clonedWorldId);
        if (id == null)
            return;

        CONFIG.clonedWorlds().remove(id);
        CONFIG.save();
    }

    public static boolean hasClone(String baseWorldId) {
        return CONFIG.clonedWorlds().containsValue(baseWorldId);
    }

    public static boolean isClone(String clonedWorldId) {
        Long id = parseIdString(clonedWorldId);
        if (id == null)
            return false;

        return CONFIG.clonedWorlds().containsKey(id);
    }

    @Nullable
    public static String getBaseWorldID(String clonedWorldId) {
        Long id = parseIdString(clonedWorldId);
        if (id == null)
            return null;

        return CONFIG.clonedWorlds().getOrDefault(id, null);
    }
}
