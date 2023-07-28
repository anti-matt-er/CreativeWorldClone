package github.antimatter.creativeworldclone;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SchematicManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    @NotNull
    private static final PlayerEntity PLAYER;

    static {
        assert CLIENT.player != null;
        PLAYER = CLIENT.player;
    }

    private static final File SCHEMATICS_DIR = new File(CLIENT.runDirectory, "schematics/CreativeWorldClone/");

    private String name;
    private File schematicFile;
    private LitematicaSchematic schematic;
    private AreaSelection area;

    public static void OnPlace(BlockPos blockPos, String which) {
        LOGGER.info("{} placed at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static void OnBreak(BlockPos blockPos, String which) {
        LOGGER.info("{} break at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

/*    public SchematicManager(String worldName) {
        this.name = this.normalizeName(worldName);

        this.schematicFile = new File(SCHEMATICS_DIR, this.name);

        if (this.schematicFile.exists() && this.schematicFile.isFile() && this.schematicFile.canRead()) {
            this.schematic = LitematicaSchematic.createFromFile(SCHEMATICS_DIR, this.name);
            this.loadSchematic(this.schematic);
            this.area = DataManager.getSimpleArea();
        } else {
            this.area = new AreaSelection();
            this.schematic = LitematicaSchematic.createEmptySchematic(area, String.valueOf(PLAYER.getName()));
            this.loadSchematic(this.schematic);
        }
    }

    private String normalizeName(String name) {
        if (name.endsWith(CreativeWorldClone.SUFFIX)) {
            name = name.substring(0, name.length() - CreativeWorldClone.SUFFIX.length());
        }

        return name;
    }

    private void loadSchematic(LitematicaSchematic schematic) {
        SchematicHolder.getInstance().addSchematic(schematic, true);
        if (DataManager.getCreatePlacementOnLoad()) {
            BlockPos pos = BlockPos.ofFloored(PLAYER.getPos());
            String name = schematic.getMetadata().getName();
            SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
            SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, name, true, true);
            manager.addSchematicPlacement(placement, true);
            manager.setSelectedSchematicPlacement(placement);
        }
    }*/
}
