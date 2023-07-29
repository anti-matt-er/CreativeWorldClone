package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class BreakBlockMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ClientWorld world = this.client.world;
        assert world != null;
        BlockState blockState = world.getBlockState(pos);
        SchematicManager.onBreak(pos, String.valueOf(blockState.getBlock()));
    }
}
