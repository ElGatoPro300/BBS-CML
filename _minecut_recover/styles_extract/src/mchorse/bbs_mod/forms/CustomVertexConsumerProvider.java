package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.forms.renderers.utils.BlockPaintOverlayVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.GlowEmissionVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import net.minecraft.class_1921;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomVertexConsumerProvider implements class_4597
{
    private static Consumer<class_1921> runnables;

    private final class_4597.class_4598 delegate;
    private Function<class_4588, class_4588> substitute;
    private boolean ui;

    public static void drawLayer(class_1921 layer)
    {
        if (runnables != null)
        {
            runnables.accept(layer);
        }
    }

    public static void hijackVertexFormat(Consumer<class_1921> runnable)
    {
        runnables = runnable;
    }

    public static void clearRunnables()
    {
        runnables = null;
    }

    public CustomVertexConsumerProvider(class_4597.class_4598 delegate)
    {
        this.delegate = delegate;
    }

    public Function<class_4588, class_4588> getSubstitute()
    {
        return this.substitute;
    }

    public void setSubstitute(Function<class_4588, class_4588> substitute)
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
    public class_4588 getBuffer(class_1921 renderLayer)
    {
        class_4588 buffer = this.delegate.getBuffer(renderLayer);

        if (this.substitute != null)
        {
            class_4588 apply = this.substitute.apply(buffer);

            if (apply != null)
            {
                return apply;
            }
        }

        return buffer;
    }

    public void draw()
    {
        this.delegate.method_22993();

        if (this.ui)
        {
            /* Force back the depth func because it seems like stuff rendered by a vertex
             * consumer is resetting the depth func to GL_LESS, and since this vertex consumer
             * is designed  */
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }
}
