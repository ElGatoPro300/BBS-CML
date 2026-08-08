package mchorse.bbs_mod.mixin.client;

import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor
{
    @Invoker("setLivingFlag")
    void invokeSetLivingFlag(int mask, boolean value);
}
