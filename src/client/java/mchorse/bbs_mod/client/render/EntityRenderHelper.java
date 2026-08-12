package mchorse.bbs_mod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 1.21.11 entity draw path: submit into {@link SubmitNodeCollector}, then optionally flush
 * via {@link FeatureRenderDispatcher#renderAllFeatures()}.
 */
public final class EntityRenderHelper
{
    private static final CameraRenderState UI_CAMERA = new CameraRenderState();

    static
    {
        UI_CAMERA.initialized = true;
        UI_CAMERA.blockPos = BlockPos.ZERO;
        UI_CAMERA.pos = Vec3.ZERO;
        UI_CAMERA.entityPos = Vec3.ZERO;
        UI_CAMERA.orientation = new Quaternionf();
    }

    private EntityRenderHelper()
    {
    }

    public static void renderEntityState(EntityRenderState state, PoseStack matrices, boolean flush)
    {
        if (state == null)
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        SubmitNodeCollector queue = client.gameRenderer.getSubmitNodeStorage();

        if (queue == null)
        {
            return;
        }

        CameraRenderState camera = UI_CAMERA;
        LevelRenderState worldStates = client.gameRenderer.getLevelRenderState();

        if (worldStates != null && worldStates.cameraRenderState != null && worldStates.cameraRenderState.initialized)
        {
            camera = worldStates.cameraRenderState;
        }

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        dispatcher.submit(state, camera, 0D, 0D, 0D, matrices, queue);

        if (flush)
        {
            FeatureRenderDispatcher renderDispatcher = client.gameRenderer.getFeatureRenderDispatcher();

            renderDispatcher.renderAllFeatures();
        }
    }
}
