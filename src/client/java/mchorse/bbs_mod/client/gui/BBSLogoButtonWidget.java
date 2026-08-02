package mchorse.bbs_mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BBSLogoButtonWidget extends ButtonWidget
{
    private static final Identifier LOGO = Identifier.of("bbs", "textures/gui/cml_icon.png");

    public BBSLogoButtonWidget(int x, int y, int width, int height, PressAction onPress)
    {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.renderWidget(context, mouseX, mouseY, delta);

        int logoSize = Math.min(this.width, this.height) - 6;
        int logoX = this.getX() + (this.width - logoSize) / 2;
        int logoY = this.getY() + (this.height - logoSize) / 2;

        context.drawTexture(LOGO, logoX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);
    }
}
