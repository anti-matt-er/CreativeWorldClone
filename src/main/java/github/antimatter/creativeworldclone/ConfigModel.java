package github.antimatter.creativeworldclone;

import io.wispforest.owo.config.annotation.Config;

import java.util.HashMap;

@SuppressWarnings("unused")
@Config(name = "creative-world-clone-config", wrapperName = "CloneConfig")
public class ConfigModel {
    public HashMap<String, String> clonedWorlds;
}
