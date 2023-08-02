package github.antimatter.creativeworldclone;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.level.storage.LevelStorage;

import java.io.IOException;
import java.util.function.Consumer;

public interface ILevelStorageSessionMixinCloneable {
    void creativeWorldClone$cloneLevel(String targetDir) throws IOException;

    void creativeWorldClone$modifyNbt(Consumer<NbtCompound> modifier) throws IOException;

    LevelStorage.LevelSave creativeWorldClone$getLevelSave();
}
