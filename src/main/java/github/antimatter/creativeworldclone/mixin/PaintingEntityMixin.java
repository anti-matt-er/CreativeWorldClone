package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends Entity {
    @SuppressWarnings("unused")
    private PaintingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "onBreak", at = @At("HEAD"))
    private void onBreak(Entity entity, CallbackInfo ci) {
        SchematicManager.onBreak(this.getBlockPos(), "Painting");
    }

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void onPlace(CallbackInfo ci) {
        SchematicManager.onPlace(this.getBlockPos(), "Painting");
    }
}
