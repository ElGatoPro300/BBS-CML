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
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_151;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_1940;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2186;
import net.minecraft.class_2246;
import net.minecraft.class_2262;
import net.minecraft.class_2267;
import net.minecraft.class_2277;
import net.minecraft.class_2300;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2586;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_3485;
import net.minecraft.class_3499;
import net.minecraft.class_5219;
import net.minecraft.class_7157;
import net.minecraft.server.MinecraftServer;
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
import java.util.function.Predicate;

public class BBSCommands
{
    public static void register(CommandDispatcher<class_2168> dispatcher, class_7157 registryAccess, class_2170.class_5364 environment)
    {
        Predicate<class_2168> hasPermissions = (source) -> source.method_9259(2);
        LiteralArgumentBuilder<class_2168> bbs = class_2170.method_9247("bbs").requires((source) -> true);

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

    private static void registerStructureSaveCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> structures = class_2170.method_9247("structures");
        LiteralArgumentBuilder<class_2168> save = class_2170.method_9247("save");
        RequiredArgumentBuilder<class_2168, String> name = class_2170.method_9244("name", StringArgumentType.word());
        RequiredArgumentBuilder<class_2168, class_2267> from = class_2170.method_9244("from", class_2262.method_9698());
        RequiredArgumentBuilder<class_2168, class_2267> to = class_2170.method_9244("to", class_2262.method_9698());

        bbs.then(structures
            .then(save.then(name.then(from.then(to
                .executes(BBSCommands::saveStructure))))
        ));
    }

    private static void registerMorphCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> morph = class_2170.method_9247("morph");
        RequiredArgumentBuilder<class_2168, class_2300> target = class_2170.method_9244("target", class_2186.method_9308());
        RequiredArgumentBuilder<class_2168, String> form = class_2170.method_9244("form", StringArgumentType.greedyString());

        morph.then(target
            .executes(BBSCommands::morphCommandDemorph)
            .then(form.executes(BBSCommands::morphCommandMorph)));

        bbs.then(morph.requires(hasPermissions));
    }

    private static void registerModelBlockCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> modelBlock = class_2170.method_9247("model_block");
        LiteralArgumentBuilder<class_2168> playState = class_2170.method_9247("play_state");
        RequiredArgumentBuilder<class_2168, class_2267> coords = class_2170.method_9244("coords", class_2262.method_9698());
        RequiredArgumentBuilder<class_2168, String> state = class_2170.method_9244("state", StringArgumentType.string());

        LiteralArgumentBuilder<class_2168> refresh = class_2170.method_9247("refresh");
        RequiredArgumentBuilder<class_2168, Integer> randomRange = class_2170.method_9244("random_range", IntegerArgumentType.integer());

        state.suggests((ctx, builder) ->
        {
            class_2338 pos = class_2262.method_48299(ctx, "coords");
            class_2586 blockEntity = ctx.getSource().method_9225().method_8321(pos);

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
                        class_2338 pos = class_2262.method_48299(ctx, "coords");
                        String animationState = StringArgumentType.getString(ctx, "state");
                        class_2586 blockEntity = ctx.getSource().method_9225().method_8321(pos);

                        if (blockEntity instanceof ModelBlockEntity)
                        {
                            for (class_3222 player : ctx.getSource().method_9225().method_18456())
                            {
                                if (player.method_24515().method_10262(pos) <= (Math.pow(BBSSettings.modelBlockAnimationStateDistance.get(), 2)))
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

                    for (class_3222 player : ctx.getSource().method_9211().method_3760().method_14571())
                    {
                        ServerNetwork.sendReloadModelBlocks(player, range);
                    }

                    return 1;
                })
            )
        );

        bbs.then(modelBlock.requires(hasPermissions));
    }

    private static void registerMorphEntityCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> morph = class_2170.method_9247("morph_entity");

        morph.executes((source) ->
        {
            class_1297 entity = source.getSource().method_9228();

            if (entity instanceof class_3222 player)
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

    private static void registerFilmsCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> scene = class_2170.method_9247("films");
        LiteralArgumentBuilder<class_2168> play = class_2170.method_9247("play");
        LiteralArgumentBuilder<class_2168> stop = class_2170.method_9247("stop");
        RequiredArgumentBuilder<class_2168, class_2300> target = class_2170.method_9244("target", class_2186.method_9308());
        RequiredArgumentBuilder<class_2168, String> playFilm = class_2170.method_9244("film", StringArgumentType.string());
        RequiredArgumentBuilder<class_2168, String> stopFilm = class_2170.method_9244("film", StringArgumentType.string());
        RequiredArgumentBuilder<class_2168, Boolean> camera = class_2170.method_9244("camera", BoolArgumentType.bool());

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

    private static void registerDCCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> dc = class_2170.method_9247("dc");
        LiteralArgumentBuilder<class_2168> shutdown = class_2170.method_9247("shutdown");
        LiteralArgumentBuilder<class_2168> start = class_2170.method_9247("start");
        LiteralArgumentBuilder<class_2168> stop = class_2170.method_9247("stop");

        bbs.then(
            dc.requires(hasPermissions).then(start.executes(BBSCommands::DCCommandStart))
                .then(stop.executes(BBSCommands::DCCommandStop))
                .then(shutdown.executes(BBSCommands::DCCommandShutdown))
        );
    }

    private static void registerOnHeadCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> onHead = class_2170.method_9247("on_head");

        bbs.then(onHead.requires(hasPermissions).executes(BBSCommands::onHead));
    }

    private static void registerConfigCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        LiteralArgumentBuilder<class_2168> config = class_2170.method_9247("config");

        config.requires((ctx) -> ctx.method_9259(4)).then(
            class_2170.method_9247("set").then(
                class_2170.method_9244("option", StringArgumentType.word())
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
                        class_2170.method_9244("value", StringArgumentType.greedyString()).executes((ctx) ->
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

    private static void registerCheatsCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment)
    {
        if (environment.field_25423)
        {
            return;
        }

        bbs.then(
            class_2170.method_9247("cheats").then(
                class_2170.method_9244("enabled", BoolArgumentType.bool()).executes((ctx) ->
                {
                    MinecraftServer server = ctx.getSource().method_9211();
                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                    class_5219 saveProperties = server.method_27728();

                    if (saveProperties instanceof LevelPropertiesAccessor accessor)
                    {
                        class_1940 levelInfo = saveProperties.method_27433();

                        accessor.bbs$setLevelInfo(new class_1940(levelInfo.method_27339(),
                            levelInfo.method_8574(),
                            levelInfo.method_8583(),
                            levelInfo.method_27340(),
                            enabled,
                            levelInfo.method_27341(),
                            levelInfo.method_29558()
                        ));

                        for (class_3222 serverPlayerEntity : server.method_3760().method_14571())
                        {
                            server.method_3734().method_9241(serverPlayerEntity);
                            ServerNetwork.sendCheatsPermission(serverPlayerEntity, enabled);
                        }
                    }

                    return 1;
                })
            )
        );
    }

    private static void registerBoomCommand(LiteralArgumentBuilder<class_2168> bbs, class_2170.class_5364 environment, Predicate<class_2168> hasPermissions)
    {
        bbs.then(
            class_2170.method_9247("boom").requires(hasPermissions).then(
                class_2170.method_9244("pos", class_2277.method_9737()).then(
                    class_2170.method_9244("radius", FloatArgumentType.floatArg(1)).then(
                        class_2170.method_9244("fire", BoolArgumentType.bool()).executes((ctx) ->
                        {
                            class_2168 source = ctx.getSource();
                            class_243 pos = class_2277.method_9736(ctx, "pos");
                            float radius = FloatArgumentType.getFloat(ctx, "radius");
                            boolean fire = BoolArgumentType.getBool(ctx, "fire");

                            source.method_9225().method_8537(null, pos.field_1352, pos.field_1351, pos.field_1350, radius, fire, class_1937.class_7867.field_40889);

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
    private static int morphCommandDemorph(CommandContext<class_2168> source) throws CommandSyntaxException
    {
        class_3222 entity = class_2186.method_9315(source, "target");

        ServerNetwork.sendMorphToTracked(entity, null);
        Morph.getMorph(entity).setForm(null);

        return 1;
    }

    /**
     * /bbs morph McHorse {id:"bbs:model",model:"butterfly",texture:"assets:models/butterfly/yellow.png"}
     *
     * Morphs player McHorseYT into a butterfly model with yellow skin
     */
    private static int morphCommandMorph(CommandContext<class_2168> source) throws CommandSyntaxException
    {
        Collection<class_3222> players = class_2186.method_9312(source, "target");
        String formData = StringArgumentType.getString(source, "form");

        try
        {
            Form form = FormUtils.fromData(DataToString.mapFromString(formData));

            for (class_3222 player : players)
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
    private static int sceneCommandPlay(CommandContext<class_2168> source, boolean withCamera) throws CommandSyntaxException
    {
        Collection<class_3222> players = class_2186.method_9312(source, "target");
        String filmId = StringArgumentType.getString(source, "film");

        for (class_3222 player : players)
        {
            ServerNetwork.sendPlayFilm(player, filmId, withCamera);
        }

        return 1;
    }

    /**
     * /bbs film McHorseYT stop test - Stops film playback
     */
    private static int sceneCommandStop(CommandContext<class_2168> source) throws CommandSyntaxException
    {
        Collection<class_3222> players = class_2186.method_9312(source, "target");
        String filmId = StringArgumentType.getString(source, "film");

        BBSMod.getActions().stop(filmId);

        for (class_3222 player : players)
        {
            ServerNetwork.sendStopFilm(player, filmId);
        }

        return 1;
    }

    private static int DCCommandShutdown(CommandContext<class_2168> source)
    {
        BBSMod.getActions().resetDamage(source.getSource().method_9225());

        return 1;
    }

    private static int DCCommandStart(CommandContext<class_2168> source)
    {
        BBSMod.getActions().trackDamage(source.getSource().method_9225());

        return 1;
    }

    private static int DCCommandStop(CommandContext<class_2168> source)
    {
        BBSMod.getActions().stopDamage(source.getSource().method_9225());

        return 1;
    }

    private static int onHead(CommandContext<class_2168> source)
    {
        if (source.getSource().method_9228() instanceof class_1309 livingEntity)
        {
            class_1799 stack = livingEntity.method_6118(class_1304.field_6173);

            if (!stack.method_7960())
            {
                livingEntity.method_5673(class_1304.field_6169, stack.method_7972());
            }
        }

        return 1;
    }

    private static int saveStructure(CommandContext<class_2168> source)
    {
        String name = StringArgumentType.getString(source, "name");
        class_2338 from = class_2262.method_48299(source, "from");
        class_2338 to = class_2262.method_48299(source, "to");

        class_3218 world = source.getSource().method_9225();
        class_3485 structureTemplateManager = world.method_14183();
        class_3499 structureTemplate;

        try
        {
            structureTemplate = structureTemplateManager.method_15091(class_2960.method_60654(name));
        }
        catch (class_151 e)
        {
            return 0;
        }

        class_2338 min = new class_2338(Math.min(from.method_10263(), to.method_10263()), Math.min(from.method_10264(), to.method_10264()), Math.min(from.method_10260(), to.method_10260()));
        class_2338 max = new class_2338(Math.max(from.method_10263(), to.method_10263()), Math.max(from.method_10264(), to.method_10264()), Math.max(from.method_10260(), to.method_10260()));
        class_2338 size = max.method_10059(min).method_10069(1, 1, 1);

        structureTemplate.method_15174(world, min, size, true, class_2246.field_10369);

        try
        {
            if (structureTemplateManager.method_15093(class_2960.method_60654(name)))
            {
                return 1;
            }
        }
        catch (class_151 var7)
        {}

        return 0;
    }
}
