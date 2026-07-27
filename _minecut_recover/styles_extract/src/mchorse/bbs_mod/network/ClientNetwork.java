package mchorse.bbs_mod.network;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.bay4lly.SkinManager;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.ui.triggers.UITriggerBlockPanel;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.repos.RepositoryOperation;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1934;
import net.minecraft.class_2338;
import net.minecraft.class_2540;
import net.minecraft.class_2586;
import net.minecraft.class_310;
import net.minecraft.class_8710;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ClientNetwork
{
    private static int ids = 0;
    private static Map<Integer, Consumer<BaseType>> callbacks = new HashMap<>();
    private static ClientPacketCrusher crusher = new ClientPacketCrusher();

    private static boolean isBBSModOnServer;

    public static void resetHandshake()
    {
        isBBSModOnServer = false;
        crusher.reset();
    }

    public static boolean isIsBBSModOnServer()
    {
        return isBBSModOnServer;
    }

    /* Network */

    public static void setup()
    {
        class_8710.class_9154<ServerNetwork.BufPayload> C_CLICKED_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_CLICKED_MODEL_BLOCK_PACKET);
        class_8710.class_9154<ServerNetwork.BufPayload> C_PLAYER_FORM_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_PLAYER_FORM_PACKET);
        class_8710.class_9154<ServerNetwork.BufPayload> C_BAY4LLY_SKIN = ServerNetwork.idFor(ServerNetwork.CLIENT_BAY4LLY_SKIN);
        class_8710.class_9154<ServerNetwork.BufPayload> C_PLAY_FILM_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_PLAY_FILM_PACKET);
        class_8710.class_9154<ServerNetwork.BufPayload> C_MANAGER_DATA_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_MANAGER_DATA_PACKET);
        class_8710.class_9154<ServerNetwork.BufPayload> C_STOP_FILM_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_STOP_FILM_PACKET);
        class_8710.class_9154<ServerNetwork.BufPayload> C_HANDSHAKE_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_HANDSHAKE);
        class_8710.class_9154<ServerNetwork.BufPayload> C_RECORDED_ACTIONS_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_RECORDED_ACTIONS);
        class_8710.class_9154<ServerNetwork.BufPayload> C_ANIMATION_STATE_TRIGGER_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_ANIMATION_STATE_TRIGGER);
        class_8710.class_9154<ServerNetwork.BufPayload> C_CHEATS_PERMISSION_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_CHEATS_PERMISSION);
        class_8710.class_9154<ServerNetwork.BufPayload> C_SHARED_FORM_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_SHARED_FORM);
        class_8710.class_9154<ServerNetwork.BufPayload> C_ENTITY_FORM_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_ENTITY_FORM);
        class_8710.class_9154<ServerNetwork.BufPayload> C_ACTORS_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_ACTORS);
        class_8710.class_9154<ServerNetwork.BufPayload> C_GUN_PROPERTIES_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_GUN_PROPERTIES);
        class_8710.class_9154<ServerNetwork.BufPayload> C_PAUSE_FILM_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_PAUSE_FILM);
        class_8710.class_9154<ServerNetwork.BufPayload> C_SELECTED_SLOT_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_SELECTED_SLOT);
        class_8710.class_9154<ServerNetwork.BufPayload> C_ANIM_STATE_MB_TRIGGER_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_ANIMATION_STATE_MODEL_BLOCK_TRIGGER);
        class_8710.class_9154<ServerNetwork.BufPayload> C_REFRESH_MODEL_BLOCKS_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_REFRESH_MODEL_BLOCKS);
        class_8710.class_9154<ServerNetwork.BufPayload> C_CLICKED_TRIGGER_BLOCK_ID = ServerNetwork.idFor(ServerNetwork.CLIENT_CLICKED_TRIGGER_BLOCK_PACKET);

        PayloadTypeRegistry.playS2C().register(C_CLICKED_ID, ServerNetwork.BufPayload.codecFor(C_CLICKED_ID));
        PayloadTypeRegistry.playS2C().register(C_PLAYER_FORM_ID, ServerNetwork.BufPayload.codecFor(C_PLAYER_FORM_ID));
        PayloadTypeRegistry.playS2C().register(C_BAY4LLY_SKIN, ServerNetwork.BufPayload.codecFor(C_BAY4LLY_SKIN));
        PayloadTypeRegistry.playS2C().register(C_PLAY_FILM_ID, ServerNetwork.BufPayload.codecFor(C_PLAY_FILM_ID));
        PayloadTypeRegistry.playS2C().register(C_MANAGER_DATA_ID, ServerNetwork.BufPayload.codecFor(C_MANAGER_DATA_ID));
        PayloadTypeRegistry.playS2C().register(C_STOP_FILM_ID, ServerNetwork.BufPayload.codecFor(C_STOP_FILM_ID));
        PayloadTypeRegistry.playS2C().register(C_HANDSHAKE_ID, ServerNetwork.BufPayload.codecFor(C_HANDSHAKE_ID));
        PayloadTypeRegistry.playS2C().register(C_RECORDED_ACTIONS_ID, ServerNetwork.BufPayload.codecFor(C_RECORDED_ACTIONS_ID));
        PayloadTypeRegistry.playS2C().register(C_ANIMATION_STATE_TRIGGER_ID, ServerNetwork.BufPayload.codecFor(C_ANIMATION_STATE_TRIGGER_ID));
        PayloadTypeRegistry.playS2C().register(C_CHEATS_PERMISSION_ID, ServerNetwork.BufPayload.codecFor(C_CHEATS_PERMISSION_ID));
        PayloadTypeRegistry.playS2C().register(C_SHARED_FORM_ID, ServerNetwork.BufPayload.codecFor(C_SHARED_FORM_ID));
        PayloadTypeRegistry.playS2C().register(C_ENTITY_FORM_ID, ServerNetwork.BufPayload.codecFor(C_ENTITY_FORM_ID));
        PayloadTypeRegistry.playS2C().register(C_ACTORS_ID, ServerNetwork.BufPayload.codecFor(C_ACTORS_ID));
        PayloadTypeRegistry.playS2C().register(C_GUN_PROPERTIES_ID, ServerNetwork.BufPayload.codecFor(C_GUN_PROPERTIES_ID));
        PayloadTypeRegistry.playS2C().register(C_PAUSE_FILM_ID, ServerNetwork.BufPayload.codecFor(C_PAUSE_FILM_ID));
        PayloadTypeRegistry.playS2C().register(C_SELECTED_SLOT_ID, ServerNetwork.BufPayload.codecFor(C_SELECTED_SLOT_ID));
        PayloadTypeRegistry.playS2C().register(C_ANIM_STATE_MB_TRIGGER_ID, ServerNetwork.BufPayload.codecFor(C_ANIM_STATE_MB_TRIGGER_ID));
        PayloadTypeRegistry.playS2C().register(C_REFRESH_MODEL_BLOCKS_ID, ServerNetwork.BufPayload.codecFor(C_REFRESH_MODEL_BLOCKS_ID));
        PayloadTypeRegistry.playS2C().register(C_CLICKED_TRIGGER_BLOCK_ID, ServerNetwork.BufPayload.codecFor(C_CLICKED_TRIGGER_BLOCK_ID));

        ClientPlayNetworking.registerGlobalReceiver(C_CLICKED_ID, (payload, context) -> handleClientModelBlockPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_PLAYER_FORM_ID, (payload, context) -> handlePlayerFormPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_BAY4LLY_SKIN, (payload, context) -> handleBay4llySkinPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_PLAY_FILM_ID, (payload, context) -> handlePlayFilmPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_MANAGER_DATA_ID, (payload, context) -> handleManagerDataPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_STOP_FILM_ID, (payload, context) -> handleStopFilmPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_HANDSHAKE_ID, (payload, context) -> handleHandshakePacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_RECORDED_ACTIONS_ID, (payload, context) -> handleRecordedActionsPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_ANIMATION_STATE_TRIGGER_ID, (payload, context) -> handleFormTriggerPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_CHEATS_PERMISSION_ID, (payload, context) -> handleCheatsPermissionPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_SHARED_FORM_ID, (payload, context) -> handleShareFormPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_ENTITY_FORM_ID, (payload, context) -> handleEntityFormPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_ACTORS_ID, (payload, context) -> handleActorsPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_GUN_PROPERTIES_ID, (payload, context) -> handleGunPropertiesPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_PAUSE_FILM_ID, (payload, context) -> handlePauseFilmPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_SELECTED_SLOT_ID, (payload, context) -> handleSelectedSlotPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_ANIM_STATE_MB_TRIGGER_ID, (payload, context) -> handleAnimationStateModelBlockPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_REFRESH_MODEL_BLOCKS_ID, (payload, context) -> handleRefreshModelBlocksPacket(context.client(), payload.asPacketByteBuf()));
        ClientPlayNetworking.registerGlobalReceiver(C_CLICKED_TRIGGER_BLOCK_ID, (payload, context) -> handleClickedTriggerBlockPacket(context.client(), payload.asPacketByteBuf()));
    }

    /* Handlers */

    private static void handleClickedTriggerBlockPacket(class_310 client, class_2540 buf)
    {
        class_2338 pos = buf.method_10811();

        client.execute(() ->
        {
            class_2586 entity = client.field_1687.method_8321(pos);

            if (!(entity instanceof TriggerBlockEntity))
            {
                return;
            }

            UIDashboard dashboard = BBSModClient.getDashboard();

            if (!(client.field_1755 instanceof UIScreen screen) || screen.getMenu() != dashboard)
            {
                UIScreen.open(dashboard);
            }

            UITriggerBlockPanel panel = dashboard.getPanel(UITriggerBlockPanel.class);

            dashboard.setPanel(panel);
            panel.fill((TriggerBlockEntity) entity, true);
        });
    }

    private static void handleClientModelBlockPacket(class_310 client, class_2540 buf)
    {
        class_2338 pos = buf.method_10811();

        client.execute(() ->
        {
            class_2586 entity = client.field_1687.method_8321(pos);

            if (!(entity instanceof ModelBlockEntity))
            {
                return;
            }

            UIBaseMenu menu = UIScreen.getCurrentMenu();
            UIDashboard dashboard = BBSModClient.getDashboard();

            if (menu != dashboard)
            {
                UIScreen.open(dashboard);
            }

            UIModelBlockPanel panel = dashboard.getPanels().getPanel(UIModelBlockPanel.class);

            dashboard.setPanel(panel);
            panel.fill((ModelBlockEntity) entity, true);
        });
    }

    private static void handlePlayerFormPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            int id = packetByteBuf.readInt();
            Form form = FormUtils.fromData(DataStorageUtils.readFromBytes(bytes));

            final Form finalForm = form;

            client.execute(() ->
            {
                class_1297 entity = client.field_1687.method_8469(id);
                Morph morph = Morph.getMorph(entity);

                if (morph != null)
                {
                    morph.setForm(finalForm);
                }
            });
        });
    }

    private static void handlePlayFilmPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            String filmId = packetByteBuf.method_19772();
            boolean withCamera = packetByteBuf.readBoolean();
            Film film = new Film();

            film.setId(filmId);
            film.fromData(DataStorageUtils.readFromBytes(bytes));

            client.execute(() -> Films.playFilm(film, withCamera));
        });
    }

    private static void handleManagerDataPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            int callbackId = packetByteBuf.readInt();
            RepositoryOperation op = RepositoryOperation.values()[packetByteBuf.readInt()];
            BaseType data = DataStorageUtils.readFromBytes(bytes);

            client.execute(() ->
            {
                Consumer<BaseType> callback = callbacks.remove(callbackId);

                if (callback != null)
                {
                    callback.accept(data);
                }
            });
        });
    }

    private static void handleStopFilmPacket(class_310 client, class_2540 buf)
    {
        String filmId = buf.method_19772();

        client.execute(() -> Films.stopFilm(filmId));
    }

    private static void handleHandshakePacket(class_310 client, class_2540 buf)
    {
        isBBSModOnServer = true;
    }

    private static void handleRecordedActionsPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            String filmId = packetByteBuf.method_19772();
            int replayId = packetByteBuf.readInt();
            int tick = packetByteBuf.readInt();
            BaseType data = DataStorageUtils.readFromBytes(bytes);

            client.execute(() ->
            {
                UIDashboard dashboard = BBSModClient.peekDashboard();

                if (dashboard == null)
                {
                    return;
                }

                UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

                if (panel != null)
                {
                    panel.receiveActions(filmId, replayId, tick, data);
                }
            });
        });
    }

    private static void handleFormTriggerPacket(class_310 client, class_2540 buf)
    {
        int id = buf.readInt();
        String triggerId = buf.method_19772();
        int type = buf.readInt();

        client.execute(() ->
        {
            class_1297 entity = client.field_1687.method_8469(id);
            Morph morph = Morph.getMorph(entity);

            if (morph != null && morph.getForm() != null)
            {
                morph.getForm().playState(triggerId);
            }

            if (entity instanceof class_1309 livingEntity && type > 0)
            {
                class_1799 stackInHand = livingEntity.method_5998(type == 1 ? class_1268.field_5808 : class_1268.field_5810);
                ModelProperties properties = BBSModClient.getItemStackProperties(stackInHand);

                if (properties != null && properties.getForm() != null)
                {
                    properties.getForm().playState(triggerId);
                }
            }
        });
    }

    private static void handleCheatsPermissionPacket(class_310 client, class_2540 buf)
    {
        boolean cheats = buf.readBoolean();

        client.execute(() ->
        {
            client.field_1724.method_3147(cheats ? 4 : 0);
        });
    }

    private static void handleShareFormPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            final Form finalForm = FormUtils.fromData(DataStorageUtils.readFromBytes(bytes));

            if (finalForm == null)
            {
                return;
            }

            client.execute(() ->
            {
                UIBaseMenu menu = UIScreen.getCurrentMenu();
                UIDashboard dashboard = BBSModClient.getDashboard();

                dashboard.setPanel(dashboard.getPanel(UIMorphingPanel.class));

                if (menu == null)
                {
                    UIScreen.open(dashboard);
                }

                BBSModClient.getFormCategories().getRecentForms().getCategories().get(0).addForm(finalForm);
                dashboard.context.notifyInfo(UIKeys.FORMS_SHARED_NOTIFICATION.format(finalForm.getDisplayName()));
            });
        });
    }

    private static void handleEntityFormPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            final Form finalForm = FormUtils.fromData(DataStorageUtils.readFromBytes(bytes));

            if (finalForm == null)
            {
                return;
            }

            int entityId = buf.readInt();

            client.execute(() ->
            {
                class_1297 entity = client.field_1687.method_8469(entityId);

                if (entity instanceof IEntityFormProvider provider)
                {
                    provider.setForm(finalForm);
                }
            });
        });
    }

    private static void handleActorsPacket(class_310 client, class_2540 buf)
    {
        Map<String, Integer> actors = new HashMap<>();
        String filmId = buf.method_19772();

        for (int i = 0, c = buf.readInt(); i < c; i++)
        {
            String key = buf.method_19772();
            int entityId = buf.readInt();

            actors.put(key, entityId);
        }

        client.execute(() ->
        {
            UIDashboard dashboard = BBSModClient.peekDashboard();

            if (dashboard == null)
            {
                return;
            }

            UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

            if (panel != null)
            {
                panel.updateActors(filmId, actors);
            }

            BBSModClient.getFilms().updateActors(filmId, actors);
        });
    }

    private static void handleGunPropertiesPacket(class_310 client, class_2540 buf)
    {
        GunProperties properties = new GunProperties();
        int entityId = buf.readInt();

        properties.fromNetwork(buf);

        client.execute(() ->
        {
            class_1297 entity = client.field_1687.method_8469(entityId);

            if (entity instanceof GunProjectileEntity projectile)
            {
                projectile.setProperties(properties);
                projectile.method_18382();
            }
        });
    }

    private static void handlePauseFilmPacket(class_310 client, class_2540 buf)
    {
        String filmId = buf.method_19772();

        client.execute(() ->
        {
            Films.togglePauseFilm(filmId);
        });
    }
    
    private static void handleBay4llySkinPacket(class_310 client, class_2540 buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            String playerName = packetByteBuf.method_19772();
            client.execute(() ->
            {
                try
                {
                    SkinManager.saveSkin(playerName, bytes);
                }
                catch (Exception e)
                {
                }
            });
        });
    }

    private static void handleSelectedSlotPacket(class_310 client, class_2540 buf)
    {
        int slot = buf.readInt();

        client.execute(() ->
        {
            client.field_1724.method_31548().field_7545 = slot;
        });
    }

    private static void handleAnimationStateModelBlockPacket(class_310 client, class_2540 buf)
    {
        class_2338 pos = buf.method_10811();
        String state = buf.method_19772();

        client.execute(() ->
        {
            class_2586 blockEntity = client.field_1687.method_8321(pos);

            if (blockEntity instanceof ModelBlockEntity block)
            {
                if (block.getProperties().getForm() != null)
                {
                    block.getProperties().getForm().playState(state);
                }
            }
        });
    }

    private static void handleRefreshModelBlocksPacket(class_310 client, class_2540 buf)
    {
        int range = buf.readInt();

        client.execute(() ->
        {
            for (ModelBlockEntity mb : BBSRendering.capturedModelBlocks)
            {
                ModelProperties properties = mb.getProperties();
                int random = (int) (Math.random() * range);

                properties.setForm(FormUtils.copy(properties.getForm()));

                while (random > 0)
                {
                    properties.update(mb.getEntity());

                    random -= 1;
                }
            }
        });
    }

    /* API */
    
    public static void sendModelBlockForm(class_2338 pos, ModelBlockEntity modelBlock)
    {
        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_MODEL_BLOCK_FORM_PACKET, modelBlock.getProperties().toData(), (packetByteBuf) ->
        {
            packetByteBuf.method_10807(pos);
        });
    }

    public static void sendTriggerBlockUpdate(class_2338 pos, TriggerBlockEntity entity)
    {
        MapType data = new MapType();

        data.put("left", entity.left.toData());
        data.put("right", entity.right.toData());
        data.put("enter", entity.enter.toData());
        data.put("exit", entity.exit.toData());
        data.put("whileIn", entity.whileIn.toData());
        data.putInt("regionDelay", entity.regionDelay.get());
        data.put("pos1", entity.pos1.toData());
        data.put("pos2", entity.pos2.toData());
        data.put("regionOffset", entity.regionOffset.toData());
        data.put("regionSize", entity.regionSize.toData());
        data.putBool("collidable", entity.collidable.get());
        data.putBool("region", entity.region.get());

        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_TRIGGER_BLOCK_UPDATE, data, (packetByteBuf) ->
        {
            packetByteBuf.method_10807(pos);
        });
    }

    public static void sendPlayerForm(Form form)
    {
        MapType mapType = FormUtils.toData(form);

        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_PLAYER_FORM_PACKET, mapType == null ? new MapType() : mapType, (packetByteBuf) ->
        {});
    }

    /**
     * Ask the server to change this client's gamemode without chat feedback.
     */
    public static void sendSetGameMode(class_1934 mode)
    {
        if (mode == null || class_310.method_1551().field_1724 == null)
        {
            return;
        }

        class_2540 buf = PacketByteBufs.create();

        buf.method_10804(mode.method_8379());
        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_SET_GAME_MODE)));
    }

    public static void sendModelBlockTransforms(MapType data)
    {
        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_MODEL_BLOCK_TRANSFORMS_PACKET, data, (packetByteBuf) ->
        {});
    }

    public static void sendManagerDataLoad(String id, Consumer<BaseType> consumer)
    {
        MapType mapType = new MapType();

        mapType.putString("id", id);
        ClientNetwork.sendManagerData(RepositoryOperation.LOAD, mapType, consumer);
    }

    public static void sendManagerData(RepositoryOperation op, BaseType data, Consumer<BaseType> consumer)
    {
        int id = ids;

        callbacks.put(id, consumer);
        sendManagerData(id, op, data);

        ids += 1;
    }

    public static void sendManagerData(int callbackId, RepositoryOperation op, BaseType data)
    {
        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_MANAGER_DATA_PACKET, data, (packetByteBuf) ->
        {
            packetByteBuf.method_53002(callbackId);
            packetByteBuf.method_53002(op.ordinal());
        });
    }

    public static void sendActionRecording(String filmId, int replayId, int tick, int countdown, boolean state)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);
        buf.method_53002(replayId);
        buf.method_53002(tick);
        buf.method_53002(countdown);
        buf.method_52964(state);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_ACTION_RECORDING)));
    }

    public static void sendToggleFilm(String filmId, boolean withCamera)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);
        buf.method_52964(withCamera);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_TOGGLE_FILM)));
    }

    public static void sendActionState(String filmId, ActionState state, int tick)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);
        buf.method_52997(state.ordinal());
        buf.method_53002(tick);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_ACTION_CONTROL)));
    }

    public static void sendSyncData(String filmId, BaseValue data)
    {
        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_FILM_DATA_SYNC, data.toData(), (packetByteBuf) ->
        {
            DataPath path = data.getPath();

            packetByteBuf.method_10814(filmId);
            packetByteBuf.method_53002(path.strings.size());

            for (String string : path.strings)
            {
                packetByteBuf.method_10814(string);
            }
        });
    }

    public static void sendTeleport(class_1657 entity, double x, double y, double z)
    {
        sendTeleport(x, y, z, entity.method_5791(), entity.method_5791(), entity.method_36455());
    }

    public static void sendTeleport(double x, double y, double z, float yaw, float bodyYaw, float pitch)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_52940(x);
        buf.method_52940(y);
        buf.method_52940(z);
        buf.method_52941(yaw);
        buf.method_52941(bodyYaw);
        buf.method_52941(pitch);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_PLAYER_TP)));
    }

    public static void sendFormTrigger(String triggerId, int type)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(triggerId);
        buf.method_53002(type);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_ANIMATION_STATE_TRIGGER)));
    }

    public static void sendSharedForm(Form form, UUID uuid)
    {
        MapType mapType = FormUtils.toData(form);

        crusher.send(class_310.method_1551().field_1724, ServerNetwork.SERVER_SHARED_FORM, mapType == null ? new MapType() : mapType, (packetByteBuf) ->
        {
            packetByteBuf.method_10797(uuid);
        });
    }

    public static void sendZoom(boolean zoom)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_52964(zoom);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_ZOOM)));
    }

    public static void sendPauseFilm(String filmId)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10814(filmId);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_PAUSE_FILM)));
    }

    public static void sendTriggerBlockClick(class_2338 pos)
    {
        class_2540 buf = PacketByteBufs.create();

        buf.method_10807(pos);

        ClientPlayNetworking.send(ServerNetwork.BufPayload.from(buf, ServerNetwork.idFor(ServerNetwork.SERVER_TRIGGER_BLOCK_CLICK)));
    }
}
