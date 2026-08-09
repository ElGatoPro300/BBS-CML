package mchorse.bbs_mod.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.joml.Quaternionf;

/**
 * 1.21.11 entity draw path: submit into {@link OrderedRenderCommandQueue}, then optionally flush
 * via {@link RenderDispatcher#render()}.
 */
public final class EntityRenderHelper
{
    private static final CameraRenderState UI_CAMERA = new CameraRenderState();

    static
    {
        UI_CAMERA.initialized = true;
        UI_CAMERA.blockPos = BlockPos.ORIGIN;
        UI_CAMERA.pos = Vec3d.ZERO;
        UI_CAMERA.entityPos = Vec3d.ZERO;
        UI_CAMERA.orientation = new Quaternionf();
    }

    private EntityRenderHelper()
    {
    }

    public static void renderEntityState(EntityRenderState state, MatrixStack matrices, boolean flush)
    {
        if (state == null)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        OrderedRenderCommandQueue queue = client.gameRenderer.getEntityRenderCommandQueue();

        if (queue == null)
        {
            return;
        }

        CameraRenderState camera = UI_CAMERA;
        WorldRenderState worldStates = client.gameRenderer.getEntityRenderStates();

        if (worldStates != null && worldStates.cameraRenderState != null && worldStates.cameraRenderState.initialized)
        {
            camera = worldStates.cameraRenderState;
        }

        EntityRenderManager dispatcher = client.getEntityRenderDispatcher();

        dispatcher.render(state, camera, 0D, 0D, 0D, matrices, queue);

        if (flush)
        {
            RenderDispatcher renderDispatcher = client.gameRenderer.getEntityRenderDispatcher();

            renderDispatcher.render();
        }
    }
}
