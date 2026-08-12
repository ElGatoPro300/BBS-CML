package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;

import org.lwjgl.glfw.GLFW;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin
{
    private static float getMovementMultiplier(boolean positive, boolean negative)
    {
        return positive == negative ? 0F : (positive ? 1F : -1F);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTick(CallbackInfo info)
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (
            menu instanceof UIDashboard dashboard &&
            dashboard.getPanels().panel instanceof UIFilmPanel filmPanel &&
            filmPanel.getController().isControlling()
        ) {
            KeyboardInput input = (KeyboardInput) (Object) this;

            boolean forward = Window.isKeyPressed(GLFW.GLFW_KEY_W);
            boolean back = Window.isKeyPressed(GLFW.GLFW_KEY_S);
            boolean left = Window.isKeyPressed(GLFW.GLFW_KEY_A);
            boolean right = Window.isKeyPressed(GLFW.GLFW_KEY_D);

            float fMul = getMovementMultiplier(forward, back);
            float sMul = getMovementMultiplier(left, right);
            Vec2 movement = new Vec2(sMul, fMul).normalized();

            boolean jump = Window.isKeyPressed(GLFW.GLFW_KEY_SPACE);
            boolean sneak = Window.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);

            Minecraft.getInstance().options.keyJump.setDown(jump);
            Minecraft.getInstance().options.keyShift.setDown(sneak);

            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isMovingSlowly())
            {
                movement = new Vec2(movement.x * 0.3F, movement.y * 0.3F);
            }

            input.moveVector = movement;

            UIFilmController controller = filmPanel.getController();
            boolean moving = movement.x != 0F || movement.y != 0F;

            controller.dampenActorControlDrift(moving);
        }
    }
}
