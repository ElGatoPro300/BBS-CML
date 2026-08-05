package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.client.renderer.MorphFireRenderer;
import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TexturedRenderLayers;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

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
            MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE)
        );
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
        int overlay = LivingEntityRenderer.getOverlay(state, 0F);
        int light = state.light;

        this.setupTransforms(livingEntity, matrices, bodyYaw, tickDelta);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        FormUtilsClient.render(livingEntity.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, livingEntity.getWrappingEntity(), matrices, light, overlay, tickDelta)
            .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

        if (livingEntity.getWrappingEntity().getFireTicks() > 0)
        {
            MorphFireRenderer.render(
                matrices,
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                livingEntity.getWrappingEntity(),
                livingEntity.getForm(),
                tickDelta,
                MinecraftClient.getInstance().gameRenderer.getCamera(),
                false
            );
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        matrices.pop();

        super.render(state, matrices, queue, cameraState);
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

        if (entity.deathTime > 0)
        {
            float deathAngle = (entity.deathTime + tickDelta - 1F) / 20F * 1.6F;

            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Math.min(MathHelper.sqrt(deathAngle), 1F) * 90F));
        }
    }
}
