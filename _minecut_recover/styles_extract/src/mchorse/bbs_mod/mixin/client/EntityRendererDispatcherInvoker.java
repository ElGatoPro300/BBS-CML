package mchorse.bbs_mod.mixin.client;

import net.minecraft.class_1297;
import net.minecraft.class_4538;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_898;
import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_898.class)
public interface EntityRendererDispatcherInvoker
{
    @Invoker("renderShadow")
    public static void bbs$renderShadow(class_4587 matrices, class_4597 vertexConsumers, class_1297 entity, float opacity, float tickDelta, class_4538 world, float radius)
    {}

    @Invoker("renderFire")
    void bbs$renderFire(class_4587 matrices, class_4597 vertexConsumers, class_1297 entity, Quaternionf rotation);
}