package mchorse.bbs_mod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

public class BBSLogoButtonWidget extends Button
{
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("bbs", "textures/gui/cml_icon.png");

    public BBSLogoButtonWidget(int x, int y, int width, int height, Button.OnPress onPress)
    {
        super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta)
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

        context.blit(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, 0F, 0F, logoSize, logoSize, logoSize, logoSize);
    }
}
