package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.MorphFireRenderer;
import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.iris.IrisUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.lwjgl.opengl.GL11;

public class ActorEntityRenderer extends EntityRenderer<ActorEntity, ActorEntityRenderer.ActorEntityState>
{
    public static class ActorEntityState extends LivingEntityRenderState {
        public ActorEntity entity;
        public float tickDelta;
        public float bodyYaw;
        public float prevBodyYaw;
        public float deathTime;
        public boolean isSleeping;
    }

    public static ArmorRenderer armorRenderer;

    public ActorEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);

        armorRenderer = new ArmorRenderer(
            new HumanoidModel(ctx.bakeLayer(ModelLayers.PLAYER_ARMOR.get(EquipmentSlot.LEGS))),
            new HumanoidModel(ctx.bakeLayer(ModelLayers.PLAYER_ARMOR.get(EquipmentSlot.CHEST))),
            new ElytraModel(ctx.bakeLayer(ModelLayers.ELYTRA)),
            Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ARMOR_TRIMS)
        );
    }

    /**
     * Match film stub shadows: vanilla ground blob only when Iris/shaders are off.
     * With a shader pack the mesh already casts into the shadow map; keeping the blob
     * stacks two dark circles under the actor.
     */
    public static void updateShadowRadius(ActorEntity entity)
    {
        if (entity == null)
        {
            return;
        }

        EntityRenderer<?, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        if (renderer instanceof ActorEntityRenderer actorRenderer)
        {
            actorRenderer.shadowRadius = IrisUtils.isShaderPackEnabled() ? 0F : 0.5F;
        }
    }

    @Override
    public ActorEntityState createRenderState() {
        return new ActorEntityState();
    }

    @Override
    public void updateRenderState(ActorEntity entity, ActorEntityState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.entity = entity;
        state.tickDelta = tickDelta;
        state.bodyYaw = entity.getBodyYaw();
        state.prevBodyYaw = entity.lastBodyYaw;
        state.deathTime = (float)entity.deathTime;
        state.isSleeping = entity.isInPose(Pose.SLEEPING);
    }

    public Identifier getTexture(ActorEntityState state)
    {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    public void render(ActorEntityState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState)
    {
        ActorEntity livingEntity = state.entity;
        if (livingEntity == null) return;

        float tickDelta = state.tickDelta;
        
        matrices.pushPose();

        float bodyYaw = Mth.rotLerp(tickDelta, state.prevBodyYaw, state.bodyYaw);
        int overlay = livingEntity.shouldShowDamageFlashOverlay()
            ? LivingEntityRenderer.getOverlayCoords(state, 0F)
            : OverlayTexture.NO_OVERLAY;
        float animDelta = livingEntity.areNaturalAnimationsPaused() ? 0F : tickDelta;

        this.setupTransforms(livingEntity, matrices, bodyYaw, animDelta);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        FormUtilsClient.render(livingEntity.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, livingEntity.getWrappingEntity(), matrices, state.light, overlay, animDelta)
            .camera(Minecraft.getInstance().gameRenderer.getMainCamera()));

        if (livingEntity.getWrappingEntity().getFireTicks() > 0)
        {
            MorphFireRenderer.render(
                matrices,
                Minecraft.getInstance().renderBuffers().bufferSource(),
                livingEntity.getWrappingEntity(),
                livingEntity.getForm(),
                animDelta,
                Minecraft.getInstance().gameRenderer.getMainCamera(),
                false
            );
        }

        BBSRendering.restoreWorldRenderState();
        GlStateManager._disableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._disableBlend();

        matrices.popPose();

        super.submit(state, matrices, queue, cameraState);
    }

    @Override
    protected boolean hasLabel(ActorEntity entity, double squaredDistanceToCamera)
    {
        /* Same visibility rules as stub film nametags / vanilla labels. */
        return entity.hasCustomName();
    }

    protected boolean isVisible(ActorEntity entity)
    {
        return !entity.isInvisible();
    }

    protected void setupTransforms(ActorEntity entity, PoseStack matrices, float bodyYaw, float tickDelta)
    {
        if (!entity.isInPose(Pose.SLEEPING))
        {
            matrices.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        }

        if (entity.deathTime > 0 && !(entity.getForm() instanceof MobForm))
        {
            float deathAngle = (entity.deathTime + tickDelta - 1F) / 20F * 1.6F;

            matrices.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathAngle), 1F) * 90F));
        }
    }
}
