package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.WorldUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditWorldScreen.class)
public abstract class EditWorldScreenMixin extends Screen {
    @Shadow
    @Final
    private LevelStorage.Session storageSession;
    @Shadow
    @Final
    private BooleanConsumer callback;

    protected EditWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void addCloneButton(CallbackInfo ci) {
        if (WorldUtils.getGameMode(this.storageSession) == GameMode.SURVIVAL) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Clone World in Creative!"), button -> this.cloneWorld()).dimensions(this.width / 2 - 100, this.height / 4 + 120 + 5, 200, 20).build());
        }
    }

    @Unique
    private void cloneWorld() {
        WorldUtils.createCreativeClone(this.client, this.storageSession, this.callback);
    }
}
