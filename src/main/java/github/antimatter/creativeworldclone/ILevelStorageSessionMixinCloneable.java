package github.antimatter.creativeworldclone;

import java.io.IOException;

public interface ILevelStorageSessionMixinCloneable {
    void creativeWorldClone$cloneLevel(String targetDir) throws IOException;
    void creativeWorldClone$setGameMode(int gameMode) throws IOException;
}
