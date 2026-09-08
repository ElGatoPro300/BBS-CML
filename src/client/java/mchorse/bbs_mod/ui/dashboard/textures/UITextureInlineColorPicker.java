package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Inline-only color picker for texture editor side panel.
 * Dynamically adjusts the gradient box, hue slider and channel inputs
 * to fit the available container height without overflowing.
 */
public class UITextureInlineColorPicker extends UIColorPicker
{
    public UITextureInlineColorPicker(Consumer<Integer> callback)
    {
        super(callback);

        this.favorite.setVisible(false);
        this.recent.setVisible(false);
    }

    @Override
    public void resize()
    {
        this.resizer.apply(this.area);

        int padX = 2;
        int padY = 2;
        int gap = COLOR_PICKER_GAP;
        int barH = COLOR_PICKER_BAR_HEIGHT;
        int modeH = MODE_ROW_HEIGHT;
        int fieldsH = this.mode == ColorMode.HEX ? FIELD_ROW_HEIGHT : CHANNEL_BLOCK_HEIGHT;

        int bottomControlsH = gap + barH + (this.editAlpha ? gap + barH : 0) + gap + modeH + gap + fieldsH + padY;
        int availableH = this.area.h - bottomControlsH;
        int gradH = Math.max(30, availableH);
        int gradW = Math.max(30, this.area.w - padX * 2);

        int x = this.area.x + padX;
        int y = this.area.y + padY;

        this.red.set(x, y, gradW, gradH);
        this.green.set(x, this.red.ey() + gap, gradW, barH);

        if (this.editAlpha)
        {
            this.alpha.set(x, this.green.ey() + gap, gradW, barH);
        }
        else
        {
            this.alpha.set(0, 0, 0, 0);
        }

        this.blue.set(0, 0, 0, 0);

        int bottomY = (this.editAlpha ? this.alpha.ey() : this.green.ey()) + gap - this.area.y;

        this.modeRow.relative(this).x(padX).y(bottomY).w(1F, -padX * 2).h(modeH);
        this.fieldRow.relative(this).x(padX).y(bottomY + modeH + gap).w(1F, -padX * 2).h(fieldsH);
        this.input.relative(this).x(padX).y(bottomY + modeH + gap).w(1F, -padX * 2).h(FIELD_ROW_HEIGHT);

        this.modeRow.resize();
        this.fieldRow.resize();
        this.input.resize();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            return true;
        }

        return super.subKeyPressed(context);
    }
}
