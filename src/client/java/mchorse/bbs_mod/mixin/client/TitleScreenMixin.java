package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.title.UIBBSTitleFilmsMenu;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class TitleScreenMixin
{
    @Shadow
    protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement)
    {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$addTitleButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof TitleScreen screen))
        {
            return;
        }

        int buttonWidth = 200;
        int x = screen.width / 2 - buttonWidth / 2;
        int maxY = screen.height / 4 + 48;

        for (Element element : screen.children())
        {
            if (element instanceof ClickableWidget widget)
            {
                maxY = Math.max(maxY, widget.getY() + widget.getHeight());
            }
        }

        int buttonY = maxY + 4;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("bbs.ui.title_menu.bbs"), (button) -> UIScreen.open(new UIBBSTitleFilmsMenu()))
            .dimensions(x, buttonY, buttonWidth, 20)
            .build());
    }
}
