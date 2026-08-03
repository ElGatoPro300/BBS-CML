package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;

/**
 * Renders vanilla armor models over an {@link IEntity}'s biped model parts.
 *
 * <p>1.21.11 moved all armor rendering off ModelPart/BipedEntityModel entirely: it now goes through the new
 * equipment-render pipeline (package net.minecraft.client.render.entity.equipment), driven by
 * OrderedRenderCommandQueue. ModelPart also lost its mutable pose fields and its render method.
 * Faithfully reproducing the new equipment-model pipeline for a detached (non-LivingEntity) biped model is
 * a large, separate undertaking. Until that is done, this renderer intentionally draws nothing so forms
 * with equipped armor simply show no armor layer instead of crashing/failing to compile.</p>
 */
public class ArmorRenderer
{
    public ArmorRenderer(BipedEntityModel innerModel, BipedEntityModel outerModel, ElytraEntityModel elytraModel, BakedModelManager bakery)
    {}

    public void renderArmorSlot(MatrixStack matrices, VertexConsumerProvider vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {}
}
