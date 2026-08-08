package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Actor-control drives the real client player with WASD. Creative flight uses
 * ice-level friction, so releasing movement keys looks like the puppet slides.
 * Force grounded movement for the duration of control (except look-only recording
 * which intentionally enables flight).
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin
{
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void bbs$forceGroundedActorControl(CallbackInfo info)
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

        boolean moving = player.input != null
            && (player.input.movementForward != 0F || player.input.movementSideways != 0F);

        if (!moving)
        {
            Vec3d velocity = player.getVelocity();

            /* Drop leftover flight/air drift so stopping matches normal walking. */
            player.setVelocity(0D, velocity.y, 0D);
        }
    }
}
