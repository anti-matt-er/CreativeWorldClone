package github.antimatter.creativeworldclone;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.projects.SchematicProjectsManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
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
    private static final String AREA_NAME = "Working Area";
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    @NotNull
    private static final PlayerEntity PLAYER = Objects.requireNonNull(CLIENT.player);
    private static final File PROJECT_DIR = new File(CLIENT.runDirectory, "schematics/CreativeWorldClone/");
    private static final LitematicaSchematic.SchematicSaveInfo SAVE_INFO = new LitematicaSchematic.SchematicSaveInfo(false, false);
    private static final IStringConsumer LOG_STRING_CONSUMER = string -> LOGGER.info("Litematica createFromWorld feedback: \"{}\"", string);
    private static final SchematicProjectsManager projectsManager = DataManager.getSchematicProjectsManager();
    private static final SchematicHolder schematicHolder = SchematicHolder.getInstance();
    private static final SelectionManager selectionManager = DataManager.getSelectionManager();
    private static SchematicManager instance;
    private Boolean worldLoaded;
    private String name;
    private LitematicaSchematic schematic;
    private AreaSelection area;
    private BlockPos minCorner;
    private BlockPos maxCorner;

    private SchematicManager() {
    }

    public static SchematicManager getInstance() {
        if (instance == null)
            instance = new SchematicManager();

        return instance;
    }

    public static void loadWorld(String worldName) {
        getInstance();
        instance.name = StringUtils.removeSuffix(worldName, CreativeWorldClone.SUFFIX);
        instance.createOrLoadProject();
        instance.worldLoaded = true;
    }

    private void createProjectDir() {
        try {
            PathUtil.createDirectories(PROJECT_DIR.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Warning suppressed as we may want to be able to change to other modes down the line
    private void setSelectionMode(@SuppressWarnings("SameParameterValue") SelectionMode mode) {
        if (selectionManager.getSelectionMode() != mode)
            selectionManager.switchSelectionMode();
    }

    private boolean tryLoadProject() {
        this.createProjectDir();
        if (projectsManager.loadProjectFromFile(new File(PROJECT_DIR, this.name + ".json"), true) == null) {
            projectsManager.createNewProject(PROJECT_DIR, this.name);

            return false;
        }

        return true;
    }

    private void createOrLoadWorkingArea(boolean shouldLoad) {
        this.setSelectionMode(SelectionMode.NORMAL);

        String areaID = StringUtils.normalize(this.name + "_" + AREA_NAME);
        String selectionID = new File(PROJECT_DIR, areaID + ".json").getAbsolutePath();

        if (shouldLoad) {
            this.schematic = LitematicaSchematic.createFromFile(PROJECT_DIR, this.name);
            schematicHolder.addSchematic(this.schematic, false);
        } else {
            selectionManager.createNewSelection(PROJECT_DIR, areaID);
        }

        this.area = selectionManager.getOrLoadSelection(selectionID);
        Objects.requireNonNull(this.area).setName(AREA_NAME);
        selectionManager.setCurrentSelection(selectionID);

        if (shouldLoad) {
            Box areaBox = this.area.getSelectedSubRegionBox();
            assert areaBox != null;
            this.minCorner = PositionUtils.getMinCorner(areaBox.getPosition(PositionUtils.Corner.CORNER_1), areaBox.getPosition(PositionUtils.Corner.CORNER_2));
            this.maxCorner = PositionUtils.getMaxCorner(areaBox.getPosition(PositionUtils.Corner.CORNER_1), areaBox.getPosition(PositionUtils.Corner.CORNER_2));
            LOGGER.info("Loaded area \"{}\"", selectionID);
        } else {
            LOGGER.info("Created new area \"{}\"", selectionID);
        }
    }

    private void createOrLoadProject() {
        boolean loaded = tryLoadProject();
        DataManager.load();
        this.createOrLoadWorkingArea(loaded);
        this.save(false);
    }

    public void save(boolean forceSave) {
        this.schematic = LitematicaSchematic.createFromWorld(MinecraftClient.getInstance().world, this.area, SAVE_INFO, PLAYER.getDisplayName().getString(), LOG_STRING_CONSUMER);
        schematicHolder.clearLoadedSchematics();
        schematicHolder.addSchematic(this.schematic, false);
        DataManager.save(forceSave);
        projectsManager.saveCurrentProject();
    }

    private void persist() {
        this.save(true);
        this.schematic.writeToFile(PROJECT_DIR, this.name, true);
    }

    public static void close() {
        instance.persist();
        instance.worldLoaded = false;
        instance = null;
    }

    private void onBlockChange(BlockPos blockPos) {
        if (!this.worldLoaded)
            return;

        if (this.minCorner == null)
            this.minCorner = blockPos;
        if (this.maxCorner == null)
            this.maxCorner = blockPos;
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

    public void onPlace(BlockPos blockPos, String which) {
        LOGGER.info("{} placed at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        this.onBlockChange(blockPos);
    }

    public void onBreak(BlockPos blockPos, String which) {
        LOGGER.info("{} break at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        this.onBlockChange(blockPos);
    }
}
