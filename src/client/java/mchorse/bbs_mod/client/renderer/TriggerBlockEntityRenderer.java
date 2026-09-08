package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.graphics.Draw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.HashSet;
import java.util.Set;

public class TriggerBlockEntityRenderer implements BlockEntityRenderer<TriggerBlockEntity, TriggerBlockEntityRenderState>
{
    public static final Set<TriggerBlockEntity> capturedTriggerBlocks = new HashSet<>();

    public TriggerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx)
    {}

    @Override
    public TriggerBlockEntityRenderState createRenderState()
    {
        return new TriggerBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(TriggerBlockEntity entity, TriggerBlockEntityRenderState state, float tickDelta, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay)
    {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.entity = entity;
        capturedTriggerBlocks.add(entity);
    }

    @Override
    public void render(TriggerBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState)
    {
        TriggerBlockEntity entity = state.entity;

        if (entity == null)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        
        if (mc.getDebugOverlay().showDebugScreen())
        {
            matrices.pushPose();
            matrices.translate(0.5D, 0, 0.5D);
            /* Render green debug box for triggers */
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 1F, 0.5F, 0.5F);
            matrices.popPose();

            if (entity.region.get())
            {
                AABB box = entity.getRegionBoxRelative();

                /* Render white debug box for region triggers */
                GlStateManager._disableDepthTest();
                Draw.renderBox(matrices, box.minX, box.minY, box.minZ, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ, 1F, 1F, 1F, 0.5F);
                GlStateManager._enableDepthTest();
            }
        }
    }
}
