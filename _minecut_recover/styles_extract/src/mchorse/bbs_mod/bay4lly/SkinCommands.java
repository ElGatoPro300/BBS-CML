package mchorse.bbs_mod.bay4lly;

import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.server.MinecraftServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class SkinCommands
{
    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r ->
    {
        Thread t = new Thread(r, "BBS-GetSkin");
        t.setDaemon(true);
        return t;
    });

    public static void attach(LiteralArgumentBuilder<class_2168> bbs, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> getskin = class_2170.method_9247("getskin");
        LiteralArgumentBuilder<class_2168> name = class_2170.method_9247("name");
        RequiredArgumentBuilder<class_2168, String> player = class_2170.method_9244("player", StringArgumentType.word());
        LiteralArgumentBuilder<class_2168> url = class_2170.method_9247("url");
        RequiredArgumentBuilder<class_2168, String> link = class_2170.method_9244("link", StringArgumentType.string());
        RequiredArgumentBuilder<class_2168, String> saveName = class_2170.method_9244("name", StringArgumentType.word());

        player.executes(ctx ->
        {
            class_2168 source = ctx.getSource();
            String playerName = StringArgumentType.getString(ctx, "player");
            source.method_9226(() -> class_2561.method_43471("command.getskin.downloading"), false);
            CompletableFuture
                .supplyAsync(() ->
                {
                    try
                    {
                        String skinUrl = getSkinUrlFromName(playerName);
                        return downloadSkin(skinUrl, playerName);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }, EXECUTOR)
                .thenAcceptAsync(file ->
                {
                    try
                    {
                        SkinManager.saveSkin(playerName, file);
                        MinecraftServer srv = source.method_9211();
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        ServerNetwork.sendBay4llySkinToAll(srv, bytes, playerName);
                        source.method_9226(() -> class_2561.method_43471("command.getskin.success"), true);
                    }
                    catch (Exception e)
                    {
                        source.method_9213(class_2561.method_43469("command.getskin.error", e.getMessage()));
                    }
                }, source.method_9211())
                .exceptionally(th ->
                {
                    source.method_9213(class_2561.method_43469("command.getskin.error", th.getMessage()));
                    return null;
                });
            return 1;
        });

        saveName.executes(ctx ->
        {
            class_2168 source = ctx.getSource();
            String u = StringArgumentType.getString(ctx, "link");
            String n = StringArgumentType.getString(ctx, "name");
            source.method_9226(() -> class_2561.method_43471("command.getskin.downloading"), false);
            CompletableFuture
                .supplyAsync(() ->
                {
                    try
                    {
                        return downloadSkin(u, n);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }, EXECUTOR)
                .thenAcceptAsync(file ->
                {
                    try
                    {
                        SkinManager.saveSkin(n, file);
                        MinecraftServer srv = source.method_9211();
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        ServerNetwork.sendBay4llySkinToAll(srv, bytes, n);
                        source.method_9226(() -> class_2561.method_43471("command.getskin.success"), true);
                    }
                    catch (Exception e)
                    {
                        source.method_9213(class_2561.method_43469("command.getskin.error", e.getMessage()));
                    }
                }, source.method_9211())
                .exceptionally(th ->
                {
                    source.method_9213(class_2561.method_43469("command.getskin.error", th.getMessage()));
                    return null;
                });
            return 1;
        });

        getskin.then(name.then(player));
        getskin.then(url.then(link.then(saveName)));
        bbs.then(getskin.requires(hasPermissions));
    }

    private static String getSkinUrlFromName(String playerName) throws IOException
    {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        if (connection.getResponseCode() == 200)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
            {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                String uuid = json.get("id").getAsString();
                URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                HttpURLConnection profileConnection = (HttpURLConnection) profileUrl.openConnection();
                profileConnection.setRequestMethod("GET");
                profileConnection.setConnectTimeout(5000);
                profileConnection.setReadTimeout(10000);
                if (profileConnection.getResponseCode() == 200)
                {
                    try (BufferedReader profileReader = new BufferedReader(new InputStreamReader(profileConnection.getInputStream())))
                    {
                        StringBuilder profileResponse = new StringBuilder();
                        String profileLine;
                        while ((profileLine = profileReader.readLine()) != null) profileResponse.append(profileLine);
                        JsonObject profileJson = JsonParser.parseString(profileResponse.toString()).getAsJsonObject();
                        String encodedTextures = profileJson.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
                        String decodedTextures = new String(Base64.getDecoder().decode(encodedTextures));
                        JsonObject texturesJson = JsonParser.parseString(decodedTextures).getAsJsonObject();
                        return texturesJson.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                    }
                }
            }
        }
        throw new IOException("Oyuncu için skin URL bulunamadı: " + playerName);
    }

    private static File downloadSkin(String skinUrl, String playerName) throws IOException
    {
        URL url = new URL(skinUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        File tmpFolder = new File("tmp_skins");
        if (!tmpFolder.exists()) tmpFolder.mkdirs();
        File tempFile = new File(tmpFolder, playerName + ".png");
        Files.copy(connection.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }
}
