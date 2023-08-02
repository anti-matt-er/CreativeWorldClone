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
    public static final String SUFFIX = " [CREATIVE]";

    private static void clone(LevelStorage.Session storageSession, String newName) throws IOException {
        ((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$cloneLevel(newName);
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

    private static String getWorldName(LevelStorage.Session storageSession) {
        try {
            return ((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$accessNbt(nbtCompound -> {
                NbtCompound dataNbt = nbtCompound.getCompound("Data");
                return dataNbt.getString("LevelName");
            });
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve level name from NBT!", e);
            throw new RuntimeException(e);
        }
    }

    public static GameMode getGameMode(LevelStorage.Session storageSession) {
        return Objects.requireNonNull(storageSession.getLevelSummary()).getGameMode();
    }

    public static void createCreativeClone(MinecraftClient client, LevelStorage.Session storageSession, BooleanConsumer editScreenCallback) {
        if (getGameMode(storageSession) == GameMode.CREATIVE)
            return;

        String baseWorldId = storageSession.getDirectoryName();
        String clonedWorldId = baseWorldId + SUFFIX;
        String baseWorldName = getWorldName(storageSession);
        String clonedWorldName = baseWorldName + SUFFIX;

        BooleanConsumer cloneTask = overwrite -> {
            try {
                storageSession.save(baseWorldName);
                LevelStorage.Session cloneSession = client.getLevelStorage().createSession(clonedWorldId);
                if (overwrite) {
                    cloneSession.deleteSessionLock();
                    cloneSession = client.getLevelStorage().createSession(clonedWorldId);
                }
                clone(storageSession, clonedWorldId);
                storageSession.close();
                cloneSession.save(clonedWorldName);
                makeCreative(cloneSession);
                cloneSession.close();
                ConfigHandler.addClone(clonedWorldId, baseWorldId);
                editScreenCallback.accept(true);
            } catch (IOException | SymlinkValidationException e) {
                LOGGER.error("Failed to clone world!", e);
                editScreenCallback.accept(false);
            }
        };

        if (clonedWorldExists(storageSession, clonedWorldId)) {
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
