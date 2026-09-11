package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MultiBufferSource;
import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * MobForm body/armor stay on the private Immediate. Builtin held meshes
 * (trident, shield, skulls) tessellate on the world entity Immediate.
 */
@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin
{
    @ModifyVariable(
        method = "renderItem",
        at = @At("HEAD"),
        argsOnly = true
    )
    private MultiBufferSource bbs$routeMobFormBuiltinItems(
        MultiBufferSource consumers,
        LivingEntity entity,
        ItemStack stack,
        ItemDisplayContext mode,
        boolean leftHanded,
        PoseStack matrices,
        MultiBufferSource ignored,
        int light
    )
    {
        return FormUtilsClient.routeMobFormBuiltinItemConsumers(stack, mode, consumers);
    }
}
