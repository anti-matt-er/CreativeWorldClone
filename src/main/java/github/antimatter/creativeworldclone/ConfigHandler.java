package github.antimatter.creativeworldclone;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ConfigHandler {
    private static final github.antimatter.creativeworldclone.CloneConfig CONFIG = github.antimatter.creativeworldclone.CloneConfig.createAndLoad();

    static {
        if (CONFIG.clonedWorlds() == null)
            CONFIG.clonedWorlds(new HashMap<>());
    }

    public static void addClone(String clonedWorldId, String baseWorldId) {
        CONFIG.clonedWorlds().put(clonedWorldId, baseWorldId);
        CONFIG.save();
    }

    public static void removeClone(String clonedWorldId) {
        CONFIG.clonedWorlds().remove(clonedWorldId);
        CONFIG.save();
    }

    public static boolean hasClone(String baseWorldId) {
        return CONFIG.clonedWorlds().containsValue(baseWorldId);
    }

    public static boolean isClone(String clonedWorldId) {
        return CONFIG.clonedWorlds().containsKey(clonedWorldId);
    }

    @Nullable
    public static String getBaseWorldID(String clonedWorldId) {
        return CONFIG.clonedWorlds().getOrDefault(clonedWorldId, null);
    }
}
