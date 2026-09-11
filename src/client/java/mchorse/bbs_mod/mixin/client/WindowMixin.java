package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.BbsGuiScale;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.platform.Window;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin
{
    /**
     * Apply BBS's UI scale as a fractional value (e.g. 1.6). Minecraft's GUI scale is integer-only,
     * so when a non-whole BBS scale is configured and a BBS screen is open in linked (legacy) mode,
     * override the window's scale factor with the exact value. Independent BBS scale never
     * mutates Minecraft's GUI scale; whole-number and "auto" scales keep Minecraft's normal
     * (clamped) behaviour.
     */
    @ModifyVariable(method = "setGuiScale", at = @At("HEAD"), argsOnly = true)
    private int bbs_overrideUIScaleFactor(int scaleFactor)
    {
        double uiScale = BBSModClient.getUIScaleFactor();

        if (uiScale > 0D && uiScale != Math.floor(uiScale) && Minecraft.getInstance().gui.screen() instanceof UIScreen
            && BbsGuiScale.isLinkedToGame() && !BbsGuiScale.isRestoringGameScale())
        {
            return (int) Math.round(uiScale);
        }

        return scaleFactor;
    }

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Shadow
    private int guiScaledWidth;

    @Shadow
    private int guiScaledHeight;

    @Shadow
    private int guiScale;

    @Inject(method = "getGuiScale", at = @At("HEAD"), cancellable = true)
    public void onGetScaleFactor(CallbackInfoReturnable<Integer> info)
    {
        if (Minecraft.getInstance().gui.screen() instanceof UIScreen && !BbsGuiScale.isLinkedToGame()
            && !BbsGuiScale.isRestoringGameScale() && BbsGuiScale.getFactor() > 0D)
        {
            info.setReturnValue((int) BbsGuiScale.getFactor());
        }
    }

    @Inject(method = "getScreenWidth", at = @At("HEAD"), cancellable = true)
    public void onGetWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue(BBSRendering.getVideoWidth());
        }
    }

    @Inject(method = "getScreenHeight", at = @At("HEAD"), cancellable = true)
    public void onGetHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue(BBSRendering.getVideoHeight());
        }
    }

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    public void onGetFramebufferWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoWidth() * BBSModClient.getOriginalFramebufferScale()));
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void onGetFramebufferHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoHeight() * BBSModClient.getOriginalFramebufferScale()));
        }
    }

    @Inject(method = "getGuiScaledWidth", at = @At("HEAD"), cancellable = true)
    public void onGetScaledWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoWidth() / (double) this.guiScale * BBSModClient.getOriginalFramebufferScale()));
        }
        else if (Minecraft.getInstance().gui.screen() instanceof UIScreen && !BbsGuiScale.isLinkedToGame())
        {
            info.setReturnValue(BbsGuiScale.getScaledWidth());
        }
    }

    @Inject(method = "getGuiScaledHeight", at = @At("HEAD"), cancellable = true)
    public void onGetScaledHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoHeight() / (double) this.guiScale * BBSModClient.getOriginalFramebufferScale()));
        }
        else if (Minecraft.getInstance().gui.screen() instanceof UIScreen && !BbsGuiScale.isLinkedToGame())
        {
            info.setReturnValue(BbsGuiScale.getScaledHeight());
        }
    }
}
