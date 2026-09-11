package mchorse.bbs_mod.client.renderer.item;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Last {@link ItemDisplayContext} applied when an {@code ItemStackRenderState}
 * is submitted. Special model renderers in 26.1 no longer receive the mode.
 */
public final class ItemDisplayContextTracker
{
    private static final ThreadLocal<ItemDisplayContext> CURRENT = ThreadLocal.withInitial(() -> ItemDisplayContext.NONE);

    private ItemDisplayContextTracker()
    {
    }

    public static void set(ItemDisplayContext mode)
    {
        CURRENT.set(mode == null ? ItemDisplayContext.NONE : mode);
    }

    public static ItemDisplayContext get()
    {
        ItemDisplayContext mode = CURRENT.get();

        return mode == null ? ItemDisplayContext.NONE : mode;
    }

    /**
     * GUI atlas / oversized PIP often leave {@code NONE} on the render state.
     * Those passes are not world draws, so treat them as inventory.
     */
    public static ItemDisplayContext resolve()
    {
        ItemDisplayContext mode = get();

        if (mode != ItemDisplayContext.NONE)
        {
            return mode;
        }

        if (!BBSRendering.isRenderingWorld())
        {
            return ItemDisplayContext.GUI;
        }

        Minecraft client = Minecraft.getInstance();

        if (client != null && client.gui.screen() != null)
        {
            return ItemDisplayContext.GUI;
        }

        return ItemDisplayContext.NONE;
    }
}
