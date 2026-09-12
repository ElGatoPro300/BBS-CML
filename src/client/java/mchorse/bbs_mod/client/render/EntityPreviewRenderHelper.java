package mchorse.bbs_mod.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;

/**
 * Isolated entity preview draw path for form-list / morph thumbnails.
 * The global {@link RenderDispatcher} can defer layers (eyes, emissive features)
 * across cells; flushing on a private queue keeps each thumbnail self-contained.
 */
public final class EntityPreviewRenderHelper
{
    private static OrderedRenderCommandQueueImpl isolatedQueue;
    private static RenderDispatcher isolatedDispatcher;

    private EntityPreviewRenderHelper()
    {}

    private static void ensureIsolatedDispatcher()
    {
        if (isolatedDispatcher != null)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        isolatedQueue = new OrderedRenderCommandQueueImpl();
        isolatedDispatcher = new RenderDispatcher(
            isolatedQueue,
            client.getBlockRenderManager(),
            client.getBufferBuilders().getEntityVertexConsumers(),
            client.getAtlasManager(),
            client.getBufferBuilders().getOutlineVertexConsumers(),
            client.getBufferBuilders().getEffectVertexConsumers(),
            client.textRenderer
        );
    }

    public static OrderedRenderCommandQueue getQueue()
    {
        ensureIsolatedDispatcher();

        return isolatedQueue;
    }

    public static RenderDispatcher getDispatcher()
    {
        ensureIsolatedDispatcher();

        return isolatedDispatcher;
    }

    /** Flush vanilla entity layers submitted during the preview draw. */
    public static void flushEntityBuffers()
    {
        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
    }
}
