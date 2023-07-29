package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.CreativeWorldClone;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        CreativeWorldClone.onWorldLoad();
    }
}
