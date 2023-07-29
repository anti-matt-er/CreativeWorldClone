package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.ILevelStorageSessionMixinCloneable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.PathUtil;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Mixin(LevelStorage.Session.class)
public abstract class LevelStorageSessionMixin implements ILevelStorageSessionMixinCloneable {
    @Shadow
    @Final
    LevelStorage.LevelSave directory;

    @Shadow
    protected abstract void checkValid();

    @Override
    public void creativeWorldClone$cloneLevel(String targetDir) throws IOException {
        this.checkValid();

        Path source = this.directory.path();
        Path target = source.resolveSibling(targetDir);

        try {
            PathUtil.createDirectories(target);
        } catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.endsWith("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }

                Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void creativeWorldClone$setGameMode(int gameMode) throws IOException {
        this.checkValid();
        Path path = this.directory.getLevelDatPath();
        if (Files.exists(path)) {
            NbtCompound nbtCompound = NbtIo.readCompressed(path.toFile());
            NbtCompound dataNbt = nbtCompound.getCompound("Data");
            NbtCompound playerNbt = dataNbt.getCompound("Player");
            dataNbt.putInt("GameType", gameMode);
            playerNbt.putInt("playerGameType", gameMode);
            NbtIo.writeCompressed(nbtCompound, path.toFile());
        }
    }
}
