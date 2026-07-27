package mchorse.bbs_mod.network;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.actions.ActionRecorder;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.actions.PlayerType;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.EnumUtils;
import mchorse.bbs_mod.utils.PermissionUtils;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.repos.RepositoryOperation;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1934;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2540;
import net.minecraft.class_2586;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9139;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerNetwork
{
    public static final int STATE_TRIGGER_MORPH = 0;
    public static final int STATE_TRIGGER_MAIN_HAND_ITEM = 1;
    public static final int STATE_TRIGGER_OFF_HAND_ITEM = 2;

    public static final class_2960 CLIENT_CLICKED_MODEL_BLOCK_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "c1");
    public static final class_2960 CLIENT_PLAYER_FORM_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "c2");
    public static final class_2960 CLIENT_PLAY_FILM_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "c3");
    public static final class_2960 CLIENT_MANAGER_DATA_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "c4");
    public static final class_2960 CLIENT_STOP_FILM_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "c5");
    public static final class_2960 CLIENT_HANDSHAKE = class_2960.method_60655(BBSMod.MOD_ID, "c6");
    public static final class_2960 CLIENT_RECORDED_ACTIONS = class_2960.method_60655(BBSMod.MOD_ID, "c7");
    public static final class_2960 CLIENT_ANIMATION_STATE_TRIGGER = class_2960.method_60655(BBSMod.MOD_ID, "c8");
    public static final class_2960 CLIENT_CHEATS_PERMISSION = class_2960.method_60655(BBSMod.MOD_ID, "c9");
    public static final class_2960 CLIENT_SHARED_FORM = class_2960.method_60655(BBSMod.MOD_ID, "c10");
    public static final class_2960 CLIENT_ENTITY_FORM = class_2960.method_60655(BBSMod.MOD_ID, "c11");
    public static final class_2960 CLIENT_ACTORS = class_2960.method_60655(BBSMod.MOD_ID, "c12");
    public static final class_2960 CLIENT_GUN_PROPERTIES = class_2960.method_60655(BBSMod.MOD_ID, "c13");
    public static final class_2960 CLIENT_PAUSE_FILM = class_2960.method_60655(BBSMod.MOD_ID, "c14");
    public static final class_2960 CLIENT_SELECTED_SLOT = class_2960.method_60655(BBSMod.MOD_ID, "c15");
    public static final class_2960 CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER = class_2960.method_60655(BBSMod.MOD_ID, "c16");
    public static final class_2960 CLIENT_REFRESH_MODEL_BLOCKS = class_2960.method_60655(BBSMod.MOD_ID, "c17");
    public static final class_2960 CLIENT_CLICKED_TRIGGER_BLOCK_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "c18");
    public static final class_2960 CLIENT_BAY4LLY_SKIN = class_2960.method_60655(BBSMod.MOD_ID, "c19");

    public static final class_2960 SERVER_MODEL_BLOCK_FORM_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "s1");
    public static final class_2960 SERVER_MODEL_BLOCK_TRANSFORMS_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "s2");
    public static final class_2960 SERVER_PLAYER_FORM_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "s3");
    public static final class_2960 SERVER_MANAGER_DATA_PACKET = class_2960.method_60655(BBSMod.MOD_ID, "s4");
    public static final class_2960 SERVER_ACTION_RECORDING = class_2960.method_60655(BBSMod.MOD_ID, "s5");
    public static final class_2960 SERVER_TOGGLE_FILM = class_2960.method_60655(BBSMod.MOD_ID, "s6");
    public static final class_2960 SERVER_ACTION_CONTROL = class_2960.method_60655(BBSMod.MOD_ID, "s7");
    public static final class_2960 SERVER_FILM_DATA_SYNC = class_2960.method_60655(BBSMod.MOD_ID, "s8");
    public static final class_2960 SERVER_PLAYER_TP = class_2960.method_60655(BBSMod.MOD_ID, "s9");
    public static final class_2960 SERVER_ANIMATION_STATE_TRIGGER = class_2960.method_60655(BBSMod.MOD_ID, "s10");
    public static final class_2960 SERVER_SHARED_FORM = class_2960.method_60655(BBSMod.MOD_ID, "s11");
    public static final class_2960 SERVER_ZOOM = class_2960.method_60655(BBSMod.MOD_ID, "s12");
    public static final class_2960 SERVER_PAUSE_FILM = class_2960.method_60655(BBSMod.MOD_ID, "s13");
    public static final class_2960 SERVER_TRIGGER_BLOCK_UPDATE = class_2960.method_60655(BBSMod.MOD_ID, "s14");
    public static final class_2960 SERVER_TRIGGER_BLOCK_CLICK = class_2960.method_60655(BBSMod.MOD_ID, "s15");
    public static final class_2960 SERVER_SET_GAME_MODE = class_2960.method_60655(BBSMod.MOD_ID, "s16");

    private static ServerPacketCrusher crusher = new ServerPacketCrusher();

    public static class_8710.class_9154<BufPayload> idFor(class_2960 identifier)
    {
        return new class_8710.class_9154<>(identifier);
    }

    public record BufPayload(byte[] data, class_8710.class_9154<BufPayload> id) implements class_8710
    {
        public static BufPayload from(class_2540 buf, class_8710.class_9154<BufPayload> id)
        {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.method_52979(bytes);
            return new BufPayload(bytes, id);
        }

        public class_2540 asPacketByteBuf()
        {
            class_2540 out = PacketByteBufs.create();
            out.method_52983(this.data);
            return out;
        }

        @Override
        public class_8710.class_9154<? extends class_8710> method_56479()
        {
            return id;
        }

        public static class_9139<class_9129, BufPayload> codecFor(class_8710.class_9154<BufPayload> id)
        {
            return new class_9139<class_9129, BufPayload>()
            {
                @Override
                public BufPayload decode(class_9129 byteBuf)
                {
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.method_52979(bytes);
                    return new BufPayload(bytes, id);
                }

                @Override
                public void encode(class_9129 byteBuf, BufPayload payload)
                {
                    byteBuf.method_52983(payload.data);
                }
            };
        }
    }

    public static void reset()
    {
        crusher.reset();
    }

    public static void setup()
    {
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_MODEL_BLOCK_FORM_PACKET), BufPayload.codecFor(idFor(SERVER_MODEL_BLOCK_FORM_PACKET)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_MODEL_BLOCK_TRANSFORMS_PACKET), BufPayload.codecFor(idFor(SERVER_MODEL_BLOCK_TRANSFORMS_PACKET)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_PLAYER_FORM_PACKET), BufPayload.codecFor(idFor(SERVER_PLAYER_FORM_PACKET)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_MANAGER_DATA_PACKET), BufPayload.codecFor(idFor(SERVER_MANAGER_DATA_PACKET)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_ACTION_RECORDING), BufPayload.codecFor(idFor(SERVER_ACTION_RECORDING)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_TOGGLE_FILM), BufPayload.codecFor(idFor(SERVER_TOGGLE_FILM)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_ACTION_CONTROL), BufPayload.codecFor(idFor(SERVER_ACTION_CONTROL)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_FILM_DATA_SYNC), BufPayload.codecFor(idFor(SERVER_FILM_DATA_SYNC)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_PLAYER_TP), BufPayload.codecFor(idFor(SERVER_PLAYER_TP)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_ANIMATION_STATE_TRIGGER), BufPayload.codecFor(idFor(SERVER_ANIMATION_STATE_TRIGGER)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_SHARED_FORM), BufPayload.codecFor(idFor(SERVER_SHARED_FORM)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_ZOOM), BufPayload.codecFor(idFor(SERVER_ZOOM)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_PAUSE_FILM), BufPayload.codecFor(idFor(SERVER_PAUSE_FILM)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_TRIGGER_BLOCK_UPDATE), BufPayload.codecFor(idFor(SERVER_TRIGGER_BLOCK_UPDATE)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_TRIGGER_BLOCK_CLICK), BufPayload.codecFor(idFor(SERVER_TRIGGER_BLOCK_CLICK)));
        PayloadTypeRegistry.playC2S().register(idFor(SERVER_SET_GAME_MODE), BufPayload.codecFor(idFor(SERVER_SET_GAME_MODE)));

        try {
            Class<?> envTypeClass = Class.forName("net.fabricmc.api.EnvType");
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Object envType = loaderClass.getMethod("getEnvironmentType").invoke(loader);
            Object serverEnum = envTypeClass.getField("SERVER").get(null);

            if (envType == serverEnum) {
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_CLICKED_MODEL_BLOCK_PACKET), BufPayload.codecFor(idFor(CLIENT_CLICKED_MODEL_BLOCK_PACKET)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_PLAYER_FORM_PACKET), BufPayload.codecFor(idFor(CLIENT_PLAYER_FORM_PACKET)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_PLAY_FILM_PACKET), BufPayload.codecFor(idFor(CLIENT_PLAY_FILM_PACKET)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_MANAGER_DATA_PACKET), BufPayload.codecFor(idFor(CLIENT_MANAGER_DATA_PACKET)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_STOP_FILM_PACKET), BufPayload.codecFor(idFor(CLIENT_STOP_FILM_PACKET)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_HANDSHAKE), BufPayload.codecFor(idFor(CLIENT_HANDSHAKE)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_RECORDED_ACTIONS), BufPayload.codecFor(idFor(CLIENT_RECORDED_ACTIONS)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_ANIMATION_STATE_TRIGGER), BufPayload.codecFor(idFor(CLIENT_ANIMATION_STATE_TRIGGER)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_CHEATS_PERMISSION), BufPayload.codecFor(idFor(CLIENT_CHEATS_PERMISSION)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_SHARED_FORM), BufPayload.codecFor(idFor(CLIENT_SHARED_FORM)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_ENTITY_FORM), BufPayload.codecFor(idFor(CLIENT_ENTITY_FORM)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_ACTORS), BufPayload.codecFor(idFor(CLIENT_ACTORS)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_GUN_PROPERTIES), BufPayload.codecFor(idFor(CLIENT_GUN_PROPERTIES)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_PAUSE_FILM), BufPayload.codecFor(idFor(CLIENT_PAUSE_FILM)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_SELECTED_SLOT), BufPayload.codecFor(idFor(CLIENT_SELECTED_SLOT)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER), BufPayload.codecFor(idFor(CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_REFRESH_MODEL_BLOCKS), BufPayload.codecFor(idFor(CLIENT_REFRESH_MODEL_BLOCKS)));
                PayloadTypeRegistry.playS2C().register(idFor(CLIENT_CLICKED_TRIGGER_BLOCK_PACKET), BufPayload.codecFor(idFor(CLIENT_CLICKED_TRIGGER_BLOCK_PACKET)));
            }
        } catch (Throwable t) {
        }

        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_MODEL_BLOCK_FORM_PACKET), (payload, context) -> handleModelBlockFormPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_MODEL_BLOCK_TRANSFORMS_PACKET), (payload, context) -> handleModelBlockTransformsPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_PLAYER_FORM_PACKET), (payload, context) -> handlePlayerFormPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_MANAGER_DATA_PACKET), (payload, context) -> handleManagerDataPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_ACTION_RECORDING), (payload, context) -> handleActionRecording(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_TOGGLE_FILM), (payload, context) -> handleToggleFilm(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_ACTION_CONTROL), (payload, context) -> handleActionControl(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_FILM_DATA_SYNC), (payload, context) -> handleSyncData(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_PLAYER_TP), (payload, context) -> handleTeleportPlayer(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_ANIMATION_STATE_TRIGGER), (payload, context) -> handleAnimationStateTriggerPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_SHARED_FORM), (payload, context) -> handleSharedFormPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_ZOOM), (payload, context) -> handleZoomPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_PAUSE_FILM), (payload, context) -> handlePauseFilmPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_TRIGGER_BLOCK_UPDATE), (payload, context) -> handleTriggerBlockUpdatePacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_TRIGGER_BLOCK_CLICK), (payload, context) -> handleTriggerBlockClickPacket(context.server(), context.player(), payload.asPacketByteBuf()));
        ServerPlayNetworking.registerGlobalReceiver(idFor(SERVER_SET_GAME_MODE), (payload, context) -> handleSetGameModePacket(context.server(), context.player(), payload.asPacketByteBuf()));
    }

    /* Handlers */

    private static void handleModelBlockFormPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            class_2338 pos = packetByteBuf.method_10811();

            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    class_1937 world = player.method_37908();
                    class_2586 be = world.method_8321(pos);

                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.updateForm(data, world);
                    }
                });
            }
            catch (Exception e)
            {}
        });
    }

    private static void handleTriggerBlockUpdatePacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            class_2338 pos = packetByteBuf.method_10811();

            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    class_1937 world = player.method_37908();
                    class_2586 be = world.method_8321(pos);

                    if (be instanceof TriggerBlockEntity trigger)
                    {
                        if (data.has("left")) trigger.left.fromData(data.getList("left"));
                        if (data.has("right")) trigger.right.fromData(data.getList("right"));
                        if (data.has("enter")) trigger.enter.fromData(data.getList("enter"));
                        if (data.has("exit")) trigger.exit.fromData(data.getList("exit"));
                        if (data.has("whileIn")) trigger.whileIn.fromData(data.getList("whileIn"));
                        if (data.has("regionDelay")) trigger.regionDelay.set(data.getInt("regionDelay"));
                        if (data.has("pos1")) trigger.pos1.fromData(data.getList("pos1"));
                        if (data.has("pos2")) trigger.pos2.fromData(data.getList("pos2"));
                        if (data.has("regionOffset")) trigger.regionOffset.fromData(data.getList("regionOffset"));
                        if (data.has("regionSize")) trigger.regionSize.fromData(data.getList("regionSize"));
                        if (data.has("collidable")) trigger.collidable.set(data.getBool("collidable"));
                        if (data.has("region")) trigger.region.set(data.getBool("region"));

                        trigger.method_5431();
                        world.method_8413(pos, world.method_8320(pos), world.method_8320(pos), 3);
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private static void handleTriggerBlockClickPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        class_2338 pos = buf.method_10811();

        server.execute(() ->
        {
            class_1937 world = player.method_37908();
            class_2586 be = world.method_8321(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                trigger.trigger(player, false);
            }
        });
    }

    /**
     * Silent gamemode change for Film / Model Block editors (no chat feedback).
     */
    private static void handleSetGameModePacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        int modeId = buf.method_10816();

        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        class_1934 mode = class_1934.method_8384(modeId);

        if (mode == null)
        {
            return;
        }

        server.execute(() ->
        {
            if (player.field_13974.method_14257() != mode)
            {
                player.method_7336(mode);
            }
        });
    }

    private static void handleModelBlockTransformsPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    class_1799 stack = player.method_6118(class_1304.field_6173).method_7972();

                    if (stack.method_7909() == BBSMod.MODEL_BLOCK_ITEM)
                    {
                        class_9279 beComponent = stack.method_57824(class_9334.field_49611);
                        class_2487 beNbt = beComponent != null ? beComponent.method_57463() : new class_2487();

                        beNbt.method_10566("Properties", DataStorageUtils.toNbt(data));
                        stack.method_57379(class_9334.field_49611, class_9279.method_57456(beNbt));
                    }
                    else if (stack.method_7909() == BBSMod.GUN_ITEM)
                    {
                        class_9279 customComponent = stack.method_57824(class_9334.field_49628);
                        class_2487 customNbt = customComponent != null ? customComponent.method_57463() : new class_2487();

                        customNbt.method_10566("GunData", DataStorageUtils.toNbt(data));
                        stack.method_57379(class_9334.field_49628, class_9279.method_57456(customNbt));
                    }

                    player.method_5673(class_1304.field_6173, stack);
                });
            }
            catch (Exception e)
            {}
        });
    }

    private static void handlePlayerFormPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            Form form = null;

            try
            {
                if (DataStorageUtils.readFromBytes(bytes) instanceof MapType data)
                {
                    form = BBSMod.getForms().fromData(data);
                }
            }
            catch (Exception e)
            {}

            final Form finalForm = form;

            server.execute(() ->
            {
                Morph.getMorph(player).setForm(FormUtils.copy(finalForm));

                sendMorphToTracked(player, finalForm);
            });
        });
    }

    private static void handleManagerDataPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);
            int callbackId = packetByteBuf.readInt();
            RepositoryOperation op = RepositoryOperation.values()[packetByteBuf.readInt()];
            FilmManager films = BBSMod.getFilms();

            if (op == RepositoryOperation.LOAD)
            {
                String id = data.getString("id");
                Film film = films.load(id);

                 if (film != null)
                {
                    sendManagerData(player, callbackId, op, film.toData());
                }

            }
            else if (op == RepositoryOperation.SAVE)
            {
                films.save(data.getString("id"), data.getMap("data"));
            }
            else if (op == RepositoryOperation.RENAME)
            {
                films.rename(data.getString("from"), data.getString("to"));
            }
            else if (op == RepositoryOperation.DELETE)
            {
                films.delete(data.getString("id"));
            }
            else if (op == RepositoryOperation.KEYS)
            {
                ListType list = DataStorageUtils.stringListToData(films.getKeys());

                sendManagerData(player, callbackId, op, list);
            }
            else if (op == RepositoryOperation.ADD_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.addFolder(data.getString("folder"))));
            }
            else if (op == RepositoryOperation.RENAME_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.renameFolder(data.getString("from"), data.getString("to"))));
            }
            else if (op == RepositoryOperation.DELETE_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.deleteFolder(data.getString("folder"))));
            }
        });
    }

    private static void handleActionRecording(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        String filmId = buf.method_19772();
        int replayId = buf.readInt();
        int tick = buf.readInt();
        int countdown = buf.readInt();
        boolean recording = buf.readBoolean();

        server.execute(() ->
        {
            if (recording)
            {
                Film film = BBSMod.getFilms().load(filmId);

                if (film != null)
                {
                    BBSMod.getActions().startRecording(film, player, 0, countdown, replayId);
                }
            }
            else
            {
                ActionRecorder recorder = BBSMod.getActions().stopRecording(player);
                Clips clips = recorder.composeClips();

                /* Send recorded clips to the client */
                sendRecordedActions(player, filmId, replayId, tick, clips);
            }
        });
    }

    private static void handleToggleFilm(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        String filmId = buf.method_19772();
        boolean withCamera = buf.readBoolean();

        server.execute(() ->
        {
            ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(filmId);

            if (actionPlayer != null)
            {
                BBSMod.getActions().stop(filmId);

                for (class_3222 otherPlayer : server.method_3760().method_14571())
                {
                    sendStopFilm(otherPlayer, filmId);
                }
            }
            else
            {
                sendPlayFilm(player, player.method_51469(), filmId, withCamera);
            }
        });
    }

    private static void handleActionControl(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        ActionManager actions = BBSMod.getActions();
        String filmId = buf.method_19772();
        ActionState state = EnumUtils.getValue(buf.readByte(), ActionState.values(), ActionState.STOP);
        int tick = buf.readInt();

        server.execute(() ->
        {
            if (state == ActionState.SEEK)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                }
            }
            else if (state == ActionState.PLAY)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                    actionPlayer.playing = true;
                }
            }
            else if (state == ActionState.PAUSE)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                    actionPlayer.playing = false;
                }
            }
            else if (state == ActionState.RESTART)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer == null)
                {
                    FilmManager films = BBSMod.getFilms();
                    Film film = (filmId != null && !filmId.isBlank() && films.exists(filmId)) ? films.load(filmId) : null;

                    if (film != null)
                    {
                        actionPlayer = actions.play(player, player.method_51469(), film, tick, PlayerType.FILM_EDITOR);
                    }
                }
                else
                {
                    actions.stop(filmId);

                    actionPlayer = actions.play(player, player.method_51469(), actionPlayer.film, tick, PlayerType.FILM_EDITOR);
                }

                if (actionPlayer != null)
                {
                    actionPlayer.syncing = true;
                    actionPlayer.playing = false;

                    if (tick != 0)
                    {
                        actionPlayer.goTo(0, tick);
                    }
                }

                sendStopFilm(player, filmId);
            }
            else if (state == ActionState.RESTORE)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);
                class_3218 world = actionPlayer != null ? actionPlayer.getWorld() : player.method_51469();

                actions.restoreDamage(world);
            }
            else if (state == ActionState.STOP)
            {
                actions.stop(filmId);
            }
        });
    }

    private static void handleSyncData(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            String filmId = packetByteBuf.method_19772();
            List<String> path = new ArrayList<>();

            for (int i = 0, c = buf.readInt(); i < c; i++)
            {
                path.add(buf.method_19772());
            }

            BaseType data = DataStorageUtils.readFromBytes(bytes);

            server.execute(() ->
            {
                BBSMod.getActions().syncData(filmId, new DataPath(path), data);
            });
        });
    }

    private static void handleTeleportPlayer(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yaw = buf.readFloat();
        float bodyYaw = buf.readFloat();
        float pitch = buf.readFloat();

        server.execute(() ->
        {
            player.method_5859(x, y, z);

            player.method_36456(yaw);
            player.method_5847(yaw);
            player.method_5636(bodyYaw);
            player.method_36457(pitch);
        });
    }

    private static void handleAnimationStateTriggerPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        String string = buf.method_19772();
        int type = buf.readInt();
        class_2540 newBuf = PacketByteBufs.create();

        newBuf.method_53002(player.method_5628());
        newBuf.method_10814(string);
        newBuf.method_53002(type);

        BufPayload payload = BufPayload.from(newBuf, idFor(CLIENT_ANIMATION_STATE_TRIGGER));

        for (class_3222 otherPlayer : PlayerLookup.tracking(player))
        {
            ServerPlayNetworking.send(otherPlayer, payload);
        }

        server.execute(() ->
        {
            /* TODO: State Triggers */
        });
    }

    private static void handleSharedFormPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            UUID playerUuid = packetByteBuf.method_10790();
            MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

            server.execute(() ->
            {
                class_3222 otherPlayer = server.method_3760().method_14602(playerUuid);

                if (otherPlayer != null)
                {
                    sendSharedForm(otherPlayer, data);
                }
            });
        });
    }

    private static void handleZoomPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        boolean zoom = buf.readBoolean();
        class_1799 main = player.method_6047();

        if (main.method_7909() == BBSMod.GUN_ITEM)
        {
            GunProperties properties = GunProperties.get(main);
            String command = zoom ? properties.cmdZoomOn : properties.cmdZoomOff;

            if (!command.isEmpty())
            {
                server.method_3734().method_44252(player.method_5671(), command);
            }
        }
    }

    private static void handlePauseFilmPacket(MinecraftServer server, class_3222 player, class_2540 buf)
    {
        String filmId = buf.method_19772();

        ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(filmId);

        if (actionPlayer != null)
        {
            actionPlayer.toggle();
        }

        for (class_3222 playerEntity : server.method_3760().method_14571())
        {
            sendPauseFilm(playerEntity, filmId);
        }
    }

    /* API */

    public static void sendMorph(class_3222 player, int playerId, Form form)
    {
        crusher.send(player, CLIENT_PLAYER_FORM_PACKET, FormUtils.toData(form), (packetByteBuf) ->
        {
            packetByteBuf.method_53002(playerId);
        });
    }

    public static void sendMorphToTracked(class_3222 player, Form form)
    {
        sendMorph(player, player.method_5628(), form);

        for (class_3222 otherPlayer : PlayerLookup.tracking(player))
        {
            sendMorph(otherPlayer, player.method_5628(), form);
        }
    }

    public static void sendClickedModelBlock(class_3222 player, class_2338 pos)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10807(pos);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_CLICKED_MODEL_BLOCK_PACKET)));
    }

    public static void sendClickedTriggerBlock(class_3222 player, class_2338 pos)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10807(pos);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_CLICKED_TRIGGER_BLOCK_PACKET)));
    }

    public static void sendPlayFilm(class_3222 player, class_3218 world, String filmId, boolean withCamera)
    {
        try
        {
            Film film = BBSMod.getFilms().load(filmId);

            if (film != null)
            {
                BBSMod.getActions().play(player, world, film, 0);

                BaseType data = film.toData();

                crusher.send(world.method_18456().stream().map((p) -> (class_1657) p).toList(), CLIENT_PLAY_FILM_PACKET, data, (packetByteBuf) ->
                {
                    packetByteBuf.method_10814(filmId);
                    packetByteBuf.method_52964(withCamera);
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPlayFilm(class_3222 player, String filmId, boolean withCamera)
    {
        try
        {
            Film film = BBSMod.getFilms().load(filmId);

            if (film != null)
            {
                BBSMod.getActions().play(player, player.method_51469(), film, 0);

                crusher.send(player, CLIENT_PLAY_FILM_PACKET, film.toData(), (packetByteBuf) ->
                {
                    packetByteBuf.method_10814(filmId);
                    packetByteBuf.method_52964(withCamera);
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendStopFilm(class_3222 player, String filmId)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_STOP_FILM_PACKET)));
    }

    public static void sendManagerData(class_3222 player, int callbackId, RepositoryOperation op, BaseType data)
    {
        crusher.send(player, CLIENT_MANAGER_DATA_PACKET, data, (packetByteBuf) ->
        {
            packetByteBuf.method_53002(callbackId);
            packetByteBuf.method_53002(op.ordinal());
        });
    }

    public static void sendRecordedActions(class_3222 player, String filmId, int replayId, int tick, Clips clips)
    {
        crusher.send(player, CLIENT_RECORDED_ACTIONS, clips.toData(), (packetByteBuf) ->
        {
            packetByteBuf.method_10814(filmId);
            packetByteBuf.method_53002(replayId);
            packetByteBuf.method_53002(tick);
        });
    }

    public static void sendHandshake(MinecraftServer server, PacketSender packetSender)
    {
        packetSender.sendPacket(BufPayload.from(createHandshakeBuf(server), idFor(CLIENT_HANDSHAKE)));
    }

    public static void sendHandshake(MinecraftServer server, class_3222 player)
    {
        ServerPlayNetworking.send(player, BufPayload.from(createHandshakeBuf(server), idFor(CLIENT_HANDSHAKE)));
    }

    private static class_2540 createHandshakeBuf(MinecraftServer server)
    {
        class_2540 buf = PacketByteBufs.create();
        String id = "";

        /* No need to do that in singleplayer */
        if (server.method_3724())
        {
            id = "";
        }

        buf.method_10814(id);

        return buf;
    }

    public static void sendCheatsPermission(class_3222 player, boolean cheats)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_52964(cheats);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_CHEATS_PERMISSION)));
    }

    public static void sendSharedForm(class_3222 player, MapType data)
    {
        crusher.send(player, CLIENT_SHARED_FORM, data, (packetByteBuf) ->
        {});
    }

    public static void sendEntityForm(class_3222 player, IEntityFormProvider actor)
    {
        crusher.send(player, CLIENT_ENTITY_FORM, FormUtils.toData(actor.getForm()), (packetByteBuf) ->
        {
            packetByteBuf.method_53002(actor.getEntityId());
        });
    }

    public static void sendActors(class_3222 player, String filmId, Map<String, class_1309> actors)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);
        buf.method_53002(actors.size());

        for (Map.Entry<String, class_1309> entry : actors.entrySet())
        {
            buf.method_10814(entry.getKey());
            buf.method_53002(entry.getValue().method_5628());
        }

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_ACTORS)));
    }

    public static void sendGunProperties(class_3222 player, GunProjectileEntity projectile)
    {
        class_2540 buf = PacketByteBufs.create();
        GunProperties properties = projectile.getProperties();

        buf.method_53002(projectile.getEntityId());
        properties.toNetwork(buf);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_GUN_PROPERTIES)));
    }

    public static void sendPauseFilm(class_3222 player, String filmId)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_PAUSE_FILM)));
    }

    public static void sendSelectedSlot(class_3222 player, int slot)
    {
        player.method_31548().field_7545 = slot;

        class_2540 buf = PacketByteBufs.create();

        buf.method_53002(slot);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_SELECTED_SLOT)));
    }
    
    public static void sendBay4llySkinToAll(MinecraftServer server, byte[] bytes, String playerName)
    {
        List<class_1657> list = new ArrayList<>();
        for (class_3222 p : PlayerLookup.all(server))
        {
            list.add(p);
        }
        crusher.send(list, CLIENT_BAY4LLY_SKIN, bytes, (packetByteBuf) ->
        {
            packetByteBuf.method_10814(playerName);
        });
    }

    public static void sendModelBlockState(class_3222 player, class_2338 pos, String trigger)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10807(pos);
        buf.method_10814(trigger);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER)));
    }

    public static void sendReloadModelBlocks(class_3222 player, int tickRandom)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_53002(tickRandom);

        ServerPlayNetworking.send(player, BufPayload.from(buf, idFor(CLIENT_REFRESH_MODEL_BLOCKS)));
    }
}
