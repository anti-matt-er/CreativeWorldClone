package github.antimatter.creativeworldclone;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class ConfigHandler {
    private static final String PREFIX = "CC_";
    private static final github.antimatter.creativeworldclone.CloneConfig CONFIG = github.antimatter.creativeworldclone.CloneConfig.createAndLoad();

    static {
        if (CONFIG.clonedWorlds() == null)
            CONFIG.clonedWorlds(new HashMap<>());
    }

    private static String generateId() {
        return Long.toHexString(UUID.randomUUID().getLeastSignificantBits());
    }

    @Nullable
    private static String parseIdString(String clonedWorldId) {
        if (!clonedWorldId.startsWith(PREFIX))
            return null;

        return clonedWorldId.substring(PREFIX.length());
    }

    public static String addClone(String baseWorldId) {
        String id = generateId();
        CONFIG.clonedWorlds().put(id, baseWorldId);
        CONFIG.save();

        return PREFIX + id;
    }

    public static void removeClone(String clonedWorldId) {
        String id = parseIdString(clonedWorldId);
        if (id == null)
            return;

        CONFIG.clonedWorlds().remove(id);
        CONFIG.save();
    }

    public static boolean hasClone(String baseWorldId) {
        return CONFIG.clonedWorlds().containsValue(baseWorldId);
    }

    public static boolean isClone(String clonedWorldId) {
        String id = parseIdString(clonedWorldId);
        if (id == null)
            return false;

        return CONFIG.clonedWorlds().containsKey(id);
    }

    @Nullable
    public static String getBaseWorldID(String clonedWorldId) {
        String id = parseIdString(clonedWorldId);
        if (id == null)
            return null;

        return CONFIG.clonedWorlds().getOrDefault(id, null);
    }
}
