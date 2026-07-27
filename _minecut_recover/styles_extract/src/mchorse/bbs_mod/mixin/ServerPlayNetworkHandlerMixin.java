package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.CloseContainerActionClip;
import mchorse.bbs_mod.actions.types.blocks.InteractBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2168;
import net.minecraft.class_2281;
import net.minecraft.class_2338;
import net.minecraft.class_2363;
import net.minecraft.class_2680;
import net.minecraft.class_2815;
import net.minecraft.class_3222;
import net.minecraft.class_3225;
import net.minecraft.class_3244;
import net.minecraft.class_3965;
import com.mojang.brigadier.ParseResults;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_3244.class)
public class ServerPlayNetworkHandlerMixin
{
    private static final Map<UUID, class_2338> OPEN_CONTAINERS = new HashMap<>();

    @Shadow
    public class_3222 player;

    @Inject(method = "parse", at = @At("HEAD"))
    public void onParse(String command, CallbackInfoReturnable<ParseResults<class_2168>> info)
    {
        BBSMod.getActions().addAction(this.player, () ->
        {
            CommandActionClip clip = new CommandActionClip();

            clip.command.set(command);

            return clip;
        });
    }

    @Redirect(method = "onPlayerInteractBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactBlock(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private class_1269 redirectOnBlockInteract(class_3225 manager, class_3222 player, class_1937 world, class_1799 stack, class_1268 hand, class_3965 hitResult)
    {
        class_2338 interactedPos = hitResult.method_17777();

        BBSMod.getActions().addAction(this.player, () ->
        {
            InteractBlockActionClip clip = new InteractBlockActionClip();

            clip.hit.setHitResult(hitResult);
            clip.hand.set(hand == class_1268.field_5808);

            return clip;
        });

        if (world.method_8320(interactedPos).method_26204() instanceof class_2281)
        {
            OPEN_CONTAINERS.put(this.player.method_5667(), interactedPos.method_10062());
        }
        else if (world.method_8320(interactedPos).method_26204() instanceof class_2363)
        {
            OPEN_CONTAINERS.put(this.player.method_5667(), interactedPos.method_10062());
        }

        return manager.method_14262(player, world, stack, hand, hitResult);
    }

    @Inject(method = "onCloseHandledScreen", at = @At("HEAD"))
    private void onCloseHandledScreen(class_2815 packet, CallbackInfo ci)
    {
        class_2338 containerPos = OPEN_CONTAINERS.remove(this.player.method_5667());

        if (containerPos == null)
        {
            return;
        }

        BBSMod.getActions().addAction(this.player, () ->
        {
            CloseContainerActionClip clip = new CloseContainerActionClip();
            class_2680 state = this.player.method_37908().method_8320(containerPos);

            clip.x.set(containerPos.method_10263());
            clip.y.set(containerPos.method_10264());
            clip.z.set(containerPos.method_10260());

            if (state.method_26204() instanceof class_2363)
            {
                clip.applyState.set(true);
                clip.state.set(state);
            }

            return clip;
        });
    }
}
