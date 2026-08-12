package mchorse.bbs_mod.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor
{
    @Accessor("dimensions")
    void bbs$setDimensions(EntityDimensions dimensions);
}
