package github.antimatter.creativeworldclone;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicProjectsManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PathUtil;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class SchematicManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    @NotNull
    private static final PlayerEntity PLAYER;
    private static final LitematicaSchematic.SchematicSaveInfo SAVE_INFO = new LitematicaSchematic.SchematicSaveInfo(false, false);
    private static final IStringConsumer LOG_STRING_CONSUMER = string -> LOGGER.info("Litematica createFromWorld feedback: \"{}\"", string);

    static {
        assert CLIENT.player != null;
        PLAYER = CLIENT.player;
    }
    private static final String AREA_NAME = "Working Area";
    private static SchematicManager instance;
    private static SchematicProjectsManager projectsManager;

    private static final File SCHEMATICS_DIR = new File(CLIENT.runDirectory, "schematics/CreativeWorldClone/");

    private String name;
    private LitematicaSchematic schematic;
    private AreaSelection area;
    private BlockPos minCorner;
    private BlockPos maxCorner;

    public SchematicManager(String worldName) {
        if (instance != null) return;

        this.name = this.normalizeName(worldName);
        String areaID = FileUtils.generateSafeFileName(this.name + "_" + AREA_NAME);

        File schematicFile = new File(SCHEMATICS_DIR, this.name + ".litematic");

        DataManager.load();
        SelectionManager selectionManager = DataManager.getSelectionManager();
        projectsManager = DataManager.getSchematicProjectsManager();

        try {
            PathUtil.createDirectories(SCHEMATICS_DIR.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SchematicProject project = projectsManager.loadProjectFromFile(new File(SCHEMATICS_DIR, this.name + ".json"), true);
        if (project == null) {
            projectsManager.createNewProject(SCHEMATICS_DIR, this.name);
        }

        if (selectionManager.getSelectionMode() == SelectionMode.SIMPLE) selectionManager.switchSelectionMode();
        String selectionID = new File(SCHEMATICS_DIR, areaID + ".json").getAbsolutePath();
        boolean loaded = (schematicFile.exists() && schematicFile.isFile() && schematicFile.canRead());
        if (loaded) {
            this.schematic = LitematicaSchematic.createFromFile(SCHEMATICS_DIR, this.name);
            SchematicHolder.getInstance().addSchematic(this.schematic, false);
        } else {
            selectionManager.createNewSelection(SCHEMATICS_DIR, areaID);
        }
        this.area = selectionManager.getOrLoadSelection(selectionID);
        Objects.requireNonNull(this.area).setName(AREA_NAME);
        selectionManager.setCurrentSelection(selectionID);
        if (loaded) {
            Box areaBox = this.area.getSelectedSubRegionBox();
            assert areaBox != null;
            this.minCorner = PositionUtils.getMinCorner(
                    areaBox.getPosition(PositionUtils.Corner.CORNER_1),
                    areaBox.getPosition(PositionUtils.Corner.CORNER_2)
            );
            this.maxCorner = PositionUtils.getMaxCorner(
                    areaBox.getPosition(PositionUtils.Corner.CORNER_1),
                    areaBox.getPosition(PositionUtils.Corner.CORNER_2)
            );
            LOGGER.info("Loaded area \"{}\"", selectionID);
        } else {
            LOGGER.info("Created new area \"{}\"", selectionID);
        }
        this.save(false);

        instance = this;
    }

    public void save(boolean forceSave) {
        this.schematic = LitematicaSchematic.createFromWorld(MinecraftClient.getInstance().world, this.area, SAVE_INFO, PLAYER.getDisplayName().getString(), LOG_STRING_CONSUMER);
        SchematicHolder.getInstance().clearLoadedSchematics();
        SchematicHolder.getInstance().addSchematic(this.schematic, false);
        DataManager.save(forceSave);
        projectsManager.saveCurrentProject();
    }

    private void persist() {
        this.save(true);
        this.schematic.writeToFile(SCHEMATICS_DIR, this.name, true);
    }

    public static void close() {
        instance.persist();
        instance = null;
    }

    private String normalizeName(String name) {
        //TODO: Move to a new utility class
        if (name.endsWith(CreativeWorldClone.SUFFIX)) {
            name = name.substring(0, name.length() - CreativeWorldClone.SUFFIX.length());
        }

        return name;
    }

    private void onBlockChange(BlockPos blockPos) {
        if (this.minCorner == null) this.minCorner = blockPos;
        if (this.maxCorner == null) this.maxCorner = blockPos;
        BlockPos newMin = PositionUtils.getMinCorner(this.minCorner, blockPos);
        BlockPos newMax = PositionUtils.getMaxCorner(this.maxCorner, blockPos);
        this.area.setSelectedSubRegionCornerPos(newMin, PositionUtils.Corner.CORNER_1);
        this.area.setSelectedSubRegionCornerPos(newMax, PositionUtils.Corner.CORNER_2);
        this.minCorner = newMin;
        this.maxCorner = newMax;
        this.area.setExplicitOrigin(newMin);
        this.area.setOriginSelected(true);

        this.save(false);

        //TODO: Instead of manually setting corners, use Litematica's SchematicVerifier class with using the survival
        // world as a base schematic to assess block changes. This would ensure the working area doesn't include
        // unnecessary changes, such as placing and removing a block where it would be outside the area.
        // Use the corners instead to limit the range of the verifier. Another advantage of this is to be able to easily
        // update the creative clone from the survival world while optionally keeping old creative changes.
    }

    public static void onPlace(BlockPos blockPos, String which) {
        if (instance == null) return;
        LOGGER.info("{} placed at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        instance.onBlockChange(blockPos);
    }

    public static void onBreak(BlockPos blockPos, String which) {
        if (instance == null) return;
        LOGGER.info("{} break at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        instance.onBlockChange(blockPos);
    }
}
