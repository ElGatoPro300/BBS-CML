package mchorse.bbs_mod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor
{
    @Invoker("setSharedFlag")
    void invokeSetFlag(int mask, boolean value);

    @Mixin(LivingEntity.class)
    public interface LivingEntityAccessor
    {
        @Invoker("setLivingEntityFlag")
        void invokeSetLivingFlag(int mask, boolean value);
    }
}
