package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
            int x = (x1 + x2) / 2;
            int y = (y1 + y2) / 2;

            context.batcher.fullTexturedBox(texture, x - w / 2, y - h / 2, w, h);
        }
        else
        {
            PoseStack stack = new PoseStack();
            Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

            GlStateManager._depthFunc(GL11.GL_LEQUAL);
            stack.pushPose();

            this.applyTransforms(uiMatrix, context.getTransition());
            MatrixStackUtils.multiply(stack, uiMatrix);
            /* Why? I don't know, because fuck you */
            stack.mulPose(Axis.YN.rotationDegrees(180F));
            MatrixStackUtils.invertUiNormalY(stack);

            Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
            Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
            // RenderSystem.setupLevelDiffuseLighting(light0, light1);

            this.renderBodyParts(new FormRenderingContext()
                .set(FormRenderType.ENTITY, this.entity, stack, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, context.getTransition())
                .inUI());

            // DiffuseLighting.disableGuiDepthLighting();

            stack.popPose();
            GlStateManager._depthFunc(GL11.GL_ALWAYS);
        }
    }
}