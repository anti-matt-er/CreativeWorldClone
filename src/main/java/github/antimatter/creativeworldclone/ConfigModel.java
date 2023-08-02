package github.antimatter.creativeworldclone;

import com.google.common.collect.BiMap;
import io.wispforest.owo.config.annotation.Config;

@SuppressWarnings("unused")
@Config(name = "creative-world-clone-config", wrapperName = "CloneConfig")
public class ConfigModel {
    public BiMap<Long, String> clonedWorlds;
}
