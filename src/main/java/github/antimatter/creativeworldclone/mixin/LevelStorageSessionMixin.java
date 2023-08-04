package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.ConfigHandler;
import github.antimatter.creativeworldclone.ILevelStorageSessionMixinCloneable;
import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.PathUtil;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.Consumer;

import static github.antimatter.creativeworldclone.CreativeWorldClone.OVERLAY_ICON;

@Mixin(LevelStorage.Session.class)
public abstract class LevelStorageSessionMixin implements ILevelStorageSessionMixinCloneable {
    @Shadow
    @Final
    LevelStorage.LevelSave directory;

    @Shadow
    protected abstract void checkValid();

    @Shadow
    @Final
    private String directoryName;

    @Shadow
    public abstract Optional<Path> getIconFile();

    @Inject(method = "deleteSessionLock", at = @At("HEAD"))
    public void deleteCloneEntry(CallbackInfo ci) {
        if (ConfigHandler.isClone(this.directoryName)) {
            SchematicManager.backupAndDeleteProject(ConfigHandler.getBaseWorldID(this.directoryName));
            ConfigHandler.removeClone(this.directoryName);
        }
    }

    @Override
    public void creativeWorldClone$createCloneIcon() {
        getIconFile().ifPresent(path -> {
            File iconFile = path.toFile();
            
            if (!iconFile.exists())
                return;

            File cloneIconFile = path.resolveSibling("clone_icon.png").toFile();

            try {
                BufferedImage iconImage = ImageIO.read(iconFile);
                BufferedImage overlayImage = ImageIO.read(OVERLAY_ICON.getInputStream());

                int width = iconImage.getWidth();
                int height = iconImage.getHeight();

                BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

                Graphics g = resultImage.getGraphics();
                g.drawImage(iconImage, 0, 0, null);
                g.drawImage(overlayImage, 0, 0, null);

                g.dispose();

                ImageIO.write(resultImage, "png", cloneIconFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

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
    public void creativeWorldClone$modifyNbt(Consumer<NbtCompound> modifier) throws IOException {
        this.checkValid();
        Path path = this.directory.getLevelDatPath();
        if (Files.exists(path)) {
            NbtCompound nbtCompound = NbtIo.readCompressed(path.toFile());
            modifier.accept(nbtCompound);
            NbtIo.writeCompressed(nbtCompound, path.toFile());
        }
    }

    @Override
    public LevelStorage.LevelSave creativeWorldClone$getLevelSave() {
        return this.directory;
    }
}
