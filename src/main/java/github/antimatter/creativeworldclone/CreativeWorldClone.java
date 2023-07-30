package github.antimatter.creativeworldclone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

public class CreativeWorldClone implements ModInitializer {
    //TODO: Run a code formatter over everything!
    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");
    public static final String SUFFIX = " [CREATIVE]";

    @Override
    public void onInitialize() {
        LOGGER.info("CreativeWorldClone initialized!");
    }

    @Unique
    public static Boolean isLitematicaLoaded() {
        return FabricLoader.getInstance().isModLoaded("litematica");
    }

    public static void onWorldLoad() {
        if (isLitematicaLoaded()) {
            SchematicManager.loadWorld(Objects.requireNonNull(MinecraftClient.getInstance().getServer()));
        }
    }

    public static void onWorldLeave() {
        if (isLitematicaLoaded()) {
            SchematicManager.close();
        }
    }
}
