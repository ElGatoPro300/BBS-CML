package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

public class ModelBlockEntityRenderer implements BlockEntityRenderer<ModelBlockEntity, ModelBlockEntityRenderer.ModelBlockRenderState>
{
    private static ActorEntity entity;

    public static void renderShadow(VertexConsumerProvider provider, MatrixStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, 0.5F, 1F);
    }

    public static void renderShadow(VertexConsumerProvider provider, MatrixStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radius, float opacity)
    {
        ClientWorld world = MinecraftClient.getInstance().world;

        if (entity == null || entity.getEntityWorld() != world)
        {
            entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        entity.setPos(x, y, z);
        //entity.lastRenderX = x;
        //entity.lastRenderY = y;
        //entity.lastRenderZ = z;
        //entity.prevX = x;
        //entity.prevY = y;
        //entity.prevZ = z;

        //double distance = MinecraftClient.getInstance().getEntityRenderDispatcher().getSquaredDistanceToCamera(new Vec3d(x, y, z));

        //opacity = (float) ((1D - distance / 256D) * opacity);

        matrices.push();
        matrices.translate(tx, ty, tz);

        /* EntityRendererDispatcherInvoker.bbs$renderShadow(matrices, provider, entity, opacity, tickDelta, entity.getWorld(), radius); */

        matrices.pop();
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

    public static void renderIntoShadowMap(ModelBlockEntity modelBlock, MatrixStack matrices, VertexConsumerProvider.Immediate consumers, float tickDelta, double camX, double camY, double camZ)
    {
        if (modelBlock == null)
        {
            return;
        }

        IEntity iEntity = modelBlock.getEntity();
        Form form = modelBlock.getProperties().getForm();

        if (form == null)
        {
            return;
        }

        matrices.push();
        matrices.translate(modelBlock.getPos().getX() - camX, modelBlock.getPos().getY() - camY, modelBlock.getPos().getZ() - camZ);

        FormUtilsClient.render(form, new FormRenderingContext()
            .set(FormRenderType.MODEL_BLOCK, iEntity, matrices, 15 << 20 | 15 << 4, OverlayTexture.DEFAULT_UV, tickDelta));

        matrices.pop();
    }

    public ModelBlockEntityRenderer(BlockEntityRendererFactory.Context ctx)
    {}

    @Override
    public ModelBlockRenderState createRenderState()
    {
        return new ModelBlockRenderState();
    }

    @Override
    public void updateRenderState(ModelBlockEntity blockEntity, ModelBlockRenderState state, float tickDelta, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay)
    {
        BlockEntityRenderer.super.updateRenderState(blockEntity, state, tickDelta, cameraPos, crumblingOverlay);

        state.entity = blockEntity;
        state.tickDelta = tickDelta;
    }

    @Override
    public boolean rendersOutsideBoundingBox()
    {
        return true;
    }

    @Override
    public void render(ModelBlockRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState)
    {
        ModelBlockEntity entity = state.entity;

        if (entity == null)
        {
            return;
        }

        float tickDelta = state.tickDelta;
        int overlay = OverlayTexture.DEFAULT_UV;
        MinecraftClient mc = MinecraftClient.getInstance();
        ModelProperties properties = entity.getProperties();
        Transform transform = properties.getTransform();
        BlockPos pos = entity.getPos();

        matrices.push();
        matrices.translate(0.5F, 0F, 0.5F);

        Form form = UIModelBlockPanel.getLiveEditedForm(entity);

        if (form == null)
        {
            form = properties.getForm();
        }

        if (form != null && canRenderStatic(entity))
        {
            matrices.push();

            Transform applied = transform;

            if (properties.isLookAt())
            {
                applied = this.applyLookingAnimation(mc, entity, properties, tickDelta);
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

            int lightAbove = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos.add((int) transform.translate.x, (int) transform.translate.y, (int) transform.translate.z));
            Camera camera = mc.gameRenderer.getCamera();

            FormUtilsClient.render(properties.getForm(), new FormRenderingContext()
                .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), matrices, lightAbove, overlay, tickDelta)
                .camera(camera));

            formContext.isShadowPass = BBSRendering.isIrisShadowPass();

            FormUtilsClient.render(form, formContext);

            if (!formContext.isShadowPass)
            {
                RenderSystem.disableDepthTest();
            }

            if (!formContext.isShadowPass && this.canRenderAxes(entity) && UIBaseMenu.renderAxes)
            {
                matrices.push();
                MatrixStackUtils.scaleBack(matrices);
                Draw.coolerAxes(matrices, 0.5F, 0.01F, 0.51F, 0.02F);
                matrices.pop();
            }

            matrices.pop();
        }

        if (mc.getDebugHud().shouldShowDebugHud())
        {
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 0.5F, 1F, 0.5F);
        }

        matrices.pop();

        if (properties.isShadow())
        {
            float tx = 0.5F + transform.translate.x;
            float ty = transform.translate.y;
            float tz = 0.5F + transform.translate.z;
            double x = pos.getX() + tx;
            double y = pos.getY() + ty;
            double z = pos.getZ() + tz;

            renderShadow(null, matrices, tickDelta, x, y, z, tx, ty, tz);
        }
    }

    private Transform applyLookingAnimation(MinecraftClient mc, ModelBlockEntity entity, ModelProperties properties, float tickDelta)
    {
        Transform transform = properties.getTransform();
        Camera camera = mc.gameRenderer.getCamera();
        //Vec3d position = !mc.options.getPerspective().isFirstPerson() && mc.player != null
        //    ? mc.player.getCameraPosVec(tickDelta)
        //    : camera.getPos();
        Vec3d position = new Vec3d(0D, 0D, 0D);

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
    public int getRenderDistance()
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
    public static void renderIntoShadowMap(ModelBlockEntity entity, MatrixStack shadowStack, VertexConsumerProvider consumers, float tickDelta, double camX, double camY, double camZ)
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

        MinecraftClient mc = MinecraftClient.getInstance();
        Transform transform = properties.getTransform();
        BlockPos pos = entity.getPos();
        Transform applied = transform;

        shadowStack.push();
        shadowStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
        shadowStack.translate(0.5F, 0F, 0.5F);

        if (properties.isLookAt())
        {
            applied = applyLookingAnimation(mc, entity, properties, tickDelta);
        }

        MatrixStackUtils.applyTransform(shadowStack, applied);

        int lightAbove = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos.add((int) transform.translate.x, (int) transform.translate.y, (int) transform.translate.z));
        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), shadowStack, lightAbove, OverlayTexture.DEFAULT_UV, tickDelta)
            .camera(mc.gameRenderer.getCamera());

        formContext.isShadowPass = true;

        RenderSystem.enableDepthTest();
        FormUtilsClient.render(form, formContext);
        shadowStack.pop();
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

    public static class ModelBlockRenderState extends BlockEntityRenderState
    {
        public ModelBlockEntity entity;
        public float tickDelta;
    }
}
