package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoController;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoRayFrame;
import mchorse.bbs_mod.ui.utils.gizmo.GizmoSurface;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class UIPickableFormRenderer extends UIFormRenderer implements GizmoSurface
{
    public UIFormEditor formEditor;

    private boolean update;

    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();
    private final Matrix4f lastGizmoMatrix = new Matrix4f();
    private boolean hasGizmoMatrix;

    private final GizmoController gizmoController = new GizmoController(this);

    private IEntity target;
    private Supplier<Boolean> renderForm;

    public UIPickableFormRenderer(UIFormEditor formEditor)
    {
        this.formEditor = formEditor;
    }

    public void updatable()
    {
        this.update = true;
    }

    public StencilFormFramebuffer getStencil()
    {
        return this.stencil;
    }

    @Override
    public StencilFormFramebuffer getGizmoStencil()
    {
        return this.stencil;
    }

    public GizmoController getGizmoController()
    {
        return this.gizmoController;
    }

    public void setRenderForm(Supplier<Boolean> renderForm)
    {
        this.renderForm = renderForm;
    }

    private boolean isPreviewVisible()
    {
        return this.renderForm == null || this.renderForm.get();
    }

    private void clearGizmoPickState()
    {
        this.stencil.clearPicking();
        this.gizmoController.updateHover();
        this.hasGizmoMatrix = false;
        Gizmo.INSTANCE.setHoveredIndex(-1);
    }

    public IEntity getTargetEntity()
    {
        return this.target == null ? this.entity : this.target;
    }

    public void setTarget(IEntity target)
    {
        this.target = target;
    }

    private void ensureFramebuffer()
    {
        this.stencil.setup(Link.bbs("stencil_form"));
        this.stencil.resizeGUI(this.area.w, this.area.h);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.ensureFramebuffer();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.formEditor.modelSettingsEditor != null && this.formEditor.modelSettingsEditor.isVisible())
        {
            return false;
        }

        if (this.formEditor.clickViewport(context, this.stencil))
        {
            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.form == null)
        {
            return;
        }

        if (!this.isPreviewVisible())
        {
            this.clearGizmoPickState();

            return;
        }

        this.formEditor.preFormRender(context, this.form);

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.target == null ? this.entity : this.target, new MatrixStack(), LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
            .camera(this.camera)
            .modelRenderer()
            .equipment(false);

        FormUtilsClient.render(this.form, formContext);

        if (this.form.hitbox.get())
        {
            this.renderFormHitbox(context);
        }

        if (this.area.w > 0 && this.area.h > 0)
        {
            if (this.stencil.getFramebuffer() == null)
            {
                this.ensureFramebuffer();
            }
            else
            {
                this.stencil.resizeGUI(this.area.w, this.area.h);
            }

            Texture fboTexture = this.stencil.getFramebuffer().getMainTexture();
            int fboW = fboTexture.width;
            int fboH = fboTexture.height;

            GlStateManager._disableScissorTest();

            this.stencilMap.setup();
            this.stencil.apply();

            this.beginStencilViewport(fboW, fboH);
            this.setupViewport(context);

            FormUtilsClient.render(this.form, formContext.stencilMap(this.stencilMap));

            Matrix4f matrix = this.formEditor.getOrigin(context.getTransition());
            MatrixStack stack = new MatrixStack();

            stack.push();

            if (matrix != null)
            {
                MatrixStackUtils.multiply(stack, matrix);
            }

            Gizmo.INSTANCE.renderStencil(new MatrixStack(), this.stencilMap);
            GlStateManager._disableCull();
            Gizmo.INSTANCE.renderStencil(stack, this.stencilMap);
            GlStateManager._enableCull();

            stack.pop();

            if (this.area.isInside(context))
            {
                this.stencil.pickGUI(context, this.area);
            }
            else
            {
                this.stencil.clearPicking();
            }

            this.stencil.unbind(this.stencilMap);
            this.gizmoController.updateHover();

            this.endStencilViewport();

            /* 1.21.11: Framebuffer.beginWrite() was removed */

            GlStateManager._enableScissorTest();
        }

        this.setupViewport(context);
        this.prepareGizmoRenderState();
        this.renderAxes(context);
    }

    private void prepareGizmoRenderState()
    {
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._disableBlend();
        GlStateManager._disableCull();
    }

    private void renderAxes(UIContext context)
    {
        Matrix4f matrix = this.formEditor.getOrigin(context.getTransition());
        MatrixStack stack = new MatrixStack();

        stack.push();

        if (matrix != null)
        {
            this.lastGizmoMatrix.set(matrix);
            MatrixStackUtils.multiply(stack, matrix);
        }
        else
        {
            this.lastGizmoMatrix.identity();
        }

        /* Draw axes */
        if (UIBaseMenu.renderAxes)
        {
            GlStateManager._disableDepthTest();
            Gizmo.INSTANCE.render(stack);
            GlStateManager._enableDepthTest();
            GlStateManager._disableCull();
            GlStateManager._disableDepthTest();
            Gizmo.INSTANCE.render(stack);
            GlStateManager._enableDepthTest();
            GlStateManager._enableCull();
        }

        stack.pop();
    }

    @Override
    public void prepareGizmoDrag(UIPropTransform transform)
    {
        if (transform == null)
        {
            return;
        }

        transform.setGizmoRayProvider(GizmoRayFrame.fromCamera(
            this.camera,
            this.area,
            () -> this.hasGizmoMatrix ? this.lastGizmoMatrix : null
        ));
    }

    private void renderFormHitbox(UIContext context)
    {
        float hitboxW = this.form.hitboxWidth.get();
        float hitboxH = this.form.hitboxHeight.get();
        float eyeHeight = hitboxH * this.form.hitboxEyeHeight.get();

        /* Draw look vector */
        final float thickness = 0.01F;
        Draw.renderBox(new MatrixStack(), -thickness, -thickness + eyeHeight, -thickness, thickness, thickness, 2F, 1F, 0F, 0F);

        /* Draw hitbox */
        Draw.renderBox(new MatrixStack(), -hitboxW / 2, 0, -hitboxW / 2, hitboxW, hitboxH, hitboxW);
    }

    @Override
    protected void update()
    {
        super.update();

        if (this.update && this.target != null)
        {
            this.form.update(this.entity);

            FormRenderer renderer = FormUtilsClient.getRenderer(this.form);

            if (renderer instanceof ITickable tickable)
            {
                tickable.tick(this.entity);
            }
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.isPreviewVisible() || !this.stencil.hasPicked())
        {
            return;
        }

        int index = this.stencil.getIndex();
        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        Pair<Form, String> pair = this.stencil.getPicked();
        int w = texture.width;
        int h = texture.height;

        /* TODO(1.21.11): RenderPipeline replaces ShaderProgram — uniform access removed
        ShaderProgram previewProgram = BBSShaders.getPickerPreviewProgram();
        GlUniform target = previewProgram.getUniform("Target");

        if (target != null)
        {
            // target.set(index);
        }
        */

        GlStateManager._enableBlend();
        context.batcher.texturedBox(texture.id, Colors.WHITE, this.area.x, this.area.y, this.area.w, this.area.h, 0, h, w, 0, w, h);

        if (pair != null && pair.a != null)
        {
            String label = pair.a.getFormIdOrName();

            if (!pair.b.isEmpty())
            {
                label += " - " + pair.b;
            }

            context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
        }
    }

    @Override
    protected void renderGrid(UIContext context)
    {
        if (this.isPreviewVisible())
        {
            super.renderGrid(context);
        }
    }
}
