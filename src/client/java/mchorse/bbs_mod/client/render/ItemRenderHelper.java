package mchorse.bbs_mod.client.render;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.11 / 26.1 item draw path: ItemModelResolver fills {@link ItemStackRenderState}, then submits
 * into an {@link SubmitNodeCollector}.
 * An isolated queue/dispatcher is used for immediate previews to avoid clearing or corrupting
 * unrelated global queues.
 */
public final class ItemRenderHelper
{
    private static final ItemStackRenderState STATE = new ItemStackRenderState();
    private static SubmitNodeStorage isolatedQueue;
    private static FeatureRenderDispatcher isolatedDispatcher;

    private static void ensureIsolatedDispatcher()
    {
        if (isolatedDispatcher == null)
        {
            Minecraft client = Minecraft.getInstance();

            isolatedQueue = new SubmitNodeStorage();
            isolatedDispatcher = new FeatureRenderDispatcher(
                isolatedQueue,
                client.getModelManager(),
                client.renderBuffers().bufferSource(),
                client.getAtlasManager(),
                client.renderBuffers().outlineBufferSource(),
                client.renderBuffers().crumblingBufferSource(),
                client.font,
                client.gameRenderer.getGameRenderState()
            );
        }
    }

    private ItemRenderHelper()
    {}

    public static void renderItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, int light, int overlay, Level world, LivingEntity entity)
    {
        renderItem(stack, mode, matrices, light, overlay, world, entity, false);
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, int light, int overlay, Level world, LivingEntity entity, boolean flush)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        STATE.clear();

        if (entity != null)
        {
            client.getItemModelResolver().updateForLiving(STATE, stack, mode, entity);
        }
        else
        {
            client.getItemModelResolver().updateForTopItem(STATE, stack, mode, world, null, 0);
        }

        if (STATE.isEmpty())
        {
            return;
        }

        if (flush)
        {
            ensureIsolatedDispatcher();
            STATE.submit(matrices, isolatedQueue, light, overlay, 0);
            isolatedDispatcher.renderAllFeatures();
            FormUtilsClient.getProvider().draw();
        }
        else
        {
            SubmitNodeCollector queue = client.gameRenderer.getSubmitNodeStorage();

            STATE.submit(matrices, queue, light, overlay, 0);
        }
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, int light, int overlay, Level world, LivingEntity entity, SubmitNodeCollector queue)
    {
        if (stack == null || stack.isEmpty() || queue == null)
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        STATE.clear();

        if (entity != null)
        {
            client.getItemModelResolver().updateForLiving(STATE, stack, mode, entity);
        }
        else
        {
            client.getItemModelResolver().updateForTopItem(STATE, stack, mode, world, null, 0);
        }

        if (!STATE.isEmpty())
        {
            STATE.submit(matrices, queue, light, overlay, 0);
        }
    }

    private static final Map<SkullBlock.Type, SkullModelBase> SKULL_MODELS = new HashMap<>();

    private static SkullModelBase getSkullModel(SkullBlock.Type type)
    {
        SkullModelBase model = SKULL_MODELS.get(type);

        if (model == null)
        {
            Minecraft client = Minecraft.getInstance();

            model = SkullBlockRenderer.createModel(client.getEntityModels(), type);

            if (model != null)
            {
                SKULL_MODELS.put(type, model);
            }
        }

        return model;
    }

    public static void renderSkull(ItemStack stack, SkullBlock.Type skullType, float animationProgress, PoseStack matrices, int light, int overlay, Color color)
    {
        if (stack == null || stack.isEmpty() || skullType == null)
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        SkullModelBase skullModel = getSkullModel(skullType);

        if (skullModel == null)
        {
            return;
        }

        RenderType renderLayer;

        if (skullType == SkullBlock.Types.PLAYER)
        {
            ResolvableProfile profile = stack.get(DataComponents.PROFILE);

            if (profile != null)
            {
                renderLayer = client.playerSkinRenderCache().getOrDefault(profile).renderType();
            }
            else
            {
                renderLayer = SkullBlockRenderer.getSkullRenderType(skullType, null);
            }
        }
        else
        {
            renderLayer = SkullBlockRenderer.getSkullRenderType(skullType, null);
        }

        ensureIsolatedDispatcher();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BBSRendering.enableBlend());
        consumers.setSubstitute(BBSRendering.getColorConsumer(color));

        SkullBlockRenderer.submitSkull(180F, matrices, isolatedQueue, light, skullModel, renderLayer, 0, null);

        isolatedDispatcher.renderAllFeatures();
        client.renderBuffers().bufferSource().endBatch();
        consumers.draw();
        consumers.setSubstitute(null);
        CustomVertexConsumerProvider.clearRunnables();
    }
}
