package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.ConfigHandler;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldListWidgetMixin {
    @Shadow
    private @Nullable Path iconPath;

    @Shadow
    protected abstract void loadIcon();

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelSummary;getDetails()Lnet/minecraft/text/Text;"))
    private Text RenderCloneWidget(LevelSummary level) {
        String worldId = level.getName();
        Text defaultText = level.getDetails();
        if (ConfigHandler.isClone(worldId)) {
            MutableText cloneText = Text.empty();
            cloneText.append(Text.literal("Creative Clone").formatted(Formatting.AQUA));
            cloneText.append(defaultText.getSiblings().get(defaultText.getSiblings().size() - 1));
            if (this.iconPath != null) {
                this.iconPath = this.iconPath.resolveSibling("clone_icon.png");
                this.loadIcon();
            }
            return cloneText;
        }

        return defaultText;
    }
}
