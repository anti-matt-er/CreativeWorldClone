package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.CreativeWorldClone;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Inject(method = "onDisconnected", at = @At("RETURN"))
    private void onGameJoin(Text reason, CallbackInfo ci) {
        CreativeWorldClone.onWorldLeave();
    }
}
