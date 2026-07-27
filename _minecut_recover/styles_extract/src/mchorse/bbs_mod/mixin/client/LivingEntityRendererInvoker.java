package mchorse.bbs_mod.mixin.client;

import net.minecraft.class_1309;
import net.minecraft.class_922;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_922.class)
public interface LivingEntityRendererInvoker
{
    @Invoker("getAnimationCounter")
    public float bbs$getAnimationCounter(class_1309 entity, float tickDelta);
}