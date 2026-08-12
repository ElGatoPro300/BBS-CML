package mchorse.bbs_mod;

import mchorse.bbs_mod.bay4lly.SkinCommands;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.mixin.LevelPropertiesAccessor;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;

import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec3;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class BBSCommands
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment)
    {
        Predicate<CommandSourceStack> hasPermissions = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        LiteralArgumentBuilder<CommandSourceStack> bbs = Commands.literal("bbs").requires((source) -> true);

        registerMorphCommand(bbs, environment, hasPermissions);
        registerModelBlockCommand(bbs, environment, hasPermissions);
        registerMorphEntityCommand(bbs, environment, hasPermissions);
        registerFilmsCommand(bbs, environment, hasPermissions);
        registerDCCommand(bbs, environment, hasPermissions);
        registerOnHeadCommand(bbs, environment, hasPermissions);
        registerConfigCommand(bbs, environment, hasPermissions);
        registerCheatsCommand(bbs, environment);
        registerBoomCommand(bbs, environment, hasPermissions);
        registerStructureSaveCommand(bbs, environment, hasPermissions);
        SkinCommands.attach(bbs, hasPermissions);

        dispatcher.register(bbs);
    }

    private static void registerStructureSaveCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> structures = Commands.literal("structures");
        LiteralArgumentBuilder<CommandSourceStack> save = Commands.literal("save");
        RequiredArgumentBuilder<CommandSourceStack, String> name = Commands.argument("name", StringArgumentType.word());
        RequiredArgumentBuilder<CommandSourceStack, Coordinates> from = Commands.argument("from", BlockPosArgument.blockPos());
        RequiredArgumentBuilder<CommandSourceStack, Coordinates> to = Commands.argument("to", BlockPosArgument.blockPos());

        bbs.then(structures
            .then(save.then(name.then(from.then(to
                .executes(BBSCommands::saveStructure))))
        ));
    }

    private static void registerMorphCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> morph = Commands.literal("morph");
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> target = Commands.argument("target", EntityArgument.players());
        RequiredArgumentBuilder<CommandSourceStack, String> form = Commands.argument("form", StringArgumentType.greedyString());

        morph.then(target
            .executes(BBSCommands::morphCommandDemorph)
            .then(form.executes(BBSCommands::morphCommandMorph)));

        bbs.then(morph.requires(hasPermissions));
    }

    private static void registerModelBlockCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> modelBlock = Commands.literal("model_block");
        LiteralArgumentBuilder<CommandSourceStack> playState = Commands.literal("play_state");
        RequiredArgumentBuilder<CommandSourceStack, Coordinates> coords = Commands.argument("coords", BlockPosArgument.blockPos());
        RequiredArgumentBuilder<CommandSourceStack, String> state = Commands.argument("state", StringArgumentType.string());

        LiteralArgumentBuilder<CommandSourceStack> refresh = Commands.literal("refresh");
        RequiredArgumentBuilder<CommandSourceStack, Integer> randomRange = Commands.argument("random_range", IntegerArgumentType.integer());

        state.suggests((ctx, builder) ->
        {
            BlockPos pos = BlockPosArgument.getBlockPos(ctx, "coords");
            BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);

            if (blockEntity instanceof ModelBlockEntity block)
            {
                Form form = block.getProperties().getForm();

                if (form != null)
                {
                    for (AnimationState animationState : form.states.getAllTyped())
                    {
                        String customId = animationState.customId.get();

                        builder.suggest(customId.trim().isEmpty() ? animationState.id.get() : customId);
                    }
                }
            }

            return builder.buildFuture();
        });

        modelBlock.then(
            playState.then(
                coords.then(
                    state.executes((ctx) ->
                    {
                        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "coords");
                        String animationState = StringArgumentType.getString(ctx, "state");
                        BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);

                        if (blockEntity instanceof ModelBlockEntity)
                        {
                            for (ServerPlayer player : ctx.getSource().getLevel().players())
                            {
                                if (player.blockPosition().distSqr(pos) <= (Math.pow(BBSSettings.modelBlockAnimationStateDistance.get(), 2)))
                                {
                                    ServerNetwork.sendModelBlockState(player, pos, animationState);
                                }
                            }

                            return 1;
                        }

                        return 0;
                    })
                )
            )
        );

        modelBlock.then(
            refresh.then(
                randomRange.executes((ctx) ->
                {
                    int range = IntegerArgumentType.getInteger(ctx, "random_range");

                    for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers())
                    {
                        ServerNetwork.sendReloadModelBlocks(player, range);
                    }

                    return 1;
                })
            )
        );

        bbs.then(modelBlock.requires(hasPermissions));
    }

    private static void registerMorphEntityCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> morph = Commands.literal("morph_entity");

        morph.executes((source) ->
        {
            Entity entity = source.getSource().getEntity();

            if (entity instanceof ServerPlayer player)
            {
                Form form = Morph.getMobForm(player);

                if (form != null)
                {
                    ServerNetwork.sendMorphToTracked(player, form);
                    Morph.getMorph(entity).setForm(FormUtils.copy(form));
                }
            }

            return 1;
        });

        bbs.then(morph.requires(hasPermissions));
    }

    private static void registerFilmsCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> scene = Commands.literal("films");
        LiteralArgumentBuilder<CommandSourceStack> play = Commands.literal("play");
        LiteralArgumentBuilder<CommandSourceStack> stop = Commands.literal("stop");
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> target = Commands.argument("target", EntityArgument.players());
        RequiredArgumentBuilder<CommandSourceStack, String> playFilm = Commands.argument("film", StringArgumentType.string());
        RequiredArgumentBuilder<CommandSourceStack, String> stopFilm = Commands.argument("film", StringArgumentType.string());
        RequiredArgumentBuilder<CommandSourceStack, Boolean> camera = Commands.argument("camera", BoolArgumentType.bool());

        playFilm.suggests((ctx, builder) ->
        {
            for (String key : BBSMod.getFilms().getKeys())
            {
                builder.suggest(key);
            }

            return builder.buildFuture();
        });

        stopFilm.suggests((ctx, builder) ->
        {
            for (String key : BBSMod.getFilms().getKeys())
            {
                builder.suggest(key);
            }

            return builder.buildFuture();
        });

        scene.then(
            target.then(
                play.then(
                    playFilm.executes((source) -> sceneCommandPlay(source, true))
                        .then(
                            camera.executes((source) -> sceneCommandPlay(source, BoolArgumentType.getBool(source, "camera")))
                        )
                )
            )
            .then(
                stop.then(
                    stopFilm.executes(BBSCommands::sceneCommandStop)
                )
            )
        );

        bbs.then(scene.requires(hasPermissions));
    }

    private static void registerDCCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> dc = Commands.literal("dc");
        LiteralArgumentBuilder<CommandSourceStack> shutdown = Commands.literal("shutdown");
        LiteralArgumentBuilder<CommandSourceStack> start = Commands.literal("start");
        LiteralArgumentBuilder<CommandSourceStack> stop = Commands.literal("stop");

        bbs.then(
            dc.requires(hasPermissions).then(start.executes(BBSCommands::DCCommandStart))
                .then(stop.executes(BBSCommands::DCCommandStop))
                .then(shutdown.executes(BBSCommands::DCCommandShutdown))
        );
    }

    private static void registerOnHeadCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> onHead = Commands.literal("on_head");

        bbs.then(onHead.requires(hasPermissions).executes(BBSCommands::onHead));
    }

    private static void registerConfigCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        LiteralArgumentBuilder<CommandSourceStack> config = Commands.literal("config");

        config.requires(Commands.hasPermission(Commands.LEVEL_OWNERS)).then(
            Commands.literal("set").then(
                Commands.argument("option", StringArgumentType.word())
                    .suggests((ctx, builder) ->
                    {
                        Settings settings = BBSMod.getSettings().modules.get("bbs");

                        if (settings != null)
                        {
                            for (ValueGroup value : settings.categories.values())
                            {
                                for (BaseValue baseValue : value.getAll())
                                {
                                    builder.suggest(value.getId() + "." + baseValue.getId());
                                }
                            }
                        }

                        return builder.buildFuture();
                    })
                    .then(
                        Commands.argument("value", StringArgumentType.greedyString()).executes((ctx) ->
                        {
                            Settings settings = BBSMod.getSettings().modules.get("bbs");

                            if (settings != null)
                            {
                                String option = StringArgumentType.getString(ctx, "option");
                                String value = StringArgumentType.getString(ctx, "value");
                                BaseType valueType = DataToString.fromString(value);
                                String[] split = option.split("\\.");

                                if (valueType != null && split.length >= 2)
                                {
                                    BaseValue baseValue = settings.get(split[0], split[1]);

                                    if (baseValue != null)
                                    {
                                        baseValue.fromData(valueType);
                                        settings.saveLater();
                                    }
                                }
                            }

                            return 1;
                        })
                    )
            )
        );

        bbs.then(config.requires(hasPermissions));
    }

    private static void registerCheatsCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment)
    {
        if (environment.includeDedicated)
        {
            return;
        }

        bbs.then(
            Commands.literal("cheats").then(
                Commands.argument("enabled", BoolArgumentType.bool()).executes((ctx) ->
                {
                    MinecraftServer server = ctx.getSource().getServer();
                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                    WorldData saveProperties = server.getWorldData();

                    if (saveProperties instanceof LevelPropertiesAccessor accessor)
                    {
                        LevelSettings levelInfo = saveProperties.getLevelSettings();

                        accessor.bbs$setLevelInfo(new LevelSettings(levelInfo.levelName(),
                            levelInfo.gameType(),
                            levelInfo.hardcore(),
                            levelInfo.difficulty(),
                            enabled,
                            levelInfo.gameRules(),
                            levelInfo.getDataConfiguration()
                        ));

                        for (ServerPlayer serverPlayerEntity : server.getPlayerList().getPlayers())
                        {
                            server.getCommands().sendCommands(serverPlayerEntity);
                            ServerNetwork.sendCheatsPermission(serverPlayerEntity, enabled);
                        }
                    }

                    return 1;
                })
            )
        );
    }

    private static void registerBoomCommand(LiteralArgumentBuilder<CommandSourceStack> bbs, Commands.CommandSelection environment, Predicate<CommandSourceStack> hasPermissions)
    {
        bbs.then(
            Commands.literal("boom").requires(hasPermissions).then(
                Commands.argument("pos", Vec3Argument.vec3()).then(
                    Commands.argument("radius", FloatArgumentType.floatArg(1)).then(
                        Commands.argument("fire", BoolArgumentType.bool()).executes((ctx) ->
                        {
                            CommandSourceStack source = ctx.getSource();
                            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                            float radius = FloatArgumentType.getFloat(ctx, "radius");
                            boolean fire = BoolArgumentType.getBool(ctx, "fire");

                            source.getLevel().explode(null, pos.x, pos.y, pos.z, radius, fire, Level.ExplosionInteraction.BLOCK);

                            return 1;
                        })
                    )
                )
            )
        );
    }

    /**
     * /bbs morph McHorseYT - demorph (remove morph) player McHorseYT
     */
    private static int morphCommandDemorph(CommandContext<CommandSourceStack> source) throws CommandSyntaxException
    {
        ServerPlayer entity = EntityArgument.getPlayer(source, "target");

        ServerNetwork.sendMorphToTracked(entity, null);
        Morph.getMorph(entity).setForm(null);

        return 1;
    }

    /**
     * /bbs morph McHorse {id:"bbs:model",model:"butterfly",texture:"assets:models/butterfly/yellow.png"}
     *
     * Morphs player McHorseYT into a butterfly model with yellow skin
     */
    private static int morphCommandMorph(CommandContext<CommandSourceStack> source) throws CommandSyntaxException
    {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(source, "target");
        String formData = StringArgumentType.getString(source, "form");

        try
        {
            Form form = FormUtils.fromData(DataToString.mapFromString(formData));

            for (ServerPlayer player : players)
            {
                ServerNetwork.sendMorphToTracked(player, form);
                Morph.getMorph(player).setForm(FormUtils.copy(form));
            }

            return 1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * /bbs film McHorseYT play test - Plays a film (with camera) to McHorseYT
     * /bbs film @a play test false - Plays a film (without camera) to all players
     */
    private static int sceneCommandPlay(CommandContext<CommandSourceStack> source, boolean withCamera) throws CommandSyntaxException
    {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(source, "target");
        String filmId = StringArgumentType.getString(source, "film");

        for (ServerPlayer player : players)
        {
            ServerNetwork.sendPlayFilm(player, filmId, withCamera);
        }

        return 1;
    }

    /**
     * /bbs film McHorseYT stop test - Stops film playback
     */
    private static int sceneCommandStop(CommandContext<CommandSourceStack> source) throws CommandSyntaxException
    {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(source, "target");
        String filmId = StringArgumentType.getString(source, "film");

        BBSMod.getActions().stop(filmId);

        for (ServerPlayer player : players)
        {
            ServerNetwork.sendStopFilm(player, filmId);
        }

        return 1;
    }

    private static int DCCommandShutdown(CommandContext<CommandSourceStack> source)
    {
        BBSMod.getActions().resetDamage(source.getSource().getLevel());

        return 1;
    }

    private static int DCCommandStart(CommandContext<CommandSourceStack> source)
    {
        BBSMod.getActions().trackDamage(source.getSource().getLevel());

        return 1;
    }

    private static int DCCommandStop(CommandContext<CommandSourceStack> source)
    {
        BBSMod.getActions().stopDamage(source.getSource().getLevel());

        return 1;
    }

    private static int onHead(CommandContext<CommandSourceStack> source)
    {
        if (source.getSource().getEntity() instanceof LivingEntity livingEntity)
        {
            ItemStack stack = livingEntity.getItemBySlot(EquipmentSlot.MAINHAND);

            if (!stack.isEmpty())
            {
                livingEntity.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            }
        }

        return 1;
    }

    private static int saveStructure(CommandContext<CommandSourceStack> source)
    {
        String name = StringArgumentType.getString(source, "name");
        BlockPos from = BlockPosArgument.getBlockPos(source, "from");
        BlockPos to = BlockPosArgument.getBlockPos(source, "to");

        ServerLevel world = source.getSource().getLevel();
        StructureTemplateManager structureTemplateManager = world.getStructureManager();
        StructureTemplate structureTemplate;

        try
        {
            structureTemplate = structureTemplateManager.getOrCreate(Identifier.parse(name));
        }
        catch (IdentifierException e)
        {
            return 0;
        }

        BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
        BlockPos size = max.subtract(min).offset(1, 1, 1);

        structureTemplate.fillFromWorld(world, min, size, true, List.of(Blocks.STRUCTURE_VOID));

        try
        {
            if (structureTemplateManager.save(Identifier.parse(name)))
            {
                return 1;
            }
        }
        catch (IdentifierException var7)
        {}

        return 0;
    }
}
