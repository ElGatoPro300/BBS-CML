package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.gui.BBSLogoButtonWidget;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIWorldFilmsBrowserPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.class_310;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_526;
import net.minecraft.class_6379;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_437.class)
public abstract class SelectWorldScreenMixin
{
    @Shadow
    protected abstract <T extends class_364 & class_4068 & class_6379> T addDrawableChild(T drawableElement);

    @Unique
    private BBSLogoButtonWidget bbs$selectWorldLogoButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$addSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof class_526 screen))
        {
            return;
        }

        this.bbs$selectWorldLogoButton = null;
        this.bbs$ensureSelectWorldBbsButton(screen);
    }

    @Inject(method = "initTabNavigation", at = @At("TAIL"))
    private void bbs$repositionSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof class_526 screen))
        {
            return;
        }

        this.bbs$ensureSelectWorldBbsButton(screen);
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void bbs$resizeSelectWorldBbsButton(class_310 client, int width, int height, CallbackInfo ci)
    {
        if (!((Object) this instanceof class_526 screen))
        {
            return;
        }

        this.bbs$ensureSelectWorldBbsButton(screen);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void bbs$clearSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof class_526))
        {
            return;
        }

        this.bbs$selectWorldLogoButton = null;
    }

    @Unique
    private void bbs$ensureSelectWorldBbsButton(class_526 screen)
    {
        if (class_310.method_1551().field_1687 != null)
        {
            return;
        }

        int size = 20;
        int vanillaLeft = screen.field_22789 / 2 - 154;
        int x = Math.max(4, vanillaLeft - size - 4);
        int y = screen.field_22790 - 52;

        if (!this.bbs$isSelectWorldButtonAttached(screen))
        {
            this.bbs$selectWorldLogoButton = new BBSLogoButtonWidget(x, y, size, size, (button) ->
            {
                UIDashboard dashboard = BBSModClient.getDashboard();

                dashboard.setPanel(dashboard.getPanel(UIWorldFilmsBrowserPanel.class));
                UIScreen.open(dashboard);
            });

            this.addDrawableChild(this.bbs$selectWorldLogoButton);
        }
        else
        {
            this.bbs$selectWorldLogoButton.method_46421(x);
            this.bbs$selectWorldLogoButton.method_46419(y);
            this.bbs$selectWorldLogoButton.method_25358(size);
            this.bbs$selectWorldLogoButton.method_53533(size);
        }
    }

    @Unique
    private boolean bbs$isSelectWorldButtonAttached(class_526 screen)
    {
        if (this.bbs$selectWorldLogoButton == null)
        {
            return false;
        }

        for (class_364 element : screen.method_25396())
        {
            if (element == this.bbs$selectWorldLogoButton)
            {
                return true;
            }
        }

        return false;
    }
}
