package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.MorphFireRenderer;
import mchorse.bbs_mod.client.renderer.MultiBufferSource;
import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.utils.FormDeathTilt;
import mchorse.bbs_mod.graphics.WorldFormRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.lwjgl.opengl.GL11;

public class ActorEntityRenderer extends EntityRenderer<ActorEntity, ActorEntityRenderer.ActorEntityState>
{
    public static class ActorEntityState extends LivingEntityRenderState
    {
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

        /* Private copies — ArmorRenderer mutates pivots/wings; never share with vanilla players. */
        armorRenderer = new ArmorRenderer(
            ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, ctx.getModelSet(), HumanoidModel::new),
            new ElytraModel(ctx.bakeLayer(ModelLayers.ELYTRA)),
            Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ARMOR_TRIMS)
        );
    }

    /**
     * Keep dispatcher {@link #shadowRadius} in sync with this entity's film shadow.
     * Without shaders the ground blob is drawn in {@link #submit} (size X/Z + offset);
     * with a shader pack the vanilla radius is used so packs that still sample the
     * shadow {@code .png} can respect the replay toggle / size.
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
            actorRenderer.applyShadowRadius(entity);
        }
    }

    private void applyShadowRadius(ActorEntity entity)
    {
        if (!entity.shouldRenderFilmGroundShadow())
        {
            this.shadowRadius = 0F;

            return;
        }

        float radius = Math.max(entity.getFilmShadowRadiusX(), entity.getFilmShadowRadiusZ());

        if (BBSRendering.isIrisShadersEnabled())
        {
            /* Packs that still draw the vanilla shadow texture honor this; Comp/BSL
             * mesh shadows are separate and stay as they are for stubs. */
            this.shadowRadius = radius;
        }
        else
        {
            /* Custom blob below handles XZ / offset — suppress the circular default. */
            this.shadowRadius = 0F;
        }
    }

    @Override
    public ActorEntityState createRenderState()
    {
        return new ActorEntityState();
    }

    @Override
    public void extractRenderState(ActorEntity entity, ActorEntityState state, float tickDelta)
    {
        super.extractRenderState(entity, state, tickDelta);
        state.entity = entity;
        state.tickDelta = tickDelta;
        state.bodyYaw = entity.yBodyRot;
        state.prevBodyYaw = entity.yBodyRotO;
        state.deathTime = (float) entity.deathTime;
        state.isSleeping = entity.hasPose(Pose.SLEEPING);
    }

    public Identifier getTexture(ActorEntityState state)
    {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }

    @Override
    public void submit(ActorEntityState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState)
    {
        WorldFormRenderer.get().submit(matrices, snapshot -> this.renderWorld(state, snapshot));
        super.submit(state, matrices, queue, cameraState);
    }

    private void renderWorld(ActorEntityState state, PoseStack matrices)
    {
        ActorEntity livingEntity = state.entity;

        if (livingEntity == null)
        {
            return;
        }

        float tickDelta = state.tickDelta;

        this.applyShadowRadius(livingEntity);

        if (this.shouldDrawCustomGroundShadow(livingEntity))
        {
            this.renderFilmGroundShadow(livingEntity, tickDelta, matrices, FormUtilsClient.getProvider());
        }

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
            .set(FormRenderType.ENTITY, livingEntity.getWrappingEntity(), matrices, state.lightCoords, overlay, animDelta)
            .camera(Minecraft.getInstance().gameRenderer.mainCamera()));

        if (livingEntity.getWrappingEntity().getFireTicks() > 0)
        {
            MorphFireRenderer.render(
                matrices,
                FormUtilsClient.getProvider(),
                livingEntity.getWrappingEntity(),
                livingEntity.getForm(),
                animDelta,
                Minecraft.getInstance().gameRenderer.mainCamera(),
                false
            );
        }

        BBSRendering.restoreWorldRenderState();
        GlStateManager._disableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._disableBlend(0);

        matrices.popPose();

    }

    private boolean shouldDrawCustomGroundShadow(ActorEntity entity)
    {
        return entity.shouldRenderFilmGroundShadow()
            && !BBSRendering.isIrisShadersEnabled()
            && !BBSRendering.isIrisShadowPass();
    }

    private void renderFilmGroundShadow(ActorEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers)
    {
        double x = Mth.lerp(tickDelta, entity.xOld, entity.getX()) + entity.getFilmShadowOffsetX();
        double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ()) + entity.getFilmShadowOffsetZ();

        matrices.pushPose();
        /* X/Z follow the sample point; Y lifts the PNG (entity Y stays at feet to avoid fade). */
        matrices.translate(entity.getFilmShadowOffsetX(), 0F, entity.getFilmShadowOffsetZ());
        ModelBlockEntityRenderer.renderShadow(
            vertexConsumers,
            matrices,
            tickDelta,
            x,
            y,
            z,
            0F,
            entity.getFilmShadowOffsetY(),
            0F,
            entity.getFilmShadowRadiusX(),
            entity.getFilmShadowRadiusZ(),
            entity.getFilmShadowOpacity());
        matrices.popPose();
    }

    @Override
    protected boolean shouldShowName(ActorEntity entity, double squaredDistanceToCamera)
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
        if (!entity.hasPose(Pose.SLEEPING))
        {
            matrices.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        }

        /* Float death_time tip for ModelForm and MobForm (morph.deathTime stays 0). */
        FormDeathTilt.apply(matrices, new MCEntity(entity), entity.getForm(), tickDelta);
    }
}
