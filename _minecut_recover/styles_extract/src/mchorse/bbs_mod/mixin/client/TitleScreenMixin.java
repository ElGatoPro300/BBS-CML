package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.title.UIBBSTitleFilmsMenu;
import net.minecraft.class_2561;
import net.minecraft.class_339;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_6379;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_437.class)
public class TitleScreenMixin
{
    @Shadow
    protected <T extends class_364 & class_4068 & class_6379> T addDrawableChild(T drawableElement)
    {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$addTitleButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof class_442 screen))
        {
            return;
        }

        int buttonWidth = 200;
        int x = screen.field_22789 / 2 - buttonWidth / 2;
        int maxY = screen.field_22790 / 4 + 48;

        for (class_364 element : screen.method_25396())
        {
            if (element instanceof class_339 widget)
            {
                maxY = Math.max(maxY, widget.method_46427() + widget.method_25364());
            }
        }

        int buttonY = maxY + 4;

        this.addDrawableChild(class_4185.method_46430(class_2561.method_43471("bbs.ui.title_menu.bbs"), (button) -> UIScreen.open(new UIBBSTitleFilmsMenu()))
            .method_46434(x, buttonY, buttonWidth, 20)
            .method_46431());
    }
}
