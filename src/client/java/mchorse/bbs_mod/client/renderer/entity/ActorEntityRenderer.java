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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import com.mojang.blaze3d.opengl.GlStateManager;

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

    public ActorEntityRenderer(EntityRendererFactory.Context ctx)
    {
        super(ctx);

        armorRenderer = new ArmorRenderer(
            new BipedEntityModel(ctx.getPart(EntityModelLayers.PLAYER_EQUIPMENT.getModelData(EquipmentSlot.LEGS))),
            new BipedEntityModel(ctx.getPart(EntityModelLayers.PLAYER_EQUIPMENT.getModelData(EquipmentSlot.CHEST))),
            new ElytraEntityModel(ctx.getPart(EntityModelLayers.ELYTRA)),
            MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(Atlases.ARMOR_TRIMS)
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

        EntityRenderer<?, ?> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);

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
        super.updateRenderState(entity, state, tickDelta);
        state.entity = entity;
        state.tickDelta = tickDelta;
        state.bodyYaw = entity.getBodyYaw();
        state.prevBodyYaw = entity.lastBodyYaw;
        state.deathTime = (float)entity.deathTime;
        state.isSleeping = entity.isInPose(EntityPose.SLEEPING);
    }

    public Identifier getTexture(ActorEntityState state)
    {
        return Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    public void render(ActorEntityState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState)
    {
        ActorEntity livingEntity = state.entity;
        if (livingEntity == null) return;

        float tickDelta = state.tickDelta;
        
        matrices.push();

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, state.prevBodyYaw, state.bodyYaw);
        int overlay = livingEntity.shouldShowDamageFlashOverlay()
            ? LivingEntityRenderer.getOverlay(state, 0F)
            : OverlayTexture.DEFAULT_UV;
        float animDelta = livingEntity.areNaturalAnimationsPaused() ? 0F : tickDelta;

        this.setupTransforms(livingEntity, matrices, bodyYaw, animDelta);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        FormUtilsClient.render(livingEntity.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, livingEntity.getWrappingEntity(), matrices, state.light, overlay, animDelta)
            .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

        if (livingEntity.getWrappingEntity().getFireTicks() > 0)
        {
            MorphFireRenderer.render(
                matrices,
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                livingEntity.getWrappingEntity(),
                livingEntity.getForm(),
                animDelta,
                MinecraftClient.getInstance().gameRenderer.getCamera(),
                false
            );
        }

        BBSRendering.restoreWorldRenderState();
        GlStateManager._disableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._disableBlend();

        matrices.pop();

        super.render(state, matrices, queue, cameraState);
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

    protected void setupTransforms(ActorEntity entity, MatrixStack matrices, float bodyYaw, float tickDelta)
    {
        if (!entity.isInPose(EntityPose.SLEEPING))
        {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        }

        if (entity.deathTime > 0 && !(entity.getForm() instanceof MobForm))
        {
            float deathAngle = (entity.deathTime + tickDelta - 1F) / 20F * 1.6F;

            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Math.min(MathHelper.sqrt(deathAngle), 1F) * 90F));
        }
    }
}
