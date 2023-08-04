package github.antimatter.creativeworldclone.mixin;

import fi.dy.masa.litematica.event.WorldLoadListener;
import github.antimatter.creativeworldclone.CreativeWorldClone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldLoadListener.class)
public abstract class LitematicWorldLoadListenerMixin {
    @Inject(method = "onWorldLoadPost", at = @At("RETURN"))
    void loadSchematicManager(ClientWorld worldBefore, ClientWorld worldAfter, MinecraftClient mc, CallbackInfo ci) {
        MinecraftServer server = mc.getServer();

        if (server == null) {
            CreativeWorldClone.onLitematicaWorldLeave();
        } else {
            CreativeWorldClone.onLitematicaWorldLoad(server);
        }
    }
}
