package github.antimatter.creativeworldclone.mixin;

import github.antimatter.creativeworldclone.SchematicManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntity.class)
public abstract class ArmorStandEntityMixin extends Entity {

    public ArmorStandEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "kill", at = @At("HEAD"))
    private void onBreak(CallbackInfo ci) {
        SchematicManager.onBreak(this.getBlockPos(), "Armor Stand");
    }
}

