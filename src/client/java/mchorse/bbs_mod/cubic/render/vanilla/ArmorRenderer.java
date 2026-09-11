package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.client.renderer.MultiBufferSource;
import mchorse.bbs_mod.client.renderer.VertexMultiConsumer;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.iris.IrisArmorHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.google.common.collect.Maps;

import java.util.Map;

public class ArmorRenderer
{
    private static final Map<String, Identifier> ARMOR_TEXTURE_CACHE = Maps.newHashMap();
    private static final Identifier ELYTRA_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/equipment/wings/elytra.png");
    /** Outward shell — avoids coplanar z-fight with armor in film/world cameras (044b2f4a6). */
    private static final float TRIM_OUTER_SCALE = 1.005F;
    /** Inward shell — uniform outer scale alone hides trim on inner armor faces. */
    private static final float TRIM_INNER_SCALE = 0.995F;
    private final ArmorModelSet<HumanoidModel> armorModels;
    private final ElytraModel elytraModel;
    private final TextureAtlas armorTrimsAtlas;

    public ArmorRenderer(ArmorModelSet<HumanoidModel> armorModels, ElytraModel elytraModel, TextureAtlas armorTrimsAtlas)
    {
        this.armorModels = armorModels;
        this.elytraModel = elytraModel;
        this.armorTrimsAtlas = armorTrimsAtlas;
    }

    public ArmorRenderer(HumanoidModel innerModel, HumanoidModel outerModel, ElytraModel elytraModel, TextureAtlas armorTrimsAtlas)
    {
        this(new ArmorModelSet<>(outerModel, outerModel, innerModel, outerModel), elytraModel, armorTrimsAtlas);
    }

    public void renderArmorSlot(PoseStack matrices, MultiBufferSource vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        ItemStack itemStack = entity.getEquipmentStack(armorSlot);
        Item item = itemStack.getItem();
        MultiBufferSource buffers = IrisArmorHooks.wrapEntityBuffers(vertexConsumers);

        try (IrisArmorHooks.Scope ignored = IrisArmorHooks.beginArmorPiece(entity, item))
        {
            this.renderArmorSlotInner(matrices, buffers, entity, armorSlot, type, light, itemStack, item);
        }
    }

    private void renderArmorSlotInner(PoseStack matrices, MultiBufferSource vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light, ItemStack itemStack, Item item)
    {
        if (itemStack.is(Items.ELYTRA))
        {
            if (type == ArmorType.CHEST && this.elytraModel != null)
            {
                matrices.pushPose();
                matrices.translate(0F, 0F, 0.125F);

                this.elytraModel.leftWing.x = 5.0F;
                this.elytraModel.leftWing.y = 0.0F;
                this.elytraModel.leftWing.z = 0.0F;

                this.elytraModel.rightWing.x = -5.0F;
                this.elytraModel.rightWing.y = 0.0F;
                this.elytraModel.rightWing.z = 0.0F;

                float transition = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                float flyProgress = entity != null ? entity.getFallFlyingProgress(transition) : 0F;

                this.elytraModel.leftWing.xRot = Mth.lerp(flyProgress, 0.2617994F, 0.35F);
                this.elytraModel.leftWing.yRot = Mth.lerp(flyProgress, 0F, -0.1F);
                this.elytraModel.leftWing.zRot = Mth.lerp(flyProgress, -0.2617994F, -1.55F);
                this.elytraModel.rightWing.xRot = this.elytraModel.leftWing.xRot;
                this.elytraModel.rightWing.yRot = -this.elytraModel.leftWing.yRot;
                this.elytraModel.rightWing.zRot = -this.elytraModel.leftWing.zRot;

                VertexConsumer consumer = vertexConsumers.getBuffer(RenderTypes.armorCutoutNoCull(ELYTRA_TEXTURE));
                this.elytraModel.renderToBuffer(matrices, consumer, light, OverlayTexture.NO_OVERLAY, -1);

                if (itemStack.hasFoil())
                {
                    this.elytraModel.renderToBuffer(matrices, vertexConsumers.getBuffer(RenderTypes.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY, -1);
                }

                matrices.popPose();
            }
            return;
        }

        if (itemStack.get(DataComponents.EQUIPPABLE) != null)
        {
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);

            if (equippable != null && equippable.slot() == armorSlot)
            {
                boolean innerModel = this.usesInnerModel(armorSlot);
                HumanoidModel bipedModel = this.getModel(armorSlot);
                ModelPart part = this.getPart(bipedModel, type);

                this.setAllVisible(bipedModel, true);

                part.x = part.y = part.z = 0F;
                part.xRot = part.yRot = part.zRot = 0F;
                part.xScale = part.yScale = part.zScale = 1F;

                DyedItemColor dyed = itemStack.get(DataComponents.DYED_COLOR);
                if (dyed != null)
                {
                    int color = dyed.rgb();
                    float r = (float)(color >> 16 & 255) / 255.0F;
                    float g = (float)(color >> 8 & 255) / 255.0F;
                    float b = (float)(color & 255) / 255.0F;

                    this.renderArmorParts(part, matrices, vertexConsumers, light, itemStack, innerModel, r, g, b, null);
                    this.renderArmorParts(part, matrices, vertexConsumers, light, itemStack, innerModel, 1F, 1F, 1F, "overlay");
                }
                else
                {
                    this.renderArmorParts(part, matrices, vertexConsumers, light, itemStack, innerModel, 1F, 1F, 1F, null);
                }

                ArmorTrim trim = itemStack.get(DataComponents.TRIM);
                boolean hasTrim = trim != null;
                boolean hasGlint = itemStack.hasFoil();

                if (hasTrim)
                {
                    ResourceKey<EquipmentAsset> assetKey = equippable != null && equippable.assetId().isPresent() ? equippable.assetId().get() : null;
                    /* Trim glint is a separate EQUAL pass fed the same verts as the trim
                     * shells (union), not a second scaled ModelPart.render — that desync
                     * was the trim↔glint z-fight. Base armor still gets its own 1.0 glint. */
                    this.renderTrim(part, assetKey, matrices, vertexConsumers, light, trim, innerModel, hasGlint);
                }

                if (hasGlint)
                {
                    this.renderGlint(part, matrices, vertexConsumers, light);
                }
            }
        }
    }

    private ModelPart getPart(HumanoidModel bipedModel, ArmorType type)
    {
        switch (type)
        {
            case HELMET -> {
                return bipedModel.head;
            }
            case CHEST, LEGGINGS -> {
                return bipedModel.body;
            }
            case LEFT_ARM -> {
                return bipedModel.leftArm;
            }
            case RIGHT_ARM -> {
                return bipedModel.rightArm;
            }
            case LEFT_LEG, LEFT_BOOT -> {
                return bipedModel.leftLeg;
            }
            case RIGHT_LEG, RIGHT_BOOT -> {
                return bipedModel.rightLeg;
            }
        }

        return bipedModel.head;
    }

    private void renderArmorParts(ModelPart part, PoseStack matrices, MultiBufferSource vertexConsumers, int light, ItemStack stack, boolean secondTextureLayer, float red, float green, float blue, String overlay)
    {
        VertexConsumer base = vertexConsumers.getBuffer(RenderTypes.armorCutoutNoCull(this.getArmorTexture(stack, secondTextureLayer, overlay)));
        VertexConsumer vertexConsumer = new RecolorVertexConsumer(base, new Color(red, green, blue, 1F));

        part.render(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
    }

    private void renderTrim(ModelPart part, ResourceKey<EquipmentAsset> armorAssetKey, PoseStack matrices, MultiBufferSource vertexConsumers, int light, ArmorTrim trim, boolean leggings, boolean withGlint)
    {
        TextureAtlasSprite sprite = this.armorTrimsAtlas.getSprite(this.getTrimTexture(trim, armorAssetKey, leggings));
        VertexConsumer trimConsumer = sprite.wrap(vertexConsumers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
        VertexConsumer vertexConsumer = withGlint
            ? VertexMultiConsumer.create(trimConsumer, vertexConsumers.getBuffer(RenderTypes.armorEntityGlint()))
            : trimConsumer;

        /* Armor + trim share the same ModelPart. Uniform 1.005 alone hides inner faces
         * (shell expands away from the cavity). Dual shells keep film/world z-fight fix
         * (044b2f4a6) while restoring inside trim like vanilla coplanar NoCull geometry.
         * Scale is baked into vertices — draw is deferred, so GL polygon-offset at submit
         * would not stick through consumers.draw(). */
        IrisArmorHooks.beginTrim(trim);

        try
        {
            this.renderScaledPart(part, matrices, vertexConsumer, light, TRIM_OUTER_SCALE);
            this.renderScaledPart(part, matrices, vertexConsumer, light, TRIM_INNER_SCALE);
        }
        finally
        {
            IrisArmorHooks.endTrim();
        }
    }

    private Identifier getTrimTexture(ArmorTrim trim, ResourceKey<EquipmentAsset> armorAssetKey, boolean leggings)
    {
        Identifier patternId = trim.pattern().value().assetId();
        MaterialAssetGroup assets = trim.material().value().assets();
        MaterialAssetGroup.AssetInfo assetId = armorAssetKey != null
            ? assets.assetId(armorAssetKey)
            : assets.base();
        String materialName = assetId.suffix();
        String suffix = leggings ? "_leggings" : "";

        return Identifier.fromNamespaceAndPath(patternId.getNamespace(), "trims/models/armor/" + patternId.getPath() + "_" + materialName + suffix);
    }

    private void renderGlint(ModelPart part, PoseStack matrices, MultiBufferSource vertexConsumers, int light)
    {
        part.render(matrices, vertexConsumers.getBuffer(RenderTypes.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
    }

    private void renderScaledPart(ModelPart part, PoseStack matrices, VertexConsumer vertexConsumer, int light, float scale)
    {
        matrices.pushPose();
        matrices.scale(scale, scale, scale);
        part.render(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        matrices.popPose();
    }

    private HumanoidModel getModel(EquipmentSlot slot)
    {
        return this.armorModels.get(slot);
    }

    private boolean usesInnerModel(EquipmentSlot slot)
    {
        return slot == EquipmentSlot.LEGS;
    }

    private Identifier getArmorTexture(ItemStack stack, boolean secondLayer, String overlay)
    {
        // Use default if not found
        String materialName = "unknown";
        
        // Try to get from components
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.assetId().isPresent())
        {
            materialName = equippable.assetId().get().identifier().getPath();
        }

        String id = "textures/entity/equipment/" + (secondLayer ? "humanoid_leggings" : "humanoid") + "/" + materialName + (overlay == null ? "" : "_" + overlay) + ".png";

        Identifier found = ARMOR_TEXTURE_CACHE.get(id);
        if (found == null)
        {
            found = Identifier.fromNamespaceAndPath("minecraft", id);
            ARMOR_TEXTURE_CACHE.put(id, found);
        }

        return found;
    }

    private void setAllVisible(HumanoidModel<?> model, boolean visible)
    {
        model.head.visible = visible;
        model.hat.visible = visible;
        model.body.visible = visible;
        model.rightArm.visible = visible;
        model.leftArm.visible = visible;
        model.rightLeg.visible = visible;
        model.leftLeg.visible = visible;
    }
}
