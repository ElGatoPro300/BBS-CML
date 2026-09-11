package mchorse.bbs_mod.client.render;

import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 1.21.11 item draw path: ItemModelManager fills {@link ItemStackRenderState}, then submits
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
                client.gameRenderer.renderBuffers(),
                client.getModelManager(),
                client.getAtlasManager(),
                client.font,
                client.gameRenderer.gameRenderState()
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
            isolatedDispatcher.renderAllFeatures(isolatedQueue);
            FormUtilsClient.getProvider().draw();
        }
        else
        {
            SubmitNodeStorage queue = new SubmitNodeStorage();

            STATE.submit(matrices, queue, light, overlay, 0);
            client.gameRenderer.featureRenderDispatcher().renderAllFeatures(queue);
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
}
