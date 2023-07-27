package github.antimatter.creativeworldclone;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreativeWorldClone implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");
    @Override
    public void onInitialize() {
        LOGGER.info("CreativeWorldClone says hello world!");
    }
}
