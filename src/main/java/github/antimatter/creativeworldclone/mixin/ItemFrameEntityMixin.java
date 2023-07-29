package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin extends Entity {

    private ItemFrameEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onBreak", at = @At("HEAD"))
    private void onBreak(Entity entity, CallbackInfo ci) {
        SchematicManager.onBreak(this.getBlockPos(), "Item Frame");
    }

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void onPlace(CallbackInfo ci) {
        SchematicManager.onPlace(this.getBlockPos(), "Item Frame");
    }
}
