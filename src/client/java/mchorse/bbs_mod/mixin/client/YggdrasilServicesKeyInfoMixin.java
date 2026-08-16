package mchorse.bbs_mod.mixin.client;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;

import java.net.SocketException;
import java.net.URL;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mojang's authlib fetches session signature keys from {@code api.minecraftservices.com} on startup.
 * Transient {@code Connection reset} errors are common on dev clients and noisy networks; the game
 * already retries in the background and falls back to an empty key set, so we retry a few times
 * and downgrade transient failures to debug logging.
 */
@Mixin(targets = "YggdrasilServicesKeyInfo")
public class YggdrasilServicesKeyInfoMixin
{
    @Unique
    private static final int BBS_MAX_FETCH_ATTEMPTS = 4;

    @Unique
    private static final long BBS_FETCH_RETRY_DELAY_MS = 750L;

    @Redirect(
        method = "fetch",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/authlib/minecraft/client/MinecraftClient;get(Ljava/net/URL;Ljava/lang/Class;)Ljava/lang/Object;"
        )
    )
    private static Object bbs$getWithRetry(MinecraftClient client, URL url, Class<?> responseClass) throws MinecraftClientException
    {
        MinecraftClientException last = null;

        for (int attempt = 1; attempt <= BBS_MAX_FETCH_ATTEMPTS; attempt++)
        {
            try
            {
                return client.get(url, responseClass);
            }
            catch (MinecraftClientException e)
            {
                last = e;

                if (!bbs$isTransientNetworkFailure(e) || attempt >= BBS_MAX_FETCH_ATTEMPTS)
                {
                    throw e;
                }

                try
                {
                    Thread.sleep(BBS_FETCH_RETRY_DELAY_MS * attempt);
                }
                catch (InterruptedException interrupted)
                {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw last;
    }

    @Redirect(
        method = "fetch",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"
        )
    )
    private static void bbs$logFetchFailure(Logger logger, String message, Throwable throwable)
    {
        if (bbs$isTransientNetworkFailure(throwable))
        {
            logger.debug("{} (transient network error; Mojang key fetch will retry in the background)", message, throwable);

            return;
        }

        logger.error(message, throwable);
    }

    @Unique
    private static boolean bbs$isTransientNetworkFailure(Throwable throwable)
    {
        while (throwable != null)
        {
            if (throwable instanceof SocketException)
            {
                return true;
            }

            String message = throwable.getMessage();

            if (message != null)
            {
                if (message.contains("Connection reset")
                    || message.contains("Connection timed out")
                    || message.contains("Unexpected end of file from server"))
                {
                    return true;
                }
            }

            throwable = throwable.getCause();
        }

        return false;
    }
}
