package mchorse.bbs_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.utils.VideoRecorder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin
{
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void onTick(IntegratedServer server, BooleanSupplier supplier, Operation<Void> original)
    {
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