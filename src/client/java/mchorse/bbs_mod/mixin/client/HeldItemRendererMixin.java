package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.vertex.PoseStack;

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
    private SubmitNodeCollector bbs$routeMobFormBuiltinItems(
        SubmitNodeCollector collector,
        LivingEntity entity,
        ItemStack stack,
        ItemDisplayContext mode,
        PoseStack matrices,
        SubmitNodeCollector ignored,
        int light
    )
    {
        return collector;
    }
}
