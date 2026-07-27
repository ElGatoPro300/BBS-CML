package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.FormRenderDepth;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.class_1268;
import net.minecraft.class_284;
import net.minecraft.class_4587;
import net.minecraft.class_5944;
import net.minecraft.class_742;
import net.minecraft.class_765;
import org.joml.Matrix4f;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class FormRenderer <T extends Form>
{
    private static boolean suppressFormDisplayName;

    public static void setSuppressFormDisplayName(boolean suppress)
    {
        suppressFormDisplayName = suppress;
    }

    protected T form;

    public FormRenderer(T form)
    {
        this.form = form;
    }

    public T getForm()
    {
        return this.form;
    }

    public List<String> getBones()
    {
        return Collections.emptyList();
    }

    public final void renderUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        this.renderInUI(context, x1, y1, x2, y2);

        context.batcher.flush();
        /* Glow/paint overlays leave additive blend; text drawn in that state looks
         * doubled and washed white. Restore before any Batcher2D labels. */
        BBSRendering.restoreGuiRenderState();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        FontRenderer font = context.batcher.getFont();
        String name = this.form.name.get();

        if (!suppressFormDisplayName && !name.isEmpty())
        {
            name = font.limitToWidth(name, x2 - x1 - 3);

            int w = font.getWidth(name);

            context.batcher.textCard(name, (x2 + x1 - w) / 2, y1 + 6, Colors.WHITE, Colors.ACTIVE | Colors.A50);
        }

        int keybind = this.form.hotkey.get();

        if (keybind > 0)
        {
            name = KeyCodes.getName(keybind);
            name = font.limitToWidth(name, x2 - x1 - 3);

            int w = font.getWidth(name);

            context.batcher.textCard(name, (x2 + x1 - w) / 2, y2 - 6 - font.getHeight(), Colors.WHITE, Colors.A50);
        }
    }

    protected abstract void renderInUI(UIContext context, int x1, int y1, int x2, int y2);

    public boolean renderArm(class_4587 matrices, int light, class_742 player, class_1268 hand)
    {
        return false;
    }

    public final void render(FormRenderingContext context)
    {
        /* Transparent forms skip casting via opacity / vertex alpha in the shadow path.
         * Color-track paint/blend/grade must not disable Form.shaderShadow. */
        if (!this.form.shaderShadow.get() && BBSRendering.isIrisShadowPass())
        {
            return;
        }

        if (!this.form.render.get())
        {
            return;
        }

        this.form.applyStates(context.transition);

        if (!this.form.visible.get())
        {
            this.form.unapplyStates();

            return;
        }

        int light = context.light;
        int savedColor = context.color;
        boolean isPicking = context.stencilMap != null;

        if (!isPicking && context.renderDepthFrame != null && context.type == FormRenderType.ENTITY && context.entity != null)
        {
            Form sourceForm = FormRenderDepth.getSourceForm(context.renderDepthFrame.sourceRootForm, this.form);
            double distanceSq = FormRenderDepth.getEntityDistanceSq(context.entity, context.camera, context.getTransition());
            float renderDepthFade = FormRenderDepth.getFade(this.form, sourceForm, distanceSq, context.renderDepthFrame.occluders);

            if (renderDepthFade <= 0F)
            {
                this.form.unapplyStates();

                return;
            }

            if (renderDepthFade < 1F)
            {
                int alpha = Math.round(((savedColor >>> 24) & 0xFF) * renderDepthFade);

                context.color = (alpha << 24) | (savedColor & Colors.RGB);
            }
        }

        context.stack.method_22903();
        if (context.world != null)
        {
            context.world.method_22903();
        }

        try
        {
            this.applyTransforms(context.stack, false, context.getTransition());
            if (context.world != null)
            {
                this.applyTransforms(context.world, false, context.getTransition());
            }

            float lf = 1F - MathUtils.clamp(this.form.lighting.get(), 0F, 1F);
            int u = context.light & '\uffff';
            int v = context.light >> 16 & '\uffff';

            u = (int) Lerps.lerp(u, class_765.field_32769, lf);
            context.light = u | v << 16;

            this.render3D(context);

            if (isPicking)
            {
                this.updateStencilMap(context);
            }

            this.renderBodyParts(context);
        }
        finally
        {
            context.stack.method_22909();
            if (context.world != null)
            {
                context.world.method_22909();
            }

            context.light = light;
            context.color = savedColor;

            this.form.unapplyStates();
        }
    }

    protected void applyTransforms(class_4587 stack, boolean origin, float transition)
    {
        Transform transform = this.createTransform();

        if (origin)
        {
            /* Gizmo preview must sit at the rotation/scale center (translate + pivot),
             * not only at translate — otherwise editing pivot XYZ leaves the gizmo behind. */
            stack.method_46416(transform.translate.x, transform.translate.y, transform.translate.z);

            if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
            {
                stack.method_46416(transform.pivot.x, transform.pivot.y, transform.pivot.z);
            }
        }
        else
        {
            MatrixStackUtils.applyTransform(stack, transform);
        }
    }

    protected void applyTransforms(Matrix4f matrix, float transition)
    {
        matrix.mul(this.createTransform().createMatrix());
    }

    protected Transform createTransform()
    {
        Transform transform = new Transform();

        transform.copy(this.form.transform.get());
        this.applyTransform(transform, this.form.transformOverlay.get());

        for (ValueTransform t : this.form.additionalTransforms)
        {
            this.applyTransform(transform, t.get());
        }

        return transform;
    }

    private void applyTransform(Transform transform, Transform overlay)
    {
        transform.translate.add(overlay.translate);
        transform.scale.add(overlay.scale).sub(1, 1, 1);
        transform.rotate.add(overlay.rotate);
        transform.rotate2.add(overlay.rotate2);
        transform.pivot.add(overlay.pivot);
    }

    protected Supplier<class_5944> getShader(FormRenderingContext context, Supplier<class_5944> normal, Supplier<class_5944> picking)
    {
        if (context.isPicking())
        {
            class_5944 program = picking.get();

            if (program == null)
            {
                return normal;
            }

            this.setupTarget(context, program);

            return () -> program;
        }

        return normal;
    }

    protected void setupTarget(FormRenderingContext context, class_5944 program)
    {
        if (program == null)
        {
            return;
        }

        class_284 target = program.method_34582("Target");

        if (target != null)
        {
            target.method_35649(context.getPickingIndex());
        }
    }

    protected void updateStencilMap(FormRenderingContext context)
    {
        context.stencilMap.addPicking(this.form);
    }

    protected void render3D(FormRenderingContext context)
    {}

    public void renderBodyParts(FormRenderingContext context)
    {
        if (this.form.parts.getAllTyped().isEmpty())
        {
            return;
        }

        FormRenderDepth.Frame savedFrame = context.renderDepthFrame;

        if (!FormRenderDepth.BODY_PART_RENDER_DEPTH)
        {
            context.renderDepthFrame = null;
        }

        try
        {
            List<BodyPart> parts = this.getSortedBodyParts(context);

            if (ItemBodyPartBatch.renderBodyParts(this, parts, context))
            {
                return;
            }

            for (BodyPart part : parts)
            {
                this.renderBodyPart(part, context);
            }
        }
        finally
        {
            context.renderDepthFrame = savedFrame;
        }
    }

    protected List<BodyPart> getSortedBodyParts(FormRenderingContext context)
    {
        List<BodyPart> parts = new ArrayList<>(this.form.parts.getAllTyped());

        if (!FormRenderDepth.BODY_PART_RENDER_DEPTH || context.renderDepthFrame == null)
        {
            return parts;
        }

        Form sourceRoot = context.renderDepthFrame.sourceRootForm;

        parts.sort(Comparator.comparingDouble(part ->
        {
            Form child = part.getForm();

            if (child == null)
            {
                return 0D;
            }

            Double depth = FormRenderDepth.getEnabledDepth(child, FormRenderDepth.getSourceForm(sourceRoot, child));

            return depth == null ? 0D : depth;
        }));

        return parts;
    }

    protected void renderBodyPart(BodyPart part, FormRenderingContext context)
    {
        IEntity oldEntity = context.entity;

        context.entity = part.useTarget.get() ? oldEntity : part.getEntity();

        if (part.getForm() != null)
        {
            context.stack.method_22903();

            if (context.world != null)
            {
                context.world.method_22903();
            }

            try
            {
                MatrixStackUtils.applyTransform(context.stack, part.transform.get());

                if (context.world != null)
                {
                    MatrixStackUtils.applyTransform(context.world, part.transform.get());
                }

                FormUtilsClient.render(part.getForm(), context);
            }
            finally
            {
                context.stack.method_22909();

                if (context.world != null)
                {
                    context.world.method_22909();
                }
            }
        }

        context.entity = oldEntity;
    }

    public MatrixCache collectMatrices(IEntity entity, float transition)
    {
        MatrixCache map = new MatrixCache();
        class_4587 stack = new class_4587();

        this.collectMatrices(entity, stack, map, "", transition);

        return map;
    }

    public void collectMatrices(IEntity entity, class_4587 stack, MatrixCache matrices, String prefix, float transition)
    {
        Matrix4f mm = new Matrix4f();
        Matrix4f oo = new Matrix4f();

        stack.method_22903();
        this.applyTransforms(stack, true, transition);
        oo.set(stack.method_23760().method_23761());
        stack.method_22909();

        stack.method_22903();
        this.applyTransforms(stack, false, transition);
        mm.set(stack.method_23760().method_23761());

        matrices.put(prefix, mm, oo);

        int i = 0;

        for (BodyPart part : this.form.parts.getAllTyped())
        {
            Form form = part.getForm();

            if (form != null)
            {
                stack.method_22903();
                MatrixStackUtils.applyTransform(stack, part.transform.get());

                FormUtilsClient.getRenderer(form).collectMatrices(entity, stack, matrices, StringUtils.combinePaths(prefix, String.valueOf(i)), transition);

                stack.method_22909();
            }

            i += 1;
        }

        stack.method_22909();
    }
}
