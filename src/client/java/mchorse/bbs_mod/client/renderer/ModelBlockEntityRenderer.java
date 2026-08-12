package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.mixin.client.EntityRendererDispatcherInvoker;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import org.lwjgl.opengl.GL11;

public class ModelBlockEntityRenderer implements BlockEntityRenderer<ModelBlockEntity, ModelBlockEntityRenderState>
{
    private static ActorEntity entity;

    public static void renderShadow(MultiBufferSource provider, PoseStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, 0.5F, 0.5F, 1F);
    }

    public static void renderShadow(MultiBufferSource provider, PoseStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radius, float opacity)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, radius, radius, opacity);
    }

    /**
     * Vanilla ground blob. Minecraft only exposes a single radius, so non-uniform size is
     * done by scaling the matrix (same idea as Iris caster scale in {@code BaseFilmController}).
     */
    public static void renderShadow(MultiBufferSource provider, PoseStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radiusX, float radiusZ, float opacity)
    {
        ClientLevel world = Minecraft.getInstance().level;

        if (entity == null || entity.getEntityWorld() != world)
        {
            entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        entity.setPos(x, y, z);
        entity.lastRenderX = x;
        entity.lastRenderY = y;
        entity.lastRenderZ = z;
        entity.lastX = x;
        entity.lastY = y;
        entity.lastZ = z;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double dx = x - camera.position().x;
        double dy = y - camera.position().y;
        double dz = z - camera.position().z;
        double distance = dx * dx + dy * dy + dz * dz;

        opacity = (float) ((1D - distance / 256D) * opacity);

        float baseRadius = 0.5F;
        float scaleX = Math.max(0.001F, radiusX / baseRadius);
        float scaleZ = Math.max(0.001F, radiusZ / baseRadius);

        matrices.pushPose();
        matrices.translate(tx, ty, tz);
        matrices.scale(scaleX, 1F, scaleZ);

        /* EntityRendererDispatcherInvoker.bbs$renderShadow(matrices, provider, entity, opacity, tickDelta, entity.getWorld(), radius); */

        matrices.popPose();
    }

    private static float getHeadYaw(float constraint, float yawDelta, float travel)
    {
        float headLimit = (float) Math.toRadians(constraint);
        float headYawBase = MathUtils.clamp(yawDelta, -headLimit, headLimit);

        float syncStart = (float) Math.toRadians(315D);
        float syncRange = (float) Math.toRadians(45D);
        float t = 0F;

        if (travel >= syncStart)
        {
            t = Math.min(1F, (travel - syncStart) / syncRange);
        }

        return headYawBase * (1F - t);
    }

    public ModelBlockEntityRenderer(BlockEntityRendererProvider.Context ctx)
    {}

    @Override
    public ModelBlockEntityRenderState createRenderState()
    {
        return new ModelBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(ModelBlockEntity entity, ModelBlockEntityRenderState state, float tickDelta, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay)
    {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.entity = entity;
        state.tickDelta = tickDelta;
    }

    @Override
    public boolean shouldRenderOffScreen()
    {
        return true;
    }

    @Override
    public void render(ModelBlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState)
    {
        ModelBlockEntity entity = state.entity;

        if (entity == null)
        {
            return;
        }

        float tickDelta = state.tickDelta;
        MultiBufferSource vertexConsumers = Minecraft.getInstance().renderBuffers().bufferSource();
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;
        Minecraft mc = Minecraft.getInstance();
        ModelProperties properties = entity.getProperties();
        Transform transform = properties.getTransform();
        BlockPos pos = entity.getPos();
        boolean appliedRuntimeOverlay = false;

        matrices.pushPose();
        matrices.translate(0.5F, 0F, 0.5F);

        Form form = UIModelBlockPanel.getLiveEditedForm(entity);

        if (form == null)
        {
            form = properties.getForm();
        }

        if (form != null && canRenderStatic(entity))
        {
            matrices.pushPose();

            Transform applied = transform;

            if (properties.isLookAt())
            {
                applied = applyLookingAnimation(mc, entity, properties, tickDelta);
            }
            else
            {
                IEntity iEntity = entity.getEntity();

                entity.resetLookYaw();
                iEntity.setHeadYaw(0F);
                iEntity.setPrevHeadYaw(0F);
                iEntity.setPitch(0F);
                iEntity.setPrevPitch(0F);
            }

            MatrixStackUtils.applyTransform(matrices, applied);

            int lightAbove = resolveModelBlockLight(entity, properties, transform, light);
            Camera camera = mc.gameRenderer.getMainCamera();

            GL11.glEnable(GL11.GL_DEPTH_TEST);

            FormRenderingContext formContext = new FormRenderingContext()
                .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), matrices, lightAbove, overlay, tickDelta)
                .camera(camera);

            formContext.isShadowPass = BBSRendering.isIrisShadowPass();

            FormUtilsClient.render(form, formContext);

            if (!formContext.isShadowPass)
            {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }

            if (!formContext.isShadowPass && this.canRenderAxes(entity) && UIBaseMenu.renderAxes)
            {
                matrices.pushPose();
                MatrixStackUtils.scaleBack(matrices);
                Draw.coolerAxes(matrices, 0.5F, 0.01F, 0.51F, 0.02F);
                matrices.popPose();
            }

            matrices.popPose();
        }

        if (!BBSRendering.isIrisShadowPass())
        {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        if (mc.getDebugOverlay().showDebugScreen())
        {
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 0.5F, 1F, 0.5F);
        }

        matrices.popPose();

        /* Vanilla ground blob only — Iris mesh shadows come from the form draw above / shadow mixin. */
        if (properties.isShadow() && !BBSRendering.isIrisShadowPass())
        {
            float tx = 0.5F + transform.translate.x;
            float ty = transform.translate.y;
            float tz = 0.5F + transform.translate.z;
            double x = pos.getX() + tx;
            double y = pos.getY() + ty;
            double z = pos.getZ() + tz;

            renderShadow(vertexConsumers, matrices, tickDelta, x, y, z, tx, ty, tz);
        }

        if (appliedRuntimeOverlay && properties.getForm() instanceof ModelForm modelForm)
        {
            modelForm.poseOverlay.setRuntimeValue(null);
        }
    }

    private static Transform applyLookingAnimation(Minecraft mc, ModelBlockEntity entity, ModelProperties properties, float tickDelta)
    {
        Transform transform = properties.getTransform();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 position = !mc.options.getCameraType().isFirstPerson() && mc.player != null
            ? mc.player.getEyePosition(tickDelta)
            : camera.position();

        BlockPos pos = entity.getPos();
        double x = pos.getX() + 0.5D + transform.translate.x;
        double y = pos.getY() + transform.translate.y;
        double z = pos.getZ() + 0.5D + transform.translate.z;

        double dx = position.x - x;
        double dz = position.z - z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        float initialYaw = transform.rotate.y;
        float yaw = (float) Math.atan2(dx, dz);
        float yawContinuous = entity.updateLookYawContinuous(yaw);
        float yawDelta = yawContinuous - initialYaw;
        float travel = Math.abs(yawDelta) % (MathUtils.PI * 2F);

        Transform finalTransform = transform.copy();
        Form form = properties.getForm();
        boolean lookAt = form instanceof MobForm;
        float headHeight = form.hitboxHeight.get() * form.hitboxEyeHeight.get() * finalTransform.scale.y;
        float constraint = 45F;
        boolean isPitching = true;

        if (form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model != null && model.view != null)
            {
                String headKey = model.view.headBone;

                lookAt = true;
                constraint = model.view.constraint;
                isPitching = model.view.pitch;

                if (FormUtilsClient.getBones(modelForm).contains(headKey))
                {
                    MatrixCache matrices = new MatrixCache();

                    model.captureMatrices(matrices);

                    Matrix4f matrix = matrices.get(headKey).matrix();

                    if (matrix != null)
                    {
                        headHeight = matrix.getTranslation(new Vector3f()).y * finalTransform.scale.y;
                    }
                }
            }
        }

        finalTransform.rotate.y = yawContinuous;

        if (lookAt)
        {
            IEntity iEntity = entity.getEntity();
            double deltaHead = position.y - (y + headHeight);
            float pitch = MathUtils.clamp((float) Math.atan2(deltaHead, distance), -MathUtils.PI / 2F, MathUtils.PI / 2F);
            float headYaw = getHeadYaw(constraint, yawDelta, travel);
            float anchorYaw = yawDelta - headYaw;

            if (travel >= (float) Math.toRadians(359D))
            {
                headYaw = 0F;
                anchorYaw = 0F;

                entity.snapLookYawToBase(yaw, initialYaw);
            }

            finalTransform.rotate.y = initialYaw + anchorYaw;
            headYaw = -MathUtils.toDeg(headYaw);
            pitch = -MathUtils.toDeg(isPitching ? pitch : 0F);

            iEntity.setHeadYaw(headYaw);
            iEntity.setPrevHeadYaw(headYaw);
            iEntity.setPitch(pitch);
            iEntity.setPrevPitch(pitch);
        }

        return finalTransform;
    }

    @Override
    public int getViewDistance()
    {
        return 512;
    }

    private boolean canRenderAxes(ModelBlockEntity entity)
    {
        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            /* The block currently being edited gets the real interactive gizmo (drawn by
             * UIModelBlockPanel), so the decorative axes would just overlap it. */
            return dashboard.getPanels().panel instanceof UIModelBlockPanel modelBlockPanel && !modelBlockPanel.isSelectedForGizmo(entity);
        }

        return false;
    }

    /**
     * Draw a model-block form into Iris' shadow map. Block entities are not covered by
     * {@code shadowEntities}; packs that only enable entity shadows still need this path.
     * Safe to call even when Iris also draws block entities — opaque depth writes are idempotent.
     */
    public static void renderIntoShadowMap(ModelBlockEntity entity, PoseStack shadowStack, MultiBufferSource consumers, float tickDelta, double camX, double camY, double camZ)
    {
        if (entity == null || entity.isRemoved() || entity.getWorld() == null)
        {
            return;
        }

        if (!canRenderStatic(entity))
        {
            return;
        }

        ModelProperties properties = entity.getProperties();
        Form form = UIModelBlockPanel.getLiveEditedForm(entity);

        if (form == null)
        {
            form = properties.getForm();
        }

        if (form == null || !form.shaderShadow.get())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Transform transform = properties.getTransform();
        BlockPos pos = entity.getPos();
        Transform applied = transform;

        shadowStack.pushPose();
        shadowStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
        shadowStack.translate(0.5F, 0F, 0.5F);

        if (properties.isLookAt())
        {
            applied = applyLookingAnimation(mc, entity, properties, tickDelta);
        }

        MatrixStackUtils.applyTransform(shadowStack, applied);

        int lightAbove = resolveModelBlockLight(entity, properties, transform, 0xF000F0);
        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), shadowStack, lightAbove, OverlayTexture.NO_OVERLAY, tickDelta)
            .camera(mc.gameRenderer.getMainCamera());

        formContext.isShadowPass = true;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        FormUtilsClient.render(form, formContext);
        shadowStack.popPose();
    }

    private static int resolveModelBlockLight(ModelBlockEntity entity, ModelProperties properties, Transform transform, int fallbackLight)
    {
        if (entity.getWorld() == null)
        {
            return fallbackLight;
        }

        BlockPos pos = entity.getPos();

        if (!properties.isLocalLighting())
        {
            return LevelRenderer.getLightColor(entity.getWorld(), pos);
        }

        return LevelRenderer.getLightColor(entity.getWorld(), pos.offset(
            (int) transform.translate.x,
            (int) transform.translate.y,
            (int) transform.translate.z));
    }

    private static boolean canRenderStatic(ModelBlockEntity entity)
    {
        if (!entity.getProperties().isEnabled())
        {
            return false;
        }

        if (!BBSSettings.renderAllModelBlocks.get())
        {
            return false;
        }

        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            if (dashboard.getPanels().panel instanceof UIModelBlockPanel modelBlockPanel)
            {
                return !modelBlockPanel.isEditing(entity) || UIModelBlockPanel.toggleRendering;
            }
        }

        return true;
    }
}
