package github.antimatter.creativeworldclone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

public class CreativeWorldClone implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");
    public static final String SUFFIX = " [CREATIVE]";

    @Override
    public void onInitialize() {
        LOGGER.info("CreativeWorldClone initialized!");
    }

    @Unique
    public void cloneWorldToCreative() {

    }

    @Unique
    public Boolean isLitematicaLoaded() {
        return FabricLoader.getInstance().isModLoaded("litematica");
    }
}
