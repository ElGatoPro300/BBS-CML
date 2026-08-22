package mchorse.bbs_mod.utils.skin;

import mchorse.bbs_mod.network.ServerNetwork;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;


public class SkinCommands
{
    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r ->
    {
        Thread thread = new Thread(r, "BBS-GetSkin");
        thread.setDaemon(true);
        return thread;
    });

    private static final MojangApiClient MOJANG_API = new MojangApiClient();
    private static final SkinDownloader DOWNLOADER = new SkinDownloader();

    public static void attach(LiteralArgumentBuilder<ServerCommandSource> bbs, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> getskin = CommandManager.literal("getskin");

        LiteralArgumentBuilder<ServerCommandSource> name = CommandManager.literal("name");
        RequiredArgumentBuilder<ServerCommandSource, String> player = CommandManager.argument("player", StringArgumentType.word());
        player.executes(SkinCommands::executeGetByName);

        LiteralArgumentBuilder<ServerCommandSource> url = CommandManager.literal("url");
        RequiredArgumentBuilder<ServerCommandSource, String> link = CommandManager.argument("link", StringArgumentType.string());
        RequiredArgumentBuilder<ServerCommandSource, String> saveName = CommandManager.argument("name", StringArgumentType.word());
        saveName.executes(SkinCommands::executeGetByUrl);

        getskin.then(name.then(player));
        getskin.then(url.then(link.then(saveName)));
        bbs.then(getskin.requires(hasPermissions));
    }

    private static int executeGetByName(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx)
    {
        ServerCommandSource source = ctx.getSource();
        String playerName = StringArgumentType.getString(ctx, "player");

        runSkinPipeline(
                source,
                () -> MOJANG_API.getSkinUrlFromName(playerName),
                playerName
        );

        return 1;
    }

    private static int executeGetByUrl(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx)
    {
        ServerCommandSource source = ctx.getSource();
        String link = StringArgumentType.getString(ctx, "link");
        String saveName = StringArgumentType.getString(ctx, "name");

        runSkinPipeline(source, () -> link, saveName);

        return 1;
    }

    private static void runSkinPipeline(ServerCommandSource source, UrlSupplier urlSupplier, String saveName)
    {
        source.sendFeedback(() -> Text.translatable("command.getskin.downloading"), false);

        CompletableFuture
                .supplyAsync(() -> resolveAndDownload(urlSupplier, saveName), EXECUTOR)
                .thenAcceptAsync(file -> saveAndBroadcast(source, file, saveName), source.getServer())
                .exceptionally(throwable -> reportFailure(source, throwable));
    }

    private static File resolveAndDownload(UrlSupplier urlSupplier, String saveName)
    {
        try
        {
            String skinUrl = urlSupplier.get();
            return DOWNLOADER.downloadSkin(skinUrl, saveName);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void saveAndBroadcast(ServerCommandSource source, File file, String saveName)
    {
        try
        {
            SkinManager.saveSkin(saveName, file);

            MinecraftServer server = source.getServer();
            byte[] bytes = Files.readAllBytes(file.toPath());
            ServerNetwork.sendBay4llySkinToAll(server, bytes, saveName);

            source.sendFeedback(() -> Text.translatable("command.getskin.success"), true);
        }
        catch (Exception e)
        {
            source.sendError(Text.translatable("command.getskin.error", e.getMessage()));
        }
    }

    private static Void reportFailure(ServerCommandSource source, Throwable throwable)
    {
        source.sendError(Text.translatable("command.getskin.error", throwable.getMessage()));
        return null;
    }

    @FunctionalInterface
    private interface UrlSupplier
    {
        String get() throws Exception;
    }
}