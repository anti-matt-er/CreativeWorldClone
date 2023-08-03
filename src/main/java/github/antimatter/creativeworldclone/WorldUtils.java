package github.antimatter.creativeworldclone;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class WorldUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreativeWorldClone.getId());

    private static void clone(LevelStorage.Session storageSession, String newName) throws IOException {
        ((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$cloneLevel(newName);
    }

    private static void createCloneIcon(LevelStorage.Session storageSession) {
        ((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$createCloneIcon();
    }

    private static boolean clonedWorldExists(LevelStorage.Session storageSession, String newName) {
        return Files.exists(((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$getLevelSave().path().resolveSibling(newName));
    }

    private static void makeCreative(LevelStorage.Session storageSession) throws IOException {
        int gameMode = GameMode.CREATIVE.getId();
        ((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$modifyNbt(nbtCompound -> {
            NbtCompound dataNbt = nbtCompound.getCompound("Data");
            NbtCompound playerNbt = dataNbt.getCompound("Player");
            dataNbt.putInt("GameType", gameMode);
            dataNbt.putBoolean("allowCommands", true);
            playerNbt.putInt("playerGameType", gameMode);
        });
    }

    public static GameMode getGameMode(LevelStorage.Session storageSession) {
        return Objects.requireNonNull(storageSession.getLevelSummary()).getGameMode();
    }

    public static void createCreativeClone(MinecraftClient client, LevelStorage.Session storageSession, BooleanConsumer editScreenCallback) {
        if (getGameMode(storageSession) == GameMode.CREATIVE)
            return;

        String clonedWorldDirName = ConfigHandler.addClone(storageSession.getDirectoryName());

        BooleanConsumer cloneTask = overwrite -> {
            try {
                LevelStorage.Session cloneSession = client.getLevelStorage().createSession(clonedWorldDirName);
                if (overwrite) {
                    cloneSession.deleteSessionLock();
                    cloneSession = client.getLevelStorage().createSession(clonedWorldDirName);
                }
                clone(storageSession, clonedWorldDirName);
                storageSession.close();
                makeCreative(cloneSession);
                createCloneIcon(cloneSession);
                cloneSession.close();
                editScreenCallback.accept(true);
            } catch (IOException | SymlinkValidationException e) {
                LOGGER.error("Failed to clone world!", e);
                ConfigHandler.removeClone(clonedWorldDirName);
                editScreenCallback.accept(false);
            }
        };

        if (clonedWorldExists(storageSession, clonedWorldDirName)) {
            Screen currentScreen = client.currentScreen;
            BooleanConsumer confirmCloneTask = proceed -> {
                if (proceed) {
                    cloneTask.accept(true);
                } else {
                    client.setScreen(currentScreen);
                }
            };
            client.setScreen(new ConfirmScreen(confirmCloneTask, Text.of("Overwrite world?"), Text.of("Cloned Creative world already exists! Overwrite?")));
        } else {
            cloneTask.accept(false);
        }
    }
}
