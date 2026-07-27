package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.class_743;
import org.lwjgl.glfw.GLFW;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_743.class)
public class KeyboardInputMixin
{
    private static float getMovementMultiplier(boolean positive, boolean negative)
    {
        return positive == negative ? 0F : (positive ? 1F : -1F);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTick(boolean slowDown, float slowDownFactor, CallbackInfo info)
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (
            menu instanceof UIDashboard dashboard &&
            dashboard.getPanels().panel instanceof UIFilmPanel filmPanel &&
            filmPanel.getController().isControlling()
        ) {
            class_743 input = (class_743) (Object) this;

            input.field_3910 = Window.isKeyPressed(GLFW.GLFW_KEY_W);
            input.field_3909 = Window.isKeyPressed(GLFW.GLFW_KEY_S);
            input.field_3908 = Window.isKeyPressed(GLFW.GLFW_KEY_A);
            input.field_3906 = Window.isKeyPressed(GLFW.GLFW_KEY_D);
            input.field_3905 = getMovementMultiplier(input.field_3910, input.field_3909);
            input.field_3907 = getMovementMultiplier(input.field_3908, input.field_3906);
            input.field_3904 = Window.isKeyPressed(GLFW.GLFW_KEY_SPACE);
            input.field_3903 = Window.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);

            if (slowDown)
            {
                input.field_3907 *= slowDownFactor;
                input.field_3905 *= slowDownFactor;
            }
        }
    }
}