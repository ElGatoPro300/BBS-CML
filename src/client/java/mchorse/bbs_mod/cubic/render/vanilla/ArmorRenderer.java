package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Renders vanilla armor models over an {@link IEntity}'s biped model parts.
 *
 * <p>1.21.11 moved all armor rendering off ModelPart/BipedEntityModel entirely: it now goes through the new
 * equipment-render pipeline (package net.minecraft.client.render.entity.equipment), driven by
 * OrderedRenderCommandQueue. ModelPart also lost its
 * mutable pose fields ({@code pivotX/Y/Z}, {@code pitch/yaw/roll}, {@code xScale/yScale/zScale}) and its
 * {@code render(MatrixStack, VertexConsumer, int, int)} method (only {@code forEachCuboid(MatrixStack,
 * CuboidConsumer)} remains) — there is no direct replacement for the old "grab a part, zero its pose, render
 * it with a recolored VertexConsumer" approach used here. {@code BakedModelManager} also lost
 * {@code getAtlas(Identifier)} (the armor-trims sprite atlas lookup this class used to do in its constructor).
 *
 * <p>Faithfully reproducing the new equipment-model pipeline for a detached (non-LivingEntity) biped model is
 * a large, separate undertaking. Until that is done, this renderer intentionally draws nothing (see
 * {@link #renderArmorSlot}) so forms with equipped armor simply show no armor layer instead of crashing/
 * failing to compile. RenderLayers#armorCutoutNoCull(Identifier) /
 * #armorEntityGlint() and TexturedRenderLayers#getArmorTrims(boolean) are confirmed direct replacements
 * for the old RenderLayer statics, for whenever this gets revisited.</p>
 */
public class ArmorRenderer
{
    private static final Map<String, Identifier> ARMOR_TEXTURE_CACHE = Maps.newHashMap();
    private static final Identifier ELYTRA_TEXTURE = Identifier.of("minecraft", "textures/entity/elytra.png");
    private final BipedEntityModel innerModel;
    private final BipedEntityModel outerModel;
    private final ElytraEntityModel elytraModel;
    private final SpriteAtlasTexture armorTrimsAtlas;

    public ArmorRenderer(BipedEntityModel innerModel, BipedEntityModel outerModel, ElytraEntityModel elytraModel, BakedModelManager bakery)
    {
        this.innerModel = innerModel;
        this.outerModel = outerModel;
        this.elytraModel = elytraModel;
        this.armorTrimsAtlas = bakery.getAtlas(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
    }

    public void renderArmorSlot(MatrixStack matrices, VertexConsumerProvider vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        ItemStack itemStack = entity.getEquipmentStack(armorSlot);
        Item item = itemStack.getItem();

        if (item instanceof ElytraItem || itemStack.isOf(Items.ELYTRA))
        {
            if (type == ArmorType.CHEST && this.elytraModel != null)
            {
                matrices.push();
                /* Position Elytra at shoulder height and scale to fit back properly like Minecraft Vanilla */
                matrices.translate(0F, -1.5F, 0.125F);
                matrices.scale(2F, 2F, 2F);

                this.elytraModel.leftWing.pivotX = 5.0F;
                this.elytraModel.leftWing.pivotY = 0.0F;
                this.elytraModel.leftWing.pivotZ = 0.0F;

                this.elytraModel.rightWing.pivotX = -5.0F;
                this.elytraModel.rightWing.pivotY = 0.0F;
                this.elytraModel.rightWing.pivotZ = 0.0F;

                float transition = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
                float flyProgress = entity != null ? entity.getFallFlyingProgress(transition) : 0F;

                this.elytraModel.leftWing.pitch = MathHelper.lerp(flyProgress, 0.2617994F, 0.35F);
                this.elytraModel.leftWing.yaw = MathHelper.lerp(flyProgress, -0.015F, -0.1F);
                this.elytraModel.leftWing.roll = MathHelper.lerp(flyProgress, -0.29F, -1.55F);
                this.elytraModel.rightWing.pitch = this.elytraModel.leftWing.pitch;
                this.elytraModel.rightWing.yaw = -this.elytraModel.leftWing.yaw;
                this.elytraModel.rightWing.roll = -this.elytraModel.leftWing.roll;

                VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(ELYTRA_TEXTURE));
                this.elytraModel.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);

                if (itemStack.hasGlint())
                {
                    this.elytraModel.render(matrices, vertexConsumers.getBuffer(RenderLayer.getArmorEntityGlint()), light, OverlayTexture.DEFAULT_UV);
                }

                matrices.pop();
            }
            return;
        }

        if (item instanceof ArmorItem armorItem)
        {
            if (armorItem.getSlotType() == armorSlot)
            {
                boolean innerModel = this.usesInnerModel(armorSlot);
                BipedEntityModel bipedModel = this.getModel(armorSlot);
                ModelPart part = this.getPart(bipedModel, type);

                bipedModel.setVisible(true);

                part.pivotX = part.pivotY = part.pivotZ = 0F;
                part.pitch = part.yaw = part.roll = 0F;
                part.xScale = part.yScale = part.zScale = 1F;

                DyedColorComponent dyed = itemStack.get(DataComponentTypes.DYED_COLOR);
                if (dyed != null)
                {
                    int color = dyed.rgb();
                    float r = (float)(color >> 16 & 255) / 255.0F;
                    float g = (float)(color >> 8 & 255) / 255.0F;
                    float b = (float)(color & 255) / 255.0F;

                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, r, g, b, null);
                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, 1F, 1F, 1F, "overlay");
                }
                else
                {
                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, 1F, 1F, 1F, null);
                }

                ArmorTrim trim = itemStack.get(DataComponentTypes.TRIM);
                if (trim != null)
                {
                    this.renderTrim(part, armorItem.getMaterial(), matrices, vertexConsumers, light, trim, innerModel);
                }

                if (itemStack.hasGlint())
                {
                    this.renderGlint(part, matrices, vertexConsumers, light);
                }
            }
        }
    }
}
