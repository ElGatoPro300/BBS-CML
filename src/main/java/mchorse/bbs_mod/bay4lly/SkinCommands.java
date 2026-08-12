package mchorse.bbs_mod.bay4lly;

import mchorse.bbs_mod.network.ServerNetwork;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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

    public static void attach(LiteralArgumentBuilder<ServerCommandSource> bbs, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> getskin = CommandManager.literal("getskin");
        LiteralArgumentBuilder<ServerCommandSource> name = CommandManager.literal("name");
        RequiredArgumentBuilder<ServerCommandSource, String> player = CommandManager.argument("player", StringArgumentType.word());
        LiteralArgumentBuilder<ServerCommandSource> url = CommandManager.literal("url");
        RequiredArgumentBuilder<ServerCommandSource, String> link = CommandManager.argument("link", StringArgumentType.string());
        RequiredArgumentBuilder<ServerCommandSource, String> saveName = CommandManager.argument("name", StringArgumentType.word());

        player.executes(ctx ->
        {
            ServerCommandSource source = ctx.getSource();
            String playerName = StringArgumentType.getString(ctx, "player");
            source.sendFeedback(() -> Text.translatable("command.getskin.downloading"), false);
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
                        MinecraftServer srv = source.getServer();
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        ServerNetwork.sendBay4llySkinToAll(srv, bytes, playerName);
                        source.sendFeedback(() -> Text.translatable("command.getskin.success"), true);
                    }
                    catch (Exception e)
                    {
                        source.sendError(Text.translatable("command.getskin.error", e.getMessage()));
                    }
                }, source.getServer())
                .exceptionally(th ->
                {
                    source.sendError(Text.translatable("command.getskin.error", th.getMessage()));
                    return null;
                });
            return 1;
        });

        saveName.executes(ctx ->
        {
            ServerCommandSource source = ctx.getSource();
            String u = StringArgumentType.getString(ctx, "link");
            String n = StringArgumentType.getString(ctx, "name");
            source.sendFeedback(() -> Text.translatable("command.getskin.downloading"), false);
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
                        MinecraftServer srv = source.getServer();
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        ServerNetwork.sendBay4llySkinToAll(srv, bytes, n);
                        source.sendFeedback(() -> Text.translatable("command.getskin.success"), true);
                    }
                    catch (Exception e)
                    {
                        source.sendError(Text.translatable("command.getskin.error", e.getMessage()));
                    }
                }, source.getServer())
                .exceptionally(th ->
                {
                    source.sendError(Text.translatable("command.getskin.error", th.getMessage()));
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
