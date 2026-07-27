package mchorse.bbs_mod.utils;

import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;

/**
 * Small HTTP helper for optional remote content (news, banners, CDN, etc.).
 * Failures are logged at debug level so transient network issues do not spam the console.
 */
public final class RemoteHttp
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private RemoteHttp()
    {}

    public static String fetchString(String url)
    {
        if (url == null || url.isEmpty())
        {
            return null;
        }

        try
        {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "BBS-CML-Edition/2.0")
                .header("Accept", "*/*")
                .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
            {
                return response.body();
            }

            LOGGER.debug("Remote HTTP request to {} returned status {}", url, response.statusCode());
        }
        catch (SocketException e)
        {
            LOGGER.debug("Remote HTTP connection reset for {}: {}", url, e.getMessage());
        }
        catch (IOException e)
        {
            LOGGER.debug("Remote HTTP request failed for {}: {}", url, e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            LOGGER.debug("Remote HTTP request interrupted for {}", url);
        }

        return null;
    }
}
