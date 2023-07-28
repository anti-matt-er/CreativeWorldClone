package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.ILevelStorageSessionMixinCloneable;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.SymlinkWarningScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

import static github.antimatter.creativeworldclone.CreativeWorldClone.SUFFIX;

@Mixin(EditWorldScreen.class)
public abstract class EditWorldScreenMixin extends Screen {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("creative-world-clone");

    @Shadow @Final private LevelStorage.Session storageSession;
    @Shadow @Final private BooleanConsumer callback;

    protected EditWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void addCloneButton(CallbackInfo ci) {
        this.addDrawableChild(
                        ButtonWidget.builder(Text.of("Clone World in Creative!"),
                        button -> this.cloneWorld(this.storageSession)).dimensions(
                                this.width / 2 - 100,
                                this.height / 4 + 120 + 5,
                                200,
                                20
                        ).build()
        );

    }

    @Unique
    private void cloneWorld(LevelStorage.Session storageSession) {
        String creativeName = storageSession.getDirectoryName() + SUFFIX;

        assert client != null;

        try {
            ((ILevelStorageSessionMixinCloneable) storageSession).creativeWorldClone$cloneLevel(creativeName);
            storageSession.close();

            LevelStorage.Session creativeStorageSession = this.client.getLevelStorage().createSession(creativeName);
            creativeStorageSession.save(creativeName);
            ((ILevelStorageSessionMixinCloneable) creativeStorageSession).creativeWorldClone$setGameMode(1);
            creativeStorageSession.close();

            this.callback.accept(true);
        } catch (IOException e) {
            LOGGER.error("Failed to access world '{}'", storageSession.getDirectoryName(), e);
            SystemToast.addWorldAccessFailureToast(this.client, storageSession.getDirectoryName());
            this.callback.accept(true);
        } catch (SymlinkValidationException symlinkValidationException) {
            LOGGER.warn("{}", symlinkValidationException.getMessage());
            this.client.setScreen(new SymlinkWarningScreen(this));
        }
    }
}
