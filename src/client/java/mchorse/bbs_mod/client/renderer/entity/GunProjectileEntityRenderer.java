package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class GunProjectileEntityRenderer extends EntityRenderer<GunProjectileEntity, GunProjectileEntityRenderer.GunProjectileEntityState>
{
    public static class GunProjectileEntityState extends EntityRenderState {
        public GunProjectileEntity projectile;
        public float tickDelta;
    }

    public GunProjectileEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public GunProjectileEntityState createRenderState() {
        return new GunProjectileEntityState();
    }

    @Override
    public void updateRenderState(GunProjectileEntity entity, GunProjectileEntityState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.projectile = entity;
        state.tickDelta = tickDelta;
    }

    public Identifier getTexture(GunProjectileEntityState state)
    {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    public void render(GunProjectileEntityState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState)
    {
        GunProjectileEntity projectile = state.projectile;
        if (projectile == null) return;
        
        float tickDelta = state.tickDelta;

        matrices.pushPose();

        GunProperties properties = projectile.getProperties();
        int out = properties.lifeSpan - 2;

        float bodyYaw = projectile.getYaw();
        float pitch = projectile.getPitch();
        float scale = Lerps.envelope(projectile.age + tickDelta, 0, properties.fadeIn, out - properties.fadeOut, out);

        if (properties.yaw) matrices.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        if (properties.pitch) matrices.mulPose(Axis.XP.rotationDegrees(-pitch));
        matrices.scale(scale, scale, scale);
        MatrixStackUtils.applyTransform(matrices, properties.projectileTransform);

        GlStateManager._enableDepthTest();
        FormUtilsClient.render(projectile.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, new MCEntity(projectile), matrices, state.light, OverlayTexture.NO_OVERLAY, tickDelta)
            .camera(Minecraft.getInstance().gameRenderer.getMainCamera()));
        GlStateManager._disableDepthTest();

        matrices.popPose();

        super.submit(state, matrices, queue, cameraState);
    }
}
