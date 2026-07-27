package mchorse.bbs_mod.client.gui;

import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185;

public class BBSLogoButtonWidget extends class_4185
{
    private static final class_2960 LOGO = class_2960.method_60655("bbs", "textures/gui/bbs_logo.png");

    public BBSLogoButtonWidget(int x, int y, int width, int height, class_4241 onPress)
    {
        super(x, y, width, height, class_2561.method_43473(), onPress, field_40754);
    }

    @Override
    protected void method_48579(class_332 context, int mouseX, int mouseY, float delta)
    {
        super.method_48579(context, mouseX, mouseY, delta);

        int logoSize = Math.min(this.field_22758, this.field_22759) - 6;
        int logoX = this.method_46426() + (this.field_22758 - logoSize) / 2;
        int logoY = this.method_46427() + (this.field_22759 - logoSize) / 2;

        context.method_25290(LOGO, logoX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);
    }
}
