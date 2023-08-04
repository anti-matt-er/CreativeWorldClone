package github.antimatter.creativeworldclone;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.projects.SchematicProjectsManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.PathUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
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
    private static final File CLONE_SCHEMATICS_DIR = new File(MinecraftClient.getInstance().runDirectory, "schematics/CreativeWorldClone/");
    private static final LitematicaSchematic.SchematicSaveInfo SAVE_INFO = new LitematicaSchematic.SchematicSaveInfo(false, false);
    private static final IStringConsumer LOG_STRING_CONSUMER = string -> LOGGER.info("Litematica createFromWorld feedback: \"{}\"", string);
    @Nullable
    private static SchematicManager instance;
    private final MinecraftClient client;
    private final SchematicProjectsManager projectsManager;
    private final SchematicHolder schematicHolder;
    private final SelectionManager selectionManager;
    @Nullable
    private File projectDir;
    @Nullable
    private GameMode mode;
    @Nullable
    private String name;
    @Nullable
    private LitematicaSchematic schematic;
    @Nullable
    private AreaSelection area;
    @Nullable
    private BlockPos minCorner;
    @Nullable
    private BlockPos maxCorner;
    @Nullable
    private SchematicPlacement placement;
    private boolean hasEdits = false;
    private boolean wasRenderEnabled = true;

    private SchematicManager() {
        this.client = MinecraftClient.getInstance();
        this.projectsManager = DataManager.getSchematicProjectsManager();
        this.schematicHolder = SchematicHolder.getInstance();
        this.selectionManager = DataManager.getSelectionManager();
    }

    public static SchematicManager getInstance() {
        if (instance == null)
            instance = new SchematicManager();

        return instance;
    }

    @NotNull
    private static SchematicManager getInstanceWithWorld(String name, GameMode mode) {
        instance = getInstance();
        instance.mode = mode;
        instance.name = name;
        instance.projectDir = getProjectDir(name);
        instance.createProjectDir();
        instance.wasRenderEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue();

        return instance;
    }

    private static void reset() {
        DataManager.clear();
        DataManager.save();
    }

    private static boolean isUnloaded() {
        return instance == null;
    }

    private static void loadCreativeWorld(String worldId) {
        String name = ConfigHandler.getBaseWorldID(worldId);
        if (name == null)
            return;

        instance = getInstanceWithWorld(name, GameMode.CREATIVE);
        Configs.Visuals.ENABLE_RENDERING.setBooleanValue(false);

        DataManager.load();

        if (!instance.tryLoadProject()) {
            instance.createProject();
        }

        if (!instance.tryLoadArea()) {
            instance.createArea();
        }

        LOGGER.info("SchematicManager loaded for world \"{}\" in Creative mode", instance.name);
    }

    private static void loadSurvivalWorld(String worldId) {
        if (!ConfigHandler.hasClone(worldId))
            return;

        instance = getInstanceWithWorld(worldId, GameMode.SURVIVAL);
        Configs.Visuals.ENABLE_RENDERING.setBooleanValue(true);

        if (!instance.trySoftLoadArea() || !instance.tryLoadSchematic()) {
            close();

            LOGGER.info("SchematicManager couldn't find project for \"{}\" in Survival mode", worldId);
            return;
        }

        DataManager.load();

        instance.placeSchematic();

        LOGGER.info("SchematicManager loaded for world \"{}\" in Survival mode", worldId);
    }

    public static void loadWorld(MinecraftServer server) {
        reset();

        String worldId = server.session.getDirectoryName();
        GameMode mode = server.getDefaultGameMode();

        if (mode == GameMode.CREATIVE) {
            loadCreativeWorld(worldId);
        } else if (mode == GameMode.SURVIVAL) {
            loadSurvivalWorld(worldId);
        }

        if (isUnloaded())
            reset();
    }

    private static File getProjectDir(String worldId) {
        return new File(CLONE_SCHEMATICS_DIR, worldId);
    }

    @Nullable
    private static AreaSelection getAreaFromFile(File areaFile) {
        return SelectionManager.tryLoadSelectionFromFile(areaFile);
    }

    @Nullable
    private static LitematicaSchematic getSchematicFromFile(File schematicFile) {
        return LitematicaSchematic.createFromFile(schematicFile.getParentFile(), FilenameUtils.getBaseName(schematicFile.getName()));
    }

    private void createProjectDir() {
        if (this.projectDir == null) {
            LOGGER.error("Project directory not set!");
            return;
        }

        try {
            PathUtil.createDirectories(this.projectDir.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File resolveFile(String fileName) {
        return new File(this.projectDir, fileName);
    }

    @Nullable
    private File getProject() {
        File projectFile = this.resolveFile("project.json");
        if (!projectFile.exists()) {
            LOGGER.info("Area file \"{}\" does not exist", projectFile.getAbsoluteFile());
            return null;
        }

        return projectFile;
    }

    private void loadProject(File projectFile) {
        projectsManager.openProject(projectFile);

        LOGGER.info("Loaded project \"{}\"", projectFile.getAbsoluteFile());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryLoadProject() {
        File project = this.getProject();
        if (project == null) {
            return false;
        } else {
            this.loadProject(project);
        }

        return true;
    }

    private void createProject() {
        projectsManager.createNewProject(this.projectDir, "project");

        LOGGER.info("Created new project");
    }

    @Nullable
    private AreaSelection getArea() {
        File areaFile = this.resolveFile("area.json");
        String selectionID = areaFile.getAbsolutePath();

        if (!areaFile.exists()) {
            LOGGER.info("Area file \"{}\" does not exist", selectionID);
            return null;
        }

        AreaSelection area = getAreaFromFile(areaFile);

        if (area == null || area.getExplicitOrigin() == null) {
            LOGGER.info("Area file \"{}\" invalid or has no edits, unloading", selectionID);

            return null;
        }

        return area;
    }

    private void loadArea(AreaSelection area, String selectionID) {
        this.setSelectionMode(SelectionMode.NORMAL);

        this.area = area;
        this.selectionManager.setCurrentSelection(selectionID);
        Box areaBox = area.getSelectedSubRegionBox();
        assert areaBox != null;
        this.minCorner = PositionUtils.getMinCorner(areaBox.getPosition(PositionUtils.Corner.CORNER_1), areaBox.getPosition(PositionUtils.Corner.CORNER_2));
        this.maxCorner = PositionUtils.getMaxCorner(areaBox.getPosition(PositionUtils.Corner.CORNER_1), areaBox.getPosition(PositionUtils.Corner.CORNER_2));
        this.hasEdits = true;

        LOGGER.info("Loaded area \"{}\"", selectionID);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryLoadArea() {
        AreaSelection area = this.getArea();
        if (area == null) {
            return false;
        } else {
            this.loadArea(area, this.resolveFile("area.json").getAbsolutePath());
        }

        return true;
    }

    private boolean trySoftLoadArea() {
        this.area = this.getArea();

        return this.area != null;
    }

    private void createArea() {
        this.setSelectionMode(SelectionMode.NORMAL);

        selectionManager.createNewSelection(this.projectDir, "area");
        this.area = selectionManager.getCurrentSelection();

        assert this.area != null;
        this.area.setName("area");
        assert Objects.equals(this.area.getName(), "area");

        LOGGER.info("Created new area");
    }

    @Nullable
    private LitematicaSchematic getSchematic() {
        File schematicFile = this.resolveFile("schematic.litematic");
        if (!schematicFile.exists()) {
            LOGGER.info("Schematic file \"{}\" does not exist", schematicFile.getAbsoluteFile());
            return null;
        }

        return getSchematicFromFile(schematicFile);
    }

    private void loadSchematic(LitematicaSchematic schematic) {
        this.schematic = schematic;
        this.schematicHolder.addSchematic(schematic, false);

        LOGGER.info("Loaded schematic \"{}\"", Objects.requireNonNull(schematic.getFile()).getAbsolutePath());
    }

    private boolean tryLoadSchematic() {
        LitematicaSchematic schematic = this.getSchematic();
        if (schematic == null) {
            return false;
        } else {
            this.loadSchematic(schematic);
        }

        return true;
    }

    private void createSchematic() {
        if (this.area == null || this.client.player == null) {
            LOGGER.info("Cannot create schematic! area: {}, player: {}", this.area, this.client.player);
            return;
        }

        schematicHolder.removeSchematic(this.schematic);
        this.schematic = LitematicaSchematic.createFromWorld(MinecraftClient.getInstance().world, this.area, SAVE_INFO, this.client.player.getDisplayName().getString(), LOG_STRING_CONSUMER);
        this.schematicHolder.addSchematic(schematic, false);
    }

    // Warning suppressed as we may want to be able to change to other modes down the line
    private void setSelectionMode(@SuppressWarnings("SameParameterValue") SelectionMode mode) {
        if (selectionManager.getSelectionMode() != mode)
            selectionManager.switchSelectionMode();
    }

    public static void backupAndDeleteProject(String worldId) {
        File dir = getProjectDir(worldId);
        File projectFile = new File(dir, "project.json");

        if (!dir.exists() || !projectFile.exists()) {
            LOGGER.info("No schematic project managed for {}", worldId);
            return;
        }

        File areaFile = new File(dir, "area.json");
        File schematicFile = new File(dir, "schematic.litematic");
        File schematicBackupFile = new File(dir, Instant.now().getEpochSecond() + ".litematic");

        if (areaFile.exists() && !areaFile.delete())
            LOGGER.error("Failed to delete {}/area.json!", worldId);
        if (projectFile.exists() && !projectFile.delete())
            LOGGER.error("Failed to delete {}/project.json!", worldId);
        if (schematicFile.exists()) {
            try {
                Files.move(schematicFile.toPath(), schematicBackupFile.toPath());
            } catch (IOException e) {
                LOGGER.error("Failed to backup {} schematic!", worldId, e);
            }
        }
    }

    private void placeSchematic() {
        if (this.schematic == null || this.area == null) {
            LOGGER.error("Schematic or area not set!");
            return;
        }

        schematicHolder.addSchematic(this.schematic, false);
        this.placement = SchematicPlacement.createFor(this.schematic, this.area.getEffectiveOrigin(), this.schematic.getMetadata().getName(), true, true);
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        manager.addSchematicPlacement(this.placement, true);
        manager.setSelectedSchematicPlacement(this.placement);

        DataManager.save();
    }

    public void save() {
        this.createSchematic();
        projectsManager.saveCurrentProject();
        DataManager.save(true);
    }

    private void persist() {
        File areaFile = this.resolveFile("area.json");
        if (this.hasEdits) {
            try {
                if (this.area != null)
                    Files.write(areaFile.toPath(), this.area.toJson().toString().getBytes());
            } catch (IOException e) {
                LOGGER.error("Cannot save area file!", e);
            }
        }
        if (this.schematic != null)
            this.schematic.writeToFile(this.projectDir, "schematic", true);
    }

    public static void close() {
        if (instance == null)
            return;

        Configs.Visuals.ENABLE_RENDERING.setBooleanValue(instance.wasRenderEnabled);
        instance.persist();
        instance = null;
        reset();

        LOGGER.info("SchematicManager unloaded");
    }

    private void onBlockChange(BlockPos blockPos) {
        if (this.mode == GameMode.SURVIVAL)
            return;

        if (this.area == null) {
            LOGGER.error("Area not set!");
            return;
        }

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
        this.hasEdits = true;

        this.save();

        //TODO: Instead of manually setting corners, use Litematica's SchematicVerifier class with using the survival
        // world as a base schematic to assess block changes. This would ensure the working area doesn't include
        // unnecessary changes, such as placing and removing a block where it would be outside the area.
        // Use the corners instead to limit the range of the verifier. Another advantage of this is to be able to easily
        // update the creative clone from the survival world while optionally keeping old creative changes.
    }

    public static void onPlace(BlockPos blockPos, String which) {
        if (instance == null)
            return;

        LOGGER.info("{} placed at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        instance.onBlockChange(blockPos);
    }

    public static void onBreak(BlockPos blockPos, String which) {
        if (instance == null)
            return;

        LOGGER.info("{} break at {}, {}, {}", which, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        instance.onBlockChange(blockPos);
    }
}
