package github.antimatter.creativeworldclone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

public class CreativeWorldClone implements ModInitializer {
    private static final String ID = "creative-world-clone";
    private static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final Identifier OVERLAY_ID = new Identifier(ID, "clone_icon_overlay.png");
    public static Resource OVERLAY_ICON;

    @Override
    public void onInitialize() {
        LOGGER.info("CreativeWorldClone initialized!");
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return OVERLAY_ID;
            }

            @Override
            public void reload(ResourceManager manager) {
                OVERLAY_ICON = manager.getResource(getFabricId()).orElseThrow();
            }
        });
    }

    @Unique
    public static String getId() {
        return ID;
    }

    public static void onLitematicaWorldLoad(MinecraftServer server) {
        SchematicManager.loadWorld(server);
    }

    public static void onLitematicaWorldLeave() {
        SchematicManager.close();
    }
}
