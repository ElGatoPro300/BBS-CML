package mchorse.bbs_mod.client.render;

import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

/**
 * Isolated entity preview draw path for form-list / morph thumbnails.
 * The global {@link FeatureRenderDispatcher} can defer layers (eyes, emissive features)
 * across cells; flushing on a private queue keeps each thumbnail self-contained.
 */
public final class EntityPreviewRenderHelper
{
    private static SubmitNodeStorage isolatedQueue;
    private static FeatureRenderDispatcher isolatedDispatcher;

    private EntityPreviewRenderHelper()
    {}

    private static void ensureIsolatedDispatcher()
    {
        if (isolatedDispatcher != null)
        {
            return;
        }

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

    public static SubmitNodeStorage getQueue()
    {
        ensureIsolatedDispatcher();

        return isolatedQueue;
    }

    public static FeatureRenderDispatcher getDispatcher()
    {
        ensureIsolatedDispatcher();

        return isolatedDispatcher;
    }

    /** Flush entity layers submitted during the preview draw. */
    public static void flushEntityBuffers()
    {
        FormUtilsClient.getProvider().draw();
    }
}
