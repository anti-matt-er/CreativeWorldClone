package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class EntityMixin {
    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void onPlace(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.getType() == EntityType.ARMOR_STAND) {
            SchematicManager.OnPlace(entity.getBlockPos(), "Armor Stand");
        }
    }
}
