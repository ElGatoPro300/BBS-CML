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
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5614;
import net.minecraft.class_638;
import net.minecraft.class_761;
import net.minecraft.class_827;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

public class ModelBlockEntityRenderer implements class_827<ModelBlockEntity>
{
    private static ActorEntity entity;

    public static void renderShadow(class_4597 provider, class_4587 matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, 0.5F, 0.5F, 1F);
    }

    public static void renderShadow(class_4597 provider, class_4587 matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radius, float opacity)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, radius, radius, opacity);
    }

    /**
     * Vanilla ground blob. Minecraft only exposes a single radius, so non-uniform size is
     * done by scaling the matrix (same idea as Iris caster scale in {@code BaseFilmController}).
     */
    public static void renderShadow(class_4597 provider, class_4587 matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radiusX, float radiusZ, float opacity)
    {
        class_638 world = class_310.method_1551().field_1687;

        if (entity == null || entity.method_37908() != world)
        {
            entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        entity.method_23327(x, y, z);
        entity.field_6038 = x;
        entity.field_5971 = y;
        entity.field_5989 = z;
        entity.field_6014 = x;
        entity.field_6036 = y;
        entity.field_5969 = z;

        double distance = class_310.method_1551().method_1561().method_3959(x, y, z);

        opacity = (float) ((1D - distance / 256D) * opacity);

        float baseRadius = 0.5F;
        float scaleX = Math.max(0.001F, radiusX / baseRadius);
        float scaleZ = Math.max(0.001F, radiusZ / baseRadius);

        matrices.method_22903();
        matrices.method_46416(tx, ty, tz);
        matrices.method_22905(scaleX, 1F, scaleZ);

        EntityRendererDispatcherInvoker.bbs$renderShadow(matrices, provider, entity, opacity, tickDelta, entity.method_37908(), baseRadius);

        matrices.method_22909();
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

    public ModelBlockEntityRenderer(class_5614.class_5615 ctx)
    {}

    @Override
    public boolean rendersOutsideBoundingBox(ModelBlockEntity blockEntity)
    {
        return blockEntity.getProperties().isGlobal();
    }

    @Override
    public void render(ModelBlockEntity entity, float tickDelta, class_4587 matrices, class_4597 vertexConsumers, int light, int overlay)
    {
        class_310 mc = class_310.method_1551();
        ModelProperties properties = entity.getProperties();
        Transform transform = properties.getTransform();
        class_2338 pos = entity.method_11016();
        boolean appliedRuntimeOverlay = false;

        matrices.method_22903();
        matrices.method_46416(0.5F, 0F, 0.5F);

        if (properties.getForm() != null && canRenderStatic(entity))
        {
            matrices.method_22903();

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

            int lightAbove = class_761.method_23794(entity.method_10997(), pos.method_10069((int) transform.translate.x, (int) transform.translate.y, (int) transform.translate.z));
            class_4184 camera = mc.field_1773.method_19418();

            RenderSystem.enableDepthTest();

            FormRenderingContext formContext = new FormRenderingContext()
                .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), matrices, lightAbove, overlay, tickDelta)
                .camera(camera);

            formContext.isShadowPass = BBSRendering.isIrisShadowPass();

            FormUtilsClient.render(properties.getForm(), formContext);

            if (!formContext.isShadowPass)
            {
                RenderSystem.disableDepthTest();
            }

            if (!formContext.isShadowPass && this.canRenderAxes(entity) && UIBaseMenu.renderAxes)
            {
                matrices.method_22903();
                MatrixStackUtils.scaleBack(matrices);
                Draw.coolerAxes(matrices, 0.5F, 0.01F, 0.51F, 0.02F);
                matrices.method_22909();
            }

            matrices.method_22909();
        }

        if (!BBSRendering.isIrisShadowPass())
        {
            RenderSystem.disableDepthTest();
        }

        if (mc.method_53526().method_53536())
        {
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 0.5F, 1F, 0.5F);
        }

        matrices.method_22909();

        /* Vanilla ground blob only — Iris mesh shadows come from the form draw above / shadow mixin. */
        if (properties.isShadow() && !BBSRendering.isIrisShadowPass())
        {
            float tx = 0.5F + transform.translate.x;
            float ty = transform.translate.y;
            float tz = 0.5F + transform.translate.z;
            double x = pos.method_10263() + tx;
            double y = pos.method_10264() + ty;
            double z = pos.method_10260() + tz;

            renderShadow(vertexConsumers, matrices, tickDelta, x, y, z, tx, ty, tz);
        }

        if (appliedRuntimeOverlay && properties.getForm() instanceof ModelForm modelForm)
        {
            modelForm.poseOverlay.setRuntimeValue(null);
        }
    }

    private static Transform applyLookingAnimation(class_310 mc, ModelBlockEntity entity, ModelProperties properties, float tickDelta)
    {
        Transform transform = properties.getTransform();
        class_4184 camera = mc.field_1773.method_19418();
        class_243 position = !mc.field_1690.method_31044().method_31034() && mc.field_1724 != null
            ? mc.field_1724.method_5836(tickDelta)
            : camera.method_19326();

        class_2338 pos = entity.method_11016();
        double x = pos.method_10263() + 0.5D + transform.translate.x;
        double y = pos.method_10264() + transform.translate.y;
        double z = pos.method_10260() + 0.5D + transform.translate.z;

        double dx = position.field_1352 - x;
        double dz = position.field_1350 - z;
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
            double deltaHead = position.field_1351 - (y + headHeight);
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
    public int method_33893()
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
    public static void renderIntoShadowMap(ModelBlockEntity entity, class_4587 shadowStack, class_4597 consumers, float tickDelta, double camX, double camY, double camZ)
    {
        if (entity == null || entity.method_11015() || entity.method_10997() == null)
        {
            return;
        }

        if (!canRenderStatic(entity))
        {
            return;
        }

        ModelProperties properties = entity.getProperties();
        Form form = properties.getForm();

        if (form == null || !form.shaderShadow.get())
        {
            return;
        }

        class_310 mc = class_310.method_1551();
        Transform transform = properties.getTransform();
        class_2338 pos = entity.method_11016();
        Transform applied = transform;

        shadowStack.method_22903();
        shadowStack.method_22904(pos.method_10263() - camX, pos.method_10264() - camY, pos.method_10260() - camZ);
        shadowStack.method_46416(0.5F, 0F, 0.5F);

        if (properties.isLookAt())
        {
            applied = applyLookingAnimation(mc, entity, properties, tickDelta);
        }

        MatrixStackUtils.applyTransform(shadowStack, applied);

        int lightAbove = class_761.method_23794(entity.method_10997(), pos.method_10069((int) transform.translate.x, (int) transform.translate.y, (int) transform.translate.z));
        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), shadowStack, lightAbove, class_4608.field_21444, tickDelta)
            .camera(mc.field_1773.method_19418());

        formContext.isShadowPass = true;

        RenderSystem.enableDepthTest();
        FormUtilsClient.render(form, formContext);
        shadowStack.method_22909();
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
