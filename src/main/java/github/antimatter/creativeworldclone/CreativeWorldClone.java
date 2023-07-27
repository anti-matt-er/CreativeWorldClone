package github.antimatter.creativeworldclone;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

public class CreativeWorldClone implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");

    @Override
    public void onInitialize() {
        LOGGER.info("CreativeWorldClone initialized!");
    }

    @Unique
    public void cloneWorldToCreative() {

    }
}
