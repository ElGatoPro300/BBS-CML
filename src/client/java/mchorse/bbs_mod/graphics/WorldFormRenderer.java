package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Keeps immediate form draws out of vanilla's feature submission phase. */
public final class WorldFormRenderer
{
    private final List<Runnable> draws = new ArrayList<>();

    public static WorldFormRenderer get()
    {
        return ((Provider) Minecraft.getInstance().gameRenderer).bbs$getWorldForms();
    }

    public void submit(PoseStack matrices, Consumer<PoseStack> draw)
    {
        PoseStack snapshot = new PoseStack();

        snapshot.last().pose().set(matrices.last().pose());
        snapshot.last().normal().set(matrices.last().normal());

        if (BBSRendering.isIrisShadowPass() || !BBSRendering.isRenderingWorld())
        {
            draw.accept(snapshot);
        }
        else
        {
            this.draws.add(() -> draw.accept(snapshot));
        }
    }

    public void flush()
    {
        try
        {
            for (Runnable draw : this.draws)
            {
                draw.run();
            }

            FormUtilsClient.getProvider().draw();
        }
        finally
        {
            this.clear();
        }
    }

    public void clear()
    {
        this.draws.clear();
    }

    public interface Provider
    {
        WorldFormRenderer bbs$getWorldForms();

        void bbs$setMainRenderTarget(RenderTarget target);
    }
}
