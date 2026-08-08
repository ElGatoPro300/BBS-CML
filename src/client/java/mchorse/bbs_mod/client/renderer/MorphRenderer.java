package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.selectors.ISelectorOwnerProvider;
import mchorse.bbs_mod.selectors.SelectorOwner;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

public class MorphRenderer
{
    public static boolean hidePlayer = false;

    public static boolean renderPlayer(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i)
    {
        Morph morph = Morph.getMorph(player);
        Form playerForm = morph != null ? morph.getForm() : null;

        UIBaseMenu menu = UIScreen.getCurrentMenu();
        if (menu instanceof UIDashboard dashboard)
        {
            UIDashboardPanel panel = dashboard.getPanels().panel;

            if (panel instanceof UIMorphingPanel morphingPanel && morphingPanel.palette.editor.isEditing())
            {
                Form editingForm = morphingPanel.palette.editor.form;

                if (!areFormsEquivalent(editingForm, playerForm))
                {
                    return true;
                }
            }
        }

        if (hidePlayer)
        {
            if (FormUtilsClient.getCurrentForm() instanceof MobForm form && !form.isPlayer())
            {
                return true;
            }
        }

        if (morph != null && morph.getForm() != null)
        {
            if (canRender(playerForm))
            {
                /* 1.21.11: GlStateManager._enableDepthTest() removed */
                // GlStateManager._enableDepthTest();

                Vector3f a = new Vector3f(0.85F, 0.85F, -1F).normalize();
                Vector3f b = new Vector3f(-0.85F, 0.85F, 1F).normalize();
                /* 1.21.11: RenderSystem.setupLevelDiffuseLighting() removed */
                // RenderSystem.setupLevelDiffuseLighting(a, b);

                float bodyYaw = /* 1.21.11: prevBodyYaw removed */ player.bodyYaw;
                int overlay = OverlayTexture.DEFAULT_UV;

                matrixStack.push();
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

                FormUtilsClient.render(morph.getForm(), new FormRenderingContext()
                    .set(FormRenderType.ENTITY, morph.entity, matrixStack, i, overlay, g)
                    .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

                if (morph.entity.getFireTicks() > 0)
                {
                    MorphFireRenderer.render(
                        matrixStack,
                        vertexConsumerProvider,
                        morph.entity,
                        morph.getForm(),
                        g,
                        MinecraftClient.getInstance().gameRenderer.getCamera(),
                        false
                    );
                }

                matrixStack.pop();

                /* 1.21.11: GlStateManager._disableDepthTest() removed */
                // GlStateManager._disableDepthTest();
            }

            return true;
        }

        return false;
    }

    private static boolean canRender(Form playerForm)
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();
        
        if (menu instanceof UIDashboard dashboard)
        {
            UIDashboardPanel panel = dashboard.getPanels().panel;

            if (panel instanceof UIMorphingPanel morphingPanel && morphingPanel.palette.editor.isEditing())
            {
                return areFormsEquivalent(morphingPanel.palette.editor.form, playerForm);
            }
        }

        return true;
    }

    private static boolean areFormsEquivalent(Form a, Form b)
    {
        if (a == b) return true;
        if (a == null || b == null) return false;

        MapType dataA = FormUtils.toData(a);
        MapType dataB = FormUtils.toData(b);

        return dataA != null && dataA.equals(dataB);
    }

    /* 1.21.11 deferred collection API — called from LivingEntityRendererMorphMixin at render HEAD */
    private static final List<Queued> QUEUE = new ArrayList<>();

    public static boolean collectPlayer(AbstractClientPlayerEntity player, int light, int overlay, float tickDelta)
    {
        if (hidePlayer)
        {
            if (FormUtilsClient.getCurrentForm() instanceof MobForm form && !form.isPlayer())
            {
                return true;
            }
        }

        Morph morph = Morph.getMorph(player);

        if (morph != null && morph.getForm() != null)
        {
            if (canRender(morph.getForm()))
            {
                QUEUE.add(new Queued(morph.getForm(), morph.entity, light, overlay, tickDelta));
            }

            return true;
        }

        return false;
    }

    public static boolean collectLivingEntity(LivingEntity livingEntity, int light, int overlay, float tickDelta)
    {
        if (!(livingEntity instanceof ISelectorOwnerProvider))
        {
            return false;
        }

        SelectorOwner owner = ((ISelectorOwnerProvider) livingEntity).getOwner();

        owner.check();

        Form form = owner.getForm();

        if (form != null)
        {
            QUEUE.add(new Queued(form, owner.entity, light, overlay, tickDelta));

            return true;
        }

        return false;
    }

    public static void renderQueued(WorldRenderContext context)
    {
        if (QUEUE.isEmpty())
        {
            return;
        }

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        double cx = camera.getCameraPos().x;
        double cy = camera.getCameraPos().y;
        double cz = camera.getCameraPos().z;
        MatrixStack stack = context.matrices();

        for (Queued queued : QUEUE)
        {
            Matrix4f target = BaseFilmController.getMatrixForRenderWithRotation(queued.entity, cx, cy, cz, queued.tickDelta);

            stack.push();

            try
            {
                MatrixStackUtils.multiply(stack, target);

                FormUtilsClient.render(queued.form, new FormRenderingContext()
                    .set(FormRenderType.ENTITY, queued.entity, stack, queued.light, queued.overlay, queued.tickDelta)
                    .camera(camera));
            }
            finally
            {
                stack.pop();
            }
        }

        QUEUE.clear();
    }

    private static class Queued
    {
        public Form form;
        public IEntity entity;
        public int light;
        public int overlay;
        public float tickDelta;

        Queued(Form form, IEntity entity, int light, int overlay, float tickDelta)
        {
            this.form = form;
            this.entity = entity;
            this.light = light;
            this.overlay = overlay;
            this.tickDelta = tickDelta;
        }
    }

    public static boolean renderLivingEntity(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int o)
    {
        if (!(livingEntity instanceof ISelectorOwnerProvider))
        {
            return false;
        }

        SelectorOwner owner = ((ISelectorOwnerProvider) livingEntity).getOwner();

        owner.check();

        Form form = owner.getForm();

        if (form != null)
        {
            /* 1.21.11: GlStateManager._enableDepthTest() removed */
            // GlStateManager._enableDepthTest();

            float bodyYaw = /* 1.21.11: prevBodyYaw removed */ livingEntity.bodyYaw;

            matrixStack.push();
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

            FormUtilsClient.render(form, new FormRenderingContext()
                .set(FormRenderType.ENTITY, owner.entity, matrixStack, i, o, g)
                .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

            if (owner.entity.getFireTicks() > 0)
            {
                MorphFireRenderer.render(
                    matrixStack,
                    vertexConsumerProvider,
                    owner.entity,
                    form,
                    g,
                    MinecraftClient.getInstance().gameRenderer.getCamera(),
                    false
                );
            }

            matrixStack.pop();

            /* 1.21.11: GlStateManager._disableDepthTest() removed */
            // GlStateManager._disableDepthTest();

            return true;
        }

        return false;
    }
}
