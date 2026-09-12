package mchorse.bbs_mod.client.render.special;

import mchorse.bbs_mod.forms.renderers.FormRenderer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import org.joml.Matrix3x2f;

import org.jspecify.annotations.Nullable;

/**
 * Render state for a BBS form thumbnail drawn as a vanilla special GUI element.
 */
public record BbsFormGuiElementRenderState(
    FormRenderer<?> renderer,
    float angle,
    float transition,
    Matrix3x2f pose,
    int x0, int y0, int x1, int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState
{
    public BbsFormGuiElementRenderState(FormRenderer<?> renderer, float angle, float transition, Matrix3x2f pose, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea)
    {
        this(renderer, angle, transition, pose, x0, y0, x1, y1, scale, scissorArea,
            PictureInPictureRenderState.getBounds(
                x0 + (int) pose.m20(), y0 + (int) pose.m21(),
                x1 + (int) pose.m20(), y1 + (int) pose.m21(), scissorArea));
    }
}
