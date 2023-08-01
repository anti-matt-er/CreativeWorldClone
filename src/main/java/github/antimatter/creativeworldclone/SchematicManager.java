package github.antimatter.creativeworldclone;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.projects.SchematicProjectsManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.PathUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Objects;

public class SchematicManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreativeWorldClone.getId());
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final PlayerEntity PLAYER = CLIENT.player;
    private static final File CLONE_SCHEMATICS_DIR = new File(CLIENT.runDirectory, "schematics/CreativeWorldClone/");
    private static final LitematicaSchematic.SchematicSaveInfo SAVE_INFO = new LitematicaSchematic.SchematicSaveInfo(false, false);
    private static final IStringConsumer LOG_STRING_CONSUMER = string -> LOGGER.info("Litematica createFromWorld feedback: \"{}\"", string);
    private static final SchematicProjectsManager projectsManager = DataManager.getSchematicProjectsManager();
    private static final SchematicHolder schematicHolder = SchematicHolder.getInstance();
    private static final SelectionManager selectionManager = DataManager.getSelectionManager();
    @Nullable
    private static SchematicManager instance;
    private File projectDir;
    private Boolean worldLoaded = false;
    private GameMode mode;
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

    private static boolean isUnloaded() {
        return instance == null || !instance.worldLoaded;
    }

    private static void loadCreativeWorld(String worldId) {
        if (!ConfigHandler.isClone(worldId))
            return;

        instance = getInstance();
        instance.mode = GameMode.CREATIVE;
        instance.name = ConfigHandler.getBaseWorldID(worldId);
        assert instance.name != null;
        instance.projectDir = getProjectDir(instance.name);
        instance.createOrLoadProject();
        instance.worldLoaded = true;

        LOGGER.info("SchematicManager loaded for world \"{}\" in Creative mode", instance.name);
    }

    private static void loadSurvivalWorld(String worldId) {
        if (!ConfigHandler.hasClone(worldId))
            return;

        instance = getInstance();
        instance.mode = GameMode.SURVIVAL;
        instance.name = worldId;
        instance.projectDir = getProjectDir(instance.name);

        if (!instance.tryLoadProject()) {
            instance = null;
            LOGGER.info("SchematicManager couldn't find project for \"{}\" in Survival mode", worldId);

            return;
        }

        instance.placeSchematic();
        instance.worldLoaded = true;

        LOGGER.info("SchematicManager loaded for world \"{}\" in Survival mode", worldId);
    }

    public static void loadWorld(MinecraftServer server) {
        String worldId = server.session.getDirectoryName();
        GameMode mode = server.getDefaultGameMode();

        if (mode == GameMode.CREATIVE) {
            loadCreativeWorld(worldId);
        } else if (mode == GameMode.SURVIVAL) {
            loadSurvivalWorld(worldId);
        }

        if (isUnloaded())
            DataManager.clear();
    }

    private static File getProjectDir(String worldId) {
        return new File(CLONE_SCHEMATICS_DIR, worldId);
    }

    private void createProjectDir() {
        try {
            PathUtil.createDirectories(this.projectDir.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Warning suppressed as we may want to be able to change to other modes down the line
    private void setSelectionMode(@SuppressWarnings("SameParameterValue") SelectionMode mode) {
        if (selectionManager.getSelectionMode() != mode)
            selectionManager.switchSelectionMode();
    }

    private boolean tryLoadProjectFile(boolean createOnFailure) {
        if (createOnFailure)
            this.createProjectDir();

        if (projectsManager.loadProjectFromFile(new File(this.projectDir, "project.json"), true) == null) {
            if (createOnFailure)
                projectsManager.createNewProject(this.projectDir, "project");

            return false;
        }

        return true;
    }

    private void loadSchematic() {
        this.schematic = LitematicaSchematic.createFromFile(this.projectDir, "schematic");
        schematicHolder.addSchematic(this.schematic, false);
    }

    private void createOrLoadWorkingArea(boolean shouldLoad) {
        this.setSelectionMode(SelectionMode.NORMAL);

        String selectionID = new File(this.projectDir, "area.json").getAbsolutePath();

        if (shouldLoad) {
            loadSchematic();
        } else {
            selectionManager.createNewSelection(this.projectDir, "area");
        }

        this.area = selectionManager.getOrLoadSelection(selectionID);
        Objects.requireNonNull(this.area).setName("area");
        selectionManager.setCurrentSelection(selectionID);

        if (shouldLoad) {
            Box areaBox = this.area.getSelectedSubRegionBox();
            assert areaBox != null;
            this.minCorner = PositionUtils.getMinCorner(areaBox.getPosition(PositionUtils.Corner.CORNER_1), areaBox.getPosition(PositionUtils.Corner.CORNER_2));
            this.maxCorner = PositionUtils.getMaxCorner(areaBox.getPosition(PositionUtils.Corner.CORNER_1), areaBox.getPosition(PositionUtils.Corner.CORNER_2));
            LOGGER.info("Loaded area \"{}\"", selectionID);
        } else {
            this.area = selectionManager.getCurrentSelection();
            LOGGER.info("Created new area \"{}\"", selectionID);
        }
    }

    private void createOrLoadProject() {
        boolean loaded = tryLoadProjectFile(true);
        DataManager.load();
        this.createOrLoadWorkingArea(loaded);
        this.save(false);
    }

    private boolean tryLoadProject() {
        if (!tryLoadProjectFile(false))
            return false;

        DataManager.load();
        this.createOrLoadWorkingArea(true);

        return true;
    }

    public static void backupAndDeleteProject(String worldId) {
        File dir = getProjectDir(worldId);

        if (!dir.exists()) {
            LOGGER.info("No schematic project managed for {}", worldId);
            return;
        }

        if (!new File(dir, "area.json").delete())
            LOGGER.error("Failed to delete {}/area.json!", worldId);
        if (!new File(dir, "project.json").delete())
            LOGGER.error("Failed to delete {}/project.json!", worldId);

        try {
            Files.move(new File(dir, "schematic.litematic").toPath(), new File(dir, Instant.now().getEpochSecond() + ".litematic").toPath());
        } catch (IOException e) {
            LOGGER.error("Failed to backup {} schematic!", worldId, e);
        }
    }

    private void placeSchematic() {
        schematicHolder.addSchematic(this.schematic, false);
        DataManager.save();
        SchematicPlacement placement = SchematicPlacement.createFor(this.schematic, this.area.getEffectiveOrigin(), this.name, true, true);
        DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);
    }

    public void save(boolean forceSave) {
        schematicHolder.removeSchematic(this.schematic);
        this.schematic = LitematicaSchematic.createFromWorld(MinecraftClient.getInstance().world, this.area, SAVE_INFO, PLAYER.getDisplayName().getString(), LOG_STRING_CONSUMER);
        schematicHolder.addSchematic(this.schematic, false);
        DataManager.save(forceSave);
        projectsManager.saveCurrentProject();
    }

    private void persist() {
        this.save(true);
        this.schematic.writeToFile(this.projectDir, "schematic", true);
    }

    public static void close() {
        if (isUnloaded())
            return;

        instance.persist();
        instance.worldLoaded = false;
        schematicHolder.removeSchematic(instance.schematic);
        DataManager.clear();
        instance = null;
        LOGGER.info("SchematicManager unloaded");
    }

    private void onBlockChange(BlockPos blockPos) {
        LOGGER.info("{}", this.area.getSelectedSubRegionBox());
        
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

    public static void onPlace(BlockPos blockPos, String which) {
        if (isUnloaded() || instance.mode == GameMode.SURVIVAL)
            return;

        LOGGER.info("{} placed at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        instance.onBlockChange(blockPos);
    }

    public static void onBreak(BlockPos blockPos, String which) {
        if (isUnloaded() || instance.mode == GameMode.SURVIVAL)
            return;

        LOGGER.info("{} break at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        instance.onBlockChange(blockPos);
    }
}
