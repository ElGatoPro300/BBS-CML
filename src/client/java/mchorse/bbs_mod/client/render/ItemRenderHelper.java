package mchorse.bbs_mod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 1.21.11 item draw path: ItemModelManager fills {@link ItemStackRenderState}, then submits
 * into an {@link SubmitNodeCollector}.
 */
public final class ItemRenderHelper
{
    private static final ItemStackRenderState STATE = new ItemStackRenderState();

    private ItemRenderHelper()
    {
    }

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

        SubmitNodeCollector queue = client.gameRenderer.getSubmitNodeStorage();

        STATE.submit(matrices, queue, light, overlay, 0);

        if (flush)
        {
            client.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
        }
    }
}
