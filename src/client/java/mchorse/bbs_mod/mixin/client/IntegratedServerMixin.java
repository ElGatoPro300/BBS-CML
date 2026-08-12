package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.RecordingPauseHelper;
import mchorse.bbs_mod.utils.VideoRecorder;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;

import java.util.function.BooleanSupplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin
{
    @WrapOperation(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickServer(Ljava/util/function/BooleanSupplier;)V"))
    private void onTick(IntegratedServer server, BooleanSupplier supplier, Operation<Void> original)
    {
        if (RecordingPauseHelper.isActive())
        {
            return;
        }

        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();

        if (videoRecorder.isRecording())
        {
            while (videoRecorder.lastServerTicks < videoRecorder.serverTicks)
            {
                original.call(server, supplier);

                videoRecorder.lastServerTicks += 1;
            }
        }
        else
        {
            original.call(server, supplier);
        }
    }
}