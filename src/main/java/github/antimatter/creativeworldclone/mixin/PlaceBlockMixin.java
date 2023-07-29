package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class PlaceBlockMixin {
    @Shadow
    public abstract Block getBlock();

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("HEAD"))
    private void place(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        SchematicManager.onPlace(context.getBlockPos(), String.valueOf(this.getBlock()));
    }
}
