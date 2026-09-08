package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.forms.renderers.utils.BlockPaintOverlayVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.GlowEmissionVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomVertexConsumerProvider implements MultiBufferSource
{
    private static Consumer<RenderType> runnables;

    private final MultiBufferSource.BufferSource delegate;
    private Function<VertexConsumer, VertexConsumer> substitute;
    private boolean ui;

    public static void drawLayer(RenderType layer)
    {
        if (runnables != null)
        {
            runnables.accept(layer);
        }
    }

    public static void hijackVertexFormat(Consumer<RenderType> runnable)
    {
        runnables = runnable;
    }

    public static void clearRunnables()
    {
        runnables = null;
    }

    public CustomVertexConsumerProvider(MultiBufferSource.BufferSource delegate)
    {
        this.delegate = delegate;
    }

    public Function<VertexConsumer, VertexConsumer> getSubstitute()
    {
        return this.substitute;
    }

    public void setSubstitute(Function<VertexConsumer, VertexConsumer> substitute)
    {
        this.substitute = substitute;

        if (this.substitute == null)
        {
            RecolorVertexConsumer.newColor = null;
            RecolorVertexConsumer.newPaintColor = null;
            GlowEmissionVertexConsumer.emissionColor = null;
            BlockPaintOverlayVertexConsumer.paintOverlayColor = null;
        }
    }

    public void setUI(boolean ui)
    {
        this.ui = ui;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderLayer)
    {
        VertexConsumer buffer = this.delegate.getBuffer(renderLayer);

        if (this.substitute != null)
        {
            VertexConsumer apply = this.substitute.apply(buffer);

            if (apply != null)
            {
                return apply;
            }
        }

        return buffer;
    }

    public void draw()
    {
        this.delegate.endBatch();

        if (this.ui)
        {
            /* Force back the depth func because it seems like stuff rendered by a vertex
             * consumer is resetting the depth func to GL_LESS, and since this vertex consumer
             * is designed  */
            GlStateManager._depthFunc(GL11.GL_ALWAYS);
        }
    }

    /**
     * Flushes only the active dynamic layer (e.g. last villager clothing pass) without
     * iterating fixed world layerBuffers.
     */
    public void drawCurrentLayer()
    {
        this.delegate.endLastBatch();
    }
}
