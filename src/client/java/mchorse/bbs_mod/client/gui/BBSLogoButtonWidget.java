package mchorse.bbs_mod.client.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.mojang.blaze3d.systems.RenderSystem;

public class BBSLogoButtonWidget extends ButtonWidget
{
    private static final Identifier LOGO = Identifier.of("bbs", "textures/gui/cml_icon.png");

    private final Runnable action;

    public BBSLogoButtonWidget(int x, int y, int width, int height, Runnable action)
    {
        super(x, y, width, height, Text.literal(" "));

        this.action = action;
    }

    @Override
    public void onPress(AbstractInput input)
    {
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.width;
        int y2 = y1 + this.height;

        int bgColor = this.isHovered() ? 0xFF24242C : 0xFF141418;
        int borderColor = this.isHovered() ? 0xFF444452 : 0xFF2A2A34;

        if (!this.active)
        {
            bgColor = 0xFF0E0E12;
            borderColor = 0xFF18181F;
        }

        /* Border and background fill */
        context.fill(x1, y1, x2, y2, borderColor);
        context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, bgColor);

        /* Icon rendering with blend enabled */
        int logoSize = Math.min(this.width, this.height) - 6;
        int logoX = x1 + (this.width - logoSize) / 2;
        int logoY = y1 + (this.height - logoSize) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        context.drawTexture(LOGO, logoX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
