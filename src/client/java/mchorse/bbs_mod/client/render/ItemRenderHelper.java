package mchorse.bbs_mod.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 1.21.11 item draw path: ItemModelManager fills {@link ItemRenderState}, then submits
 * into an {@link OrderedRenderCommandQueue}.
 */
public final class ItemRenderHelper
{
    private static final ItemRenderState STATE = new ItemRenderState();

    private ItemRenderHelper()
    {
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext mode, MatrixStack matrices, int light, int overlay, World world, LivingEntity entity)
    {
        renderItem(stack, mode, matrices, light, overlay, world, entity, false);
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext mode, MatrixStack matrices, int light, int overlay, World world, LivingEntity entity, boolean flush)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        STATE.clear();

        if (entity != null)
        {
            client.getItemModelManager().updateForLivingEntity(STATE, stack, mode, entity);
        }
        else
        {
            client.getItemModelManager().clearAndUpdate(STATE, stack, mode, world, null, 0);
        }

        if (STATE.isEmpty())
        {
            return;
        }

        OrderedRenderCommandQueue queue = client.gameRenderer.getEntityRenderCommandQueue();

        STATE.render(matrices, queue, light, overlay, 0);

        if (flush)
        {
            client.gameRenderer.getEntityRenderDispatcher().render();
        }
    }
}
