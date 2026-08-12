package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.gui.BBSLogoButtonWidget;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIWorldFilmsBrowserPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class SelectWorldScreenMixin
{
    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T drawableElement);

    @Unique
    private BBSLogoButtonWidget bbs$selectWorldLogoButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$addSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof SelectWorldScreen screen))
        {
            return;
        }

        this.bbs$selectWorldLogoButton = null;
        this.bbs$ensureSelectWorldBbsButton(screen);
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void bbs$resizeSelectWorldBbsButton(int width, int height, CallbackInfo ci)
    {
        if (!((Object) this instanceof SelectWorldScreen screen))
        {
            return;
        }

        this.bbs$ensureSelectWorldBbsButton(screen);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void bbs$clearSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof SelectWorldScreen))
        {
            return;
        }

        this.bbs$selectWorldLogoButton = null;
    }

    @Unique
    private void bbs$ensureSelectWorldBbsButton(SelectWorldScreen screen)
    {
        if (Minecraft.getInstance().level != null)
        {
            return;
        }

        int size = 20;
        int vanillaLeft = screen.width / 2 - 154;
        int x = Math.max(4, vanillaLeft - size - 4);
        int y = screen.height - 52;

        if (!this.bbs$isSelectWorldButtonAttached(screen))
        {
            this.bbs$selectWorldLogoButton = new BBSLogoButtonWidget(x, y, size, size, (button) ->
            {
                UIDashboard dashboard = BBSModClient.getDashboard();

                dashboard.setPanel(dashboard.getPanel(UIWorldFilmsBrowserPanel.class));
                UIScreen.open(dashboard);
            });

            this.addRenderableWidget(this.bbs$selectWorldLogoButton);
        }
        else
        {
            this.bbs$selectWorldLogoButton.setX(x);
            this.bbs$selectWorldLogoButton.setY(y);
            this.bbs$selectWorldLogoButton.setWidth(size);
            this.bbs$selectWorldLogoButton.setHeight(size);
        }
    }

    @Unique
    private boolean bbs$isSelectWorldButtonAttached(SelectWorldScreen screen)
    {
        if (this.bbs$selectWorldLogoButton == null)
        {
            return false;
        }

        for (GuiEventListener element : screen.children())
        {
            if (element == this.bbs$selectWorldLogoButton)
            {
                return true;
            }
        }

        return false;
    }
}
