package mchorse.bbs_mod.client.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;

public class BBSLogoButtonWidget extends ButtonWidget
{
    private static final Identifier LOGO = Identifier.of("bbs", "textures/gui/cml_icon.png");

    private final Runnable action;

    public BBSLogoButtonWidget(int x, int y, int width, int height, Runnable action)
    {
        super(x, y, width, height, Text.of(" "), (button) -> action.run(), DEFAULT_NARRATION_SUPPLIER);

        this.action = action;
    }

    @Override
    public void onPress(AbstractInput input)
    {
        this.action.run();
    }

    @Override
    protected void drawIcon(DrawContext context, int x, int y, float delta)
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

        context.fill(x1, y1, x2, y2, borderColor);
        context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, bgColor);

        int logoSize = Math.min(this.width, this.height) - 6;
        int logoX = x1 + (this.width - logoSize) / 2;
        int logoY = y1 + (this.height - logoSize) / 2;

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, 0F, 0F, logoSize, logoSize, logoSize, logoSize, logoSize, logoSize, -1);
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder)
    {}
}
