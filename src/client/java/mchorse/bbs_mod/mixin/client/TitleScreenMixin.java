package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.title.UIBBSTitleFilmsMenu;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class TitleScreenMixin
{
    @Shadow
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T drawableElement)
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

        for (GuiEventListener element : screen.children())
        {
            if (element instanceof AbstractWidget widget)
            {
                maxY = Math.max(maxY, widget.getY() + widget.getHeight());
            }
        }

        int buttonY = maxY + 4;

        this.addRenderableWidget(Button.builder(Component.translatable("bbs.ui.title_menu.bbs"), (button) -> UIScreen.open(new UIBBSTitleFilmsMenu()))
            .bounds(x, buttonY, buttonWidth, 20)
            .build());
    }
}
