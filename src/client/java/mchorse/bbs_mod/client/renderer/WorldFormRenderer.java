package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.Minecraft;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Runs immediate form draws after the world target has been cleared. */
public class WorldFormRenderer
{
    public interface Provider
    {
        WorldFormRenderer bbs$getWorldForms();
    }

    private final List<Runnable> draws = new ArrayList<>();
    private boolean collecting;

    public static WorldFormRenderer get()
    {
        return ((Provider) Minecraft.getInstance().levelRenderer).bbs$getWorldForms();
    }

    public static boolean defer(PoseStack matrices, Consumer<PoseStack> draw)
    {
        WorldFormRenderer renderer = get();

        if (!renderer.collecting || BBSRendering.isIrisShadowPass())
        {
            return false;
        }

        PoseStack snapshot = new PoseStack();
        snapshot.last().set(matrices.last());
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrixCopy());

        renderer.draws.add(() -> {
            Matrix4fStack stack = RenderSystem.getModelViewStack();
            stack.pushMatrix();
            stack.set(modelView);

            try
            {
                draw.accept(snapshot);
                FormUtilsClient.getProvider().draw();
            }
            finally
            {
                stack.popMatrix();
            }
        });

        return true;
    }

    public void beginSubmission()
    {
        this.draws.clear();
        this.collecting = true;
    }

    public void endSubmission()
    {
        this.collecting = false;
    }

    public void flush()
    {
        try
        {
            for (Runnable draw : this.draws)
            {
                draw.run();
            }
        }
        finally
        {
            this.draws.clear();
        }
    }
}
