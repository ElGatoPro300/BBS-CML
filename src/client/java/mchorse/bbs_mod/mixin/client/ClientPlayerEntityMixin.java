package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.network.ClientPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Actor-control drives the live player. Keep creative flight off for the session so
 * ice-level flight friction cannot reappear via ability sync. Velocity is left alone
 * so vanilla walk stop inertia and form animations can decay naturally.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin
{
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bbs$keepActorControlGrounded(CallbackInfo info)
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (!(menu instanceof UIDashboard dashboard))
        {
            return;
        }

        if (!(dashboard.getPanels().panel instanceof UIFilmPanel filmPanel))
        {
            return;
        }

        UIFilmController controller = filmPanel.getController();

        if (!controller.shouldForceGroundedControl())
        {
            return;
        }

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        player.getAbilities().flying = false;
        player.getAbilities().allowFlying = false;
    }
}
