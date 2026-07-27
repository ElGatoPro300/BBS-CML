package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.class_308;
import net.minecraft.class_4587;
import net.minecraft.class_4608;
import net.minecraft.class_765;
import net.minecraft.class_7833;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

public class AnchorFormRenderer extends FormRenderer<AnchorForm>
{
    public static final Link ANCHOR_PREVIEW = Link.assets("textures/anchor.png");

    private IEntity entity = new StubEntity();

    public AnchorFormRenderer(AnchorForm form)
    {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        if (this.form.parts.getAll().isEmpty())
        {
            Texture texture = context.render.getTextures().getTexture(ANCHOR_PREVIEW);

            int w = texture.width;
            int h = texture.height;
            int cellW = Math.max(8, x2 - x1 - 8);
            int cellH = Math.max(8, y2 - y1 - 8);
            float scale = Math.min((float) cellW / (float) Math.max(1, w), (float) cellH / (float) Math.max(1, h));
            int dw = Math.max(1, Math.round(w * scale));
            int dh = Math.max(1, Math.round(h * scale));
            int x = (x1 + x2) / 2;
            int y = (y1 + y2) / 2;

            context.batcher.fullTexturedBox(texture, x - dw / 2, y - dh / 2, dw, dh);
        }
        else
        {
            class_4587 stack = context.batcher.getContext().method_51448();
            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            stack.method_22903();

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);
            /* Why? I don't know, because fuck you */
            stack.method_22907(class_7833.field_40715.rotationDegrees(180F));
            MatrixStackUtils.invertUiNormalY(stack);

            Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
            Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
            RenderSystem.setupLevelDiffuseLighting(light0, light1);

            this.renderBodyParts(new FormRenderingContext()
                .set(FormRenderType.ENTITY, this.entity, stack, class_765.method_23687(15, 15), class_4608.field_21444, context.getTransition())
                .inUI());

            class_308.method_24210();

            stack.method_22909();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }
}