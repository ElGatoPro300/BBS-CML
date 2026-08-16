package mchorse.bbs_mod.mixin.client.audio;

import mchorse.bbs_mod.utils.LoopbackAudioController;

import net.minecraft.client.sound.SoundEngine;

import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin
{
    @Shadow
    private long devicePointer;

    @Unique
    private boolean bbs$usingLoopbackDevice;

    @Inject(method = "init", at = @At("HEAD"))
    private void bbs$init(String deviceSpecifier, boolean directionalAudio, CallbackInfo ci)
    {
        this.bbs$usingLoopbackDevice = LoopbackAudioController.isCaptureRequested();

        if (!this.bbs$usingLoopbackDevice)
        {
            LoopbackAudioController.setLoopbackDevice(0L);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$afterInit(String deviceSpecifier, boolean directionalAudio, CallbackInfo ci)
    {
        if (this.bbs$usingLoopbackDevice)
        {
            LoopbackAudioController.setLoopbackDevice(this.devicePointer);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void bbs$close(CallbackInfo ci)
    {
        if (this.bbs$usingLoopbackDevice)
        {
            LoopbackAudioController.setLoopbackDevice(0L);
        }
    }

    @WrapOperation(
        method = "init",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundEngine;openDeviceOrFallback(Ljava/lang/String;)J")
    )
    private long bbs$openLoopbackDevice(String deviceSpecifier, Operation<Long> original)
    {
        if (!this.bbs$usingLoopbackDevice)
        {
            return original.call(deviceSpecifier);
        }

        return SOFTLoopback.alcLoopbackOpenDeviceSOFT((CharSequence) null);
    }

    @WrapOperation(
        method = "init",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcCreateContext(JLjava/nio/IntBuffer;)J", remap = false)
    )
    private long bbs$createLoopbackContext(long deviceHandle, IntBuffer attrList, Operation<Long> original)
    {
        if (!this.bbs$usingLoopbackDevice)
        {
            return original.call(deviceHandle, attrList);
        }

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            IntBuffer format = stack.callocInt(7)
                .put(SOFTLoopback.ALC_FORMAT_TYPE_SOFT).put(SOFTLoopback.ALC_FLOAT_SOFT)
                .put(SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT).put(SOFTLoopback.ALC_STEREO_SOFT)
                .put(ALC10.ALC_FREQUENCY).put(48000)
                .put(0)
                .flip();

            return ALC10.alcCreateContext(deviceHandle, format);
        }
    }
}
