package mchorse.bbs_mod.client.render.special;

import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;

import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Special GUI element renderer for BBS form previews in 1.21.11.
 */
public class BbsFormGuiElementRenderer extends PictureInPictureRenderer<BbsFormGuiElementRenderState>
{
    private static int errorLog;

    public BbsFormGuiElementRenderer()
    {
        super();
    }

    public BbsFormGuiElementRenderer(Object ignored)
    {
        super();
    }

    @Override
    public Class<BbsFormGuiElementRenderState> getRenderStateClass()
    {
        return BbsFormGuiElementRenderState.class;
    }

    @Override
    protected void renderToTexture(BbsFormGuiElementRenderState state, PoseStack matrices, SubmitNodeCollector submitNodeCollector)
    {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);

        try
        {
            if (state.angle() != 0F)
            {
                matrices.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(state.angle()), 0F, 1F, 0F)));
            }

            FormRenderingContext context = new FormRenderingContext();

            context.set(FormRenderType.PREVIEW, null, matrices, 0x00F000F0, OverlayTexture.NO_OVERLAY, state.transition());
            context.ui = true;
            context.modelRenderer = true;

            state.renderer().render(context);
        }
        catch (Exception e)
        {
            if (errorLog++ % 120 == 0)
            {
                System.out.println("[BBS list preview] form render failed: " + e);
            }
        }
    }

    @Override
    protected float getTranslateY(int height, int windowScaleFactor)
    {
        return 0.85F * height;
    }

    @Override
    protected String getTextureLabel()
    {
        return "bbs form";
    }
}
