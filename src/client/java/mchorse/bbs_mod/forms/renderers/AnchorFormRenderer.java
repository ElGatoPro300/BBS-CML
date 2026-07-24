package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

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
            MatrixStack stack = context.batcher.getContext().getMatrices();
            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            stack.push();

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);
            /* Why? I don't know, because fuck you */
            stack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180F));
            MatrixStackUtils.invertUiNormalY(stack);

            Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
            Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
            RenderSystem.setupLevelDiffuseLighting(light0, light1);

            this.renderBodyParts(new FormRenderingContext()
                .set(FormRenderType.ENTITY, this.entity, stack, LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
                .inUI());

            DiffuseLighting.disableGuiDepthLighting();

            stack.pop();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }
}