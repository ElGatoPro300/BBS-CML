package mchorse.bbs_mod;

import mchorse.bbs_mod.addons.AddonInfo;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.misc.CurveClientClip;
import mchorse.bbs_mod.camera.clips.misc.TrackerClientClip;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.PendingFilmLaunch;
import mchorse.bbs_mod.client.StructurePickerClient;
import mchorse.bbs_mod.client.video.VideoFormEngine;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.TriggerBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.entity.ActorEntityRenderer;
import mchorse.bbs_mod.client.renderer.entity.GunProjectileEntityRenderer;
import mchorse.bbs_mod.client.renderer.item.GunItemRenderer;
import mchorse.bbs_mod.client.renderer.item.ModelBlockItemRenderer;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.discord.DiscordPresenceManager;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterFilmPreviewEvent;
import mchorse.bbs_mod.events.register.RegisterFilmUiAddonEvent;
import mchorse.bbs_mod.events.register.RegisterFormCategoriesEvent;
import mchorse.bbs_mod.events.register.RegisterFormEditorsEvent;
import mchorse.bbs_mod.events.register.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.events.register.RegisterIconsEvent;
import mchorse.bbs_mod.events.register.RegisterImportersEvent;
import mchorse.bbs_mod.events.register.RegisterInterpolationsEvent;
import mchorse.bbs_mod.events.register.RegisterKeyframeShapesEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.events.register.RegisterModelLoadersEvent;
import mchorse.bbs_mod.events.register.RegisterParticleComponentsEvent;
import mchorse.bbs_mod.events.register.RegisterPropTransformEvent;
import mchorse.bbs_mod.events.register.RegisterRayTracingEvent;
import mchorse.bbs_mod.events.register.RegisterReplayListContextMenuEvent;
import mchorse.bbs_mod.events.register.RegisterReplayPanelEvent;
import mchorse.bbs_mod.events.register.RegisterShadersEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.events.register.RegisterStencilMapEvent;
import mchorse.bbs_mod.events.register.RegisterUIKeyframeFactoriesEvent;
import mchorse.bbs_mod.events.register.RegisterUIValueFactoriesEvent;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.FramebufferManager;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.items.GunZoom;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.particles.ParticleManager;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLError;
import mchorse.bbs_mod.resources.packs.URLRepository;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.resources.packs.URLTextureErrorCallback;
import mchorse.bbs_mod.selectors.EntitySelectors;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel;
import mchorse.bbs_mod.settings.ui.UIValueMap;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.text.RtlFontManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.iris.IrisUtils;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.WorldPropertiesHelper;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIMobCaptureRecordOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIQuickReplayOverlayPanel;
import mchorse.bbs_mod.ui.film.toolbar.TimelineToolbarDockSync;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.KeyframeShapeRenderers;
import mchorse.bbs_mod.ui.framework.elements.utils.CustomFontManager;
import mchorse.bbs_mod.ui.model.UIModelPanel;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockEditorMenu;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.ui.utils.keys.KeybindSettings;
import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.RecentAssetsTracker;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import mchorse.bbs_mod.utils.VideoRecorder;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.resources.MinecraftSourcePack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.impl.client.rendering.BlockEntityRendererRegistryImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.class_1041;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_4587;
import net.minecraft.class_746;
import net.minecraft.class_757;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class BBSModClient implements ClientModInitializer
{
    public static final List<AddonInfo> registeredAddons = new ArrayList<>();

    public static void registerAddon(AddonInfo info)
    {
        registeredAddons.add(info);
    }
    private static TextureManager textures;
    private static FramebufferManager framebuffers;
    private static SoundManager sounds;
    private static L10n l10n;

    private static ModelManager models;
    private static FormCategories formCategories;
    private static ScreenshotRecorder screenshotRecorder;
    private static VideoRecorder videoRecorder;
    private static EntitySelectors selectors;

    private static ParticleManager particles;

    private static class_304 keyDashboard;
    private static class_304 keyItemEditor;
    private static class_304 keyPlayFilm;
    private static class_304 keyPauseFilm;
    private static class_304 keyRecordReplay;
    private static class_304 keyRecordVideo;
    private static class_304 keyOpenReplays;
    private static class_304 keyOpenQuickReplays;
    private static class_304 keyOpenMorphing;
    private static class_304 keyDemorph;
    private static class_304 keyTeleport;
    private static class_304 keyZoom;
    private static class_304 keyToggleReplayHud;

    private static UIDashboard dashboard;

    private static CameraController cameraController = new CameraController();
    private static ModelBlockItemRenderer modelBlockItemRenderer = new ModelBlockItemRenderer();
    private static GunItemRenderer gunItemRenderer = new GunItemRenderer();
    private static Films films;
    private static GunZoom gunZoom;

    private static Replay selectedReplay;

    private static float originalFramebufferScale;

    public static TextureManager getTextures()
    {
        return textures;
    }

    public static FramebufferManager getFramebuffers()
    {
        return framebuffers;
    }

    public static SoundManager getSounds()
    {
        return sounds;
    }

    public static L10n getL10n()
    {
        return l10n;
    }

    public static ModelManager getModels()
    {
        return models;
    }

    public static FormCategories getFormCategories()
    {
        return formCategories;
    }

    public static ScreenshotRecorder getScreenshotRecorder()
    {
        return screenshotRecorder;
    }

    public static VideoRecorder getVideoRecorder()
    {
        return videoRecorder;
    }

    public static EntitySelectors getSelectors()
    {
        return selectors;
    }

    public static ParticleManager getParticles()
    {
        return particles;
    }

    public static CameraController getCameraController()
    {
        return cameraController;
    }

    public static Films getFilms()
    {
        return films;
    }

     public static void setSelectedReplay(Replay replay)
    {
        selectedReplay = replay;
    }

    public static Replay getSelectedReplay()
    {
        return selectedReplay;
    }


    public static GunZoom getGunZoom()
    {
        return gunZoom;
    }

    public static class_304 getKeyZoom()
    {
        return keyZoom;
    }

    public static class_304 getKeyRecordVideo()
    {
        return keyRecordVideo;
    }

    public static class_304 getKeyOpenQuickReplays()
    {
        return keyOpenQuickReplays;
    }

    public static UIDashboard getDashboard()
    {
        if (dashboard == null)
        {
            dashboard = new UIDashboard();
        }

        return dashboard;
    }

    public static UIDashboard peekDashboard()
    {
        return dashboard;
    }

    public static int getGUIScale()
    {
        float scale = BBSSettings.getUIScaleFactor();

        if (scale <= 0F)
        {
            return class_310.method_1551().field_1690.method_42474().method_41753();
        }

        return Math.max(1, Math.round(scale));
    }

    /**
     * The exact (possibly fractional) BBS UI scale, e.g. 1.6. Returns 0 when set to "auto" so the
     * window keeps Minecraft's computed integer scale.
     */
    public static double getUIScaleFactor()
    {
        return BBSSettings.getUIScaleFactor();
    }

    public static float getOriginalFramebufferScale()
    {
        return Math.max(originalFramebufferScale, 1);
    }

    public static ModelProperties getItemStackProperties(class_1799 stack)
    {
        ModelBlockItemRenderer.Item item = modelBlockItemRenderer.get(stack);

        if (item != null)
        {
            return item.entity.getProperties();
        }

        GunItemRenderer.Item gunItem = gunItemRenderer.get(stack);

        if (gunItem != null)
        {
            return gunItem.properties;
        }

        return null;
    }

    public static void onEndKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info)
    {
        if (action != GLFW.GLFW_PRESS)
        {
            return;
        }

        class_746 player = class_310.method_1551().field_1724;

        if (player == null || class_310.method_1551().field_1755 != null)
        {
            return;
        }

        Morph morph = Morph.getMorph(player);

        /* Animation state trigger */
        if (morph != null && morph.getForm() != null && morph.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_MORPH);
            form.playState(state);
        }))
            return;

        /* Animation state trigger for items*/
        ModelProperties main = getItemStackProperties(player.method_5998(class_1268.field_5808));
        ModelProperties offhand = getItemStackProperties(player.method_5998(class_1268.field_5810));

        if (main != null && main.getForm() != null && main.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_MAIN_HAND_ITEM);
            form.playState(state);
        }))
            return;

        if (offhand != null && offhand.getForm() != null && offhand.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_OFF_HAND_ITEM);
            form.playState(state);
        }))
            return;

        /* Change form based on the hotkey */
        for (Form form : BBSModClient.getFormCategories().getRecentForms().getCategories().get(0).getForms())
        {
            if (form.hotkey.get() == key)
            {
                ClientNetwork.sendPlayerForm(form);

                return;
            }
        }

        for (UserFormCategory category : BBSModClient.getFormCategories().getUserForms().categories)
        {
            for (Form form : category.getForms())
            {
                if (form.hotkey.get() == key)
                {
                    ClientNetwork.sendPlayerForm(form);

                    return;
                }
            }
        }
    }

    @Override
    public void onInitializeClient()
    {
        mchorse.bbs_mod.forms.structure.ModelCollisionLiveBake.register();

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
        {
            if (world.method_8321(pos) instanceof TriggerBlockEntity)
            {
                if (player.method_7337())
                {
                    return class_1269.field_5811;
                }

                ClientNetwork.sendTriggerBlockClick(pos);

                return class_1269.field_5812;
            }

            if (player.method_5998(hand).method_7909() == BBSMod.STRUCTURE_PICKER_ITEM)
            {
                if (world.field_9236)
                {
                    return StructurePickerClient.onAttackBlock();
                }

                return class_1269.field_5812;
            }

            return class_1269.field_5811;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            if (!world.field_9236)
            {
                if (player.method_5998(hand).method_7909() == BBSMod.STRUCTURE_PICKER_ITEM)
                {
                    /* Allow opening Model Block UI while holding Structure Picker. */
                    if (hitResult != null && world.method_8320(hitResult.method_17777()).method_26204() instanceof ModelBlock)
                    {
                        return class_1269.field_5811;
                    }

                    return class_1269.field_5812;
                }

                return class_1269.field_5811;
            }

            return StructurePickerClient.onUseBlock(hitResult, player.method_5715());
        });

        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon-client", BBSAddonMod.class)
            .forEach((container) ->
            {
                BBSMod.events.register(container.getEntrypoint());
            });

        AssetProvider provider = BBSMod.getProvider();

        textures = new TextureManager(provider);
        framebuffers = new FramebufferManager();
        sounds = new SoundManager(provider);
        l10n = new L10n();
        l10n.register((lang) -> Collections.singletonList(Link.assets("strings/" + lang + ".json")));
        l10n.reload();

        BBSMod.events.post(new RegisterL10nEvent(l10n));

        File parentFile = BBSMod.getSettingsFolder().getParentFile();

        particles = new ParticleManager(() -> new File(BBSMod.getAssetsFolder(), "particles"));

        models = new ModelManager(provider);
        BBSMod.events.post(new RegisterModelLoadersEvent(models));
        formCategories = new FormCategories();
        BBSMod.events.post(new RegisterFormCategoriesEvent(formCategories));
        BBSMod.events.post(new RegisterImportersEvent());
        BBSMod.events.post(new RegisterParticleComponentsEvent(ParticleScheme.PARSER.components));
        BBSMod.events.post(new RegisterInterpolationsEvent(Interpolations.MAP));
        BBSMod.events.post(new RegisterFormsRenderersEvent());
        BBSMod.events.post(new RegisterFormEditorsEvent(UIFormEditor.panels));
        BBSMod.events.post(new RegisterIconsEvent());
        BBSMod.events.post(new RegisterUIValueFactoriesEvent(UIValueMap.factories));
        BBSMod.events.post(new RegisterUIKeyframeFactoriesEvent(UIKeyframeFactory.FACTORIES));
        BBSMod.events.post(new RegisterKeyframeShapesEvent(KeyframeShapeRenderers.SHAPES));
        BBSMod.events.post(new RegisterPropTransformEvent());
        BBSMod.events.post(new RegisterStencilMapEvent());
        BBSMod.events.post(new RegisterRayTracingEvent());
        BBSMod.events.post(new RegisterFilmPreviewEvent());
        BBSMod.events.post(new RegisterFilmUiAddonEvent());
        BBSSettings.setOptionalFilmUiSkinActive(mchorse.bbs_mod.ui.framework.styles.UIStyle::isMinecut);
        BBSMod.events.post(new RegisterReplayListContextMenuEvent());
        BBSMod.events.post(new RegisterReplayPanelEvent());
        screenshotRecorder = new ScreenshotRecorder(new File(parentFile, "screenshots"));
        videoRecorder = new VideoRecorder();
        selectors = new EntitySelectors();
        selectors.read();
        films = new Films();

        RecentAssetsTracker.load();

        BBSResources.init();

        URLRepository repository = new URLRepository(new File(parentFile, "url_cache"));

        provider.register(new URLSourcePack("http", repository));
        provider.register(new URLSourcePack("https", repository));

        KeybindSettings.registerClasses();

        BBSMod.setupConfig(Icons.KEY_CAP, "keybinds", new File(BBSMod.getSettingsFolder(), "keybinds.json"), KeybindSettings::register);

        BBSMod.events.post(new RegisterClientSettingsEvent());

        BBSSettings.language.postCallback((v, f) ->
        {
            RtlFontManager.invalidate();
            reloadLanguage(getLanguageKey());
            RtlFontManager.ensureLoaded();
        });

        BBSSettings.editorTimeMode.postCallback((v, f) ->
        {
            if (dashboard != null && dashboard.getPanels().panel instanceof UIFilmPanel panel)
            {
                panel.fillData();
            }
        });

        BBSSettings.discordPresence.postCallback((v, f) -> DiscordPresenceManager.INSTANCE.onSettingsChanged());
        BBSSettings.discordApplicationId.postCallback((v, f) -> DiscordPresenceManager.INSTANCE.onSettingsChanged());

        if (BBSSettings.irisOpacityFix != null)
        {
            BBSSettings.irisOpacityFix.postCallback((v, f) -> IrisUtils.reloadShaders());
        }

        if (BBSSettings.shaderShadowOpacity != null)
        {
            BBSSettings.shaderShadowOpacity.postCallback((v, f) ->
                mchorse.bbs_mod.utils.iris.ShaderOpacityPatch.syncShadowOpacityDefault());
        }

        if (BBSSettings.worldGammaPercent != null)
        {
            WorldPropertiesHelper.setGammaPercent(BBSSettings.worldGammaPercent.get());
        }

        IValueListener refreshModelHover = (v, f) ->
        {
            if (!UISettingsOverlayPanel.isDeferringLiveSettings())
            {
                BBSSettings.syncAppliedAppearance();
                refreshModelEditorHover();
            }
        };
        BBSSettings.modelEditorHoverColor.postCallback(refreshModelHover);
        BBSSettings.modelEditorHoverOpacity.postCallback(refreshModelHover);
        BBSSettings.modelEditorAltHoverColor.postCallback(refreshModelHover);
        BBSSettings.modelEditorAltHoverOpacity.postCallback(refreshModelHover);
        BBSSettings.modelEditorAltHoverMultipleColors.postCallback(refreshModelHover);
        BBSSettings.favoriteColors.postCallback(refreshModelHover);

        BBSSettings.editorTimelineToolbar.postCallback((v, f) -> TimelineToolbarDockSync.applySettingsChange());

        BBSSettings.editorSeparateReplayPropertiesPanel.postCallback((v, f) ->
        {
            if (dashboard != null && dashboard.getPanels().panel instanceof UIFilmPanel panel)
            {
                panel.applySeparateReplayPropertiesPanelSetting();
            }
        });
        BBSSettings.editorEmbeddedKeyframeSidePanel.postCallback((v, f) ->
        {
            if (dashboard != null && dashboard.getPanels().panel instanceof UIFilmPanel panel)
            {
                panel.applyEmbeddedKeyframeSidePanelSetting();
            }
        });
        BBSSettings.tooltipStyle.modes(
            UIKeys.ENGINE_TOOLTIP_STYLE_LIGHT,
            UIKeys.ENGINE_TOOLTIP_STYLE_DARK
        );
        BBSSettings.uiStyle.modes(UIKeys.ENGINE_UI_STYLE_CLASSIC);

        if (mchorse.bbs_mod.ui.film.FilmUiCapabilities.hasAddon())
        {
            BBSSettings.uiStyle.modes(
                UIKeys.ENGINE_UI_STYLE_CLASSIC,
                UIKeys.ENGINE_UI_STYLE_MINECUT
            );
        }
        else if (BBSSettings.uiStyle != null && BBSSettings.uiStyle.get() == 1)
        {
            BBSSettings.uiStyle.set(0);
        }

        BBSSettings.uiStyle.postCallback((v, f) ->
        {
            mchorse.bbs_mod.ui.framework.styles.UIStyle.invalidateMinecutCache();
        });

        BBSSettings.replayContextOptions.modes(
            UIKeys.CONFIG_GENERAL_COMPACTED_OPTIONS_DEFAULT,
            UIKeys.CONFIG_GENERAL_COMPACTED_OPTIONS_SEPARATED,
            UIKeys.CONFIG_GENERAL_COMPACTED_OPTIONS_COMPACTED
        );

        BBSSettings.gizmoStyle.modes(
            UIKeys.CONFIG_AXES_GIZMO_STYLE_1,
            UIKeys.CONFIG_AXES_GIZMO_STYLE_2,
            UIKeys.CONFIG_AXES_GIZMO_STYLE_3
        );

        BBSSettings.editorTimeMode.modes(
            UIKeys.CONFIG_EDITOR_TICKS_MODE,
            UIKeys.CONFIG_EDITOR_SECONDS_MODE,
            UIKeys.CONFIG_EDITOR_FRAMES_MODE
        );

        BBSSettings.keystrokeMode.modes(
            UIKeys.ENGINE_KEYSTROKES_POSITION_AUTO,
            UIKeys.ENGINE_KEYSTROKES_POSITION_BOTTOM_LEFT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_BOTTOM_RIGHT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_TOP_RIGHT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_TOP_LEFT
        );

        UIKeys.C_KEYBIND_CATGORIES.load(KeyCombo.getCategoryKeys());
        UIKeys.C_KEYBIND_CATGORIES_TOOLTIP.load(KeyCombo.getCategoryKeys());

        /* Replace audio clip with client version that plays audio */
        BBSMod.getFactoryCameraClips()
            .register(Link.bbs("audio"), AudioClientClip.class, new ClipFactoryData(Icons.SOUND, 0xffc825))
            .register(Link.bbs("tracker"), TrackerClientClip.class, new ClipFactoryData(Icons.USER, 0x4cedfc))
            .register(Link.bbs("curve"), CurveClientClip.class, new ClipFactoryData(Icons.ARC, 0xff775f));

        /* Keybinds */
        keyDashboard = this.createKey("dashboard", GLFW.GLFW_KEY_0);
        keyItemEditor = this.createKey("item_editor", GLFW.GLFW_KEY_HOME);
        keyPlayFilm = this.createKey("play_film", GLFW.GLFW_KEY_RIGHT_CONTROL);
        keyPauseFilm = this.createKey("pause_film", GLFW.GLFW_KEY_BACKSLASH);
        keyRecordReplay = this.createKey("record_replay", GLFW.GLFW_KEY_RIGHT_ALT);
        keyRecordVideo = this.createKey("record_video", GLFW.GLFW_KEY_F4);
        keyOpenReplays = this.createKey("open_replays", GLFW.GLFW_KEY_RIGHT_SHIFT);
        keyOpenQuickReplays = this.createKey("open_quick_replays", GLFW.GLFW_KEY_RIGHT_BRACKET);
        keyOpenMorphing = this.createKey("open_morphing", GLFW.GLFW_KEY_B);
        keyDemorph = this.createKey("demorph", GLFW.GLFW_KEY_PERIOD);
        keyTeleport = this.createKey("teleport", GLFW.GLFW_KEY_Y);
        keyZoom = this.createKeyMouse("zoom", 2);
        keyToggleReplayHud = this.createKey("toggle_replay_hud", GLFW.GLFW_KEY_P);

        WorldRenderEvents.AFTER_ENTITIES.register((context) ->
        {
            BBSRendering.renderCoolStuff(context);

            if (BBSRendering.isChromaSkyEnabled())
            {
                float d = BBSRendering.getChromaSkyBillboard();

                if (d > 0)
                {
                    class_4587 stack = context.matrixStack();
                    Color color = Colors.COLOR.set(BBSRendering.getChromaSkyColor());

                    stack.method_22903();

                    class_4587.class_4665 peek = stack.method_23760();

                    peek.method_23761().identity();
                    peek.method_23762().identity();
                    stack.method_46416(0F, 0F, -d);

                    RenderSystem.enableDepthTest();
                    class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1576);

                    float fov = class_310.method_1551().field_1690.method_41808().method_41753();
                    float dd = d * (float) Math.pow(fov / 40F, 2F);

                    Draw.fillQuad(builder, stack,
                        -dd, -dd, 0,
                        dd, -dd, 0,
                        dd, dd, 0,
                        -dd, dd, 0,
                        color.r, color.g, color.b, 1F
                    );

                    RenderSystem.setShader(class_757::method_34540);

                    Matrix4fStack mvStack = RenderSystem.getModelViewStack();
                    mvStack.pushMatrix();
                    mvStack.identity();
                    RenderSystem.applyModelViewMatrix();

                    class_286.method_43433(builder.method_60800());

                    mvStack.popMatrix();
                    RenderSystem.applyModelViewMatrix();

                    RenderSystem.disableDepthTest();

                    stack.method_22909();
                }
            }
        });

        /* Soft-opacity forms wait until water/lava/portals are drawn; flush here (not inside
         * renderLayer) so WorldRenderer's pose stack stays balanced. */
        WorldRenderEvents.AFTER_TRANSLUCENT.register((context) ->
        {
            ShaderOpacityPatch.onAfterTranslucentTerrain();
        });

        WorldRenderEvents.LAST.register((context) ->
        {
            mchorse.bbs_mod.graphics.Draw.flushIrisBoxes();

            /* After clouds / translucents / model blocks so selection+gizmos stay on top. */
            mchorse.bbs_mod.client.StructurePickerRenderer.render(context);
            mchorse.bbs_mod.graphics.Draw.flushIrisBoxes();

            if (Gizmo.INSTANCE.hasDeferred())
            {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                Gizmo.INSTANCE.renderDeferred(context.matrixStack());
                RenderSystem.depthMask(true);
            }

            if (videoRecorder.isRecording() && BBSRendering.canRender)
            {
                videoRecorder.recordFrame();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            RecentAssetsTracker.load();
            PendingFilmLaunch.onJoin();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            dashboard = null;
            films = new Films();
            setSelectedReplay(null);

            ClientNetwork.resetHandshake();
            films.reset();
            cameraController.reset();
            BBSMod.setRegistryManager(null);
        });

        ClientTickEvents.START_CLIENT_TICK.register((client) ->
        {
            BBSRendering.startTick();

            if (!client.method_1493())
            {
                TriggerBlockEntityRenderer.capturedTriggerBlocks.clear();
            }
        });

        ClientTickEvents.END_WORLD_TICK.register((client) ->
        {
            class_310 mc = class_310.method_1551();

            if (!mc.method_1493())
            {
                films.updateEndWorld();
            }

            BBSResources.tick();
        });

        ClientTickEvents.END_CLIENT_TICK.register((client) ->
        {
            class_310 mc = class_310.method_1551();

            if (mc.field_1755 instanceof UIScreen screen)
            {
                screen.update();
            }

            DiscordPresenceManager.INSTANCE.tick();

            PendingFilmLaunch.tick(mc);

            cameraController.update();

            if (!mc.method_1493())
            {
                films.update();
                modelBlockItemRenderer.update();
                gunItemRenderer.update();
                textures.update();
                VideoFormEngine.tickCleanup();
            }

            StructurePickerClient.tick(mc);

            while (keyDashboard.method_1436()) UIScreen.open(getDashboard());
            while (keyItemEditor.method_1436()) this.keyOpenModelBlockEditor(mc);
            while (keyPlayFilm.method_1436()) this.keyPlayFilm();
            while (keyPauseFilm.method_1436()) this.keyPauseFilm();
            while (keyRecordReplay.method_1436()) this.keyRecordReplay();
            while (keyRecordVideo.method_1436())
            {
                class_1041 window = mc.method_22683();
                int width = Math.max(window.method_4480(), 2);
                int height = Math.max(window.method_4507(), 2);

                if (width % 2 == 1) width -= width % 2;
                if (height % 2 == 1) height -= height % 2;

                videoRecorder.toggleRecording(BBSRendering.getTexture().id, width, height);
                BBSRendering.setCustomSize(videoRecorder.isRecording(), width, height);
            }
            while (keyOpenReplays.method_1436()) this.keyOpenReplays();
            while (keyOpenQuickReplays.method_1436())
            {
                if (!UIQuickReplayOverlayPanel.isOpened())
                {
                    this.keyOpenQuickReplays();
                }
            }
            while (keyOpenMorphing.method_1436())
            {
                UIDashboard dashboard = getDashboard();

                /* Select Morphing before open so onOpen does not briefly apply
                 * Spectator from a leftover Film / Model Block panel. */
                dashboard.setPanel(dashboard.getPanel(UIMorphingPanel.class));
                UIScreen.open(dashboard);
            }
            while (keyDemorph.method_1436()) ClientNetwork.sendPlayerForm(null);
            while (keyTeleport.method_1436()) this.keyTeleport();
            while (keyToggleReplayHud.method_1436()) BBSSettings.editorReplayHud.set(!BBSSettings.editorReplayHud.get());

            if (mc.field_1724 != null)
            {
                boolean zoom = keyZoom.method_1434();
                class_1799 stack = mc.field_1724.method_6047();

                if (gunZoom == null && zoom && stack.method_7909() == BBSMod.GUN_ITEM)
                {
                    GunProperties properties = GunProperties.get(stack);

                    ClientNetwork.sendZoom(true);
                    gunZoom = new GunZoom(properties.fovTarget, properties.fovInterp, properties.fovDuration);
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
        {
            BBSRendering.renderHud(drawContext, tickCounter.method_60637(false));

            if (gunZoom != null)
            {
                gunZoom.update(keyZoom.method_1434(), tickCounter.method_60636());

                if (gunZoom.canBeRemoved())
                {
                    ClientNetwork.sendZoom(false);
                    gunZoom = null;
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register((e) ->
        {
            DiscordPresenceManager.INSTANCE.shutdown();
            BBSResources.stopWatchdog();
        });
        ClientLifecycleEvents.CLIENT_STARTED.register((e) ->
        {
            DiscordPresenceManager.INSTANCE.init();
            DiscordPresenceManager.INSTANCE.onClientStarted();
            BBSRendering.setupFramebuffer();
            provider.register(new MinecraftSourcePack());
            RtlFontManager.ensureLoaded();

            class_1041 window = class_310.method_1551().method_22683();

            originalFramebufferScale = window.method_4489() / window.method_4480();
        });

        URLTextureErrorCallback.EVENT.register((url, error) ->
        {
            UIBaseMenu menu = UIScreen.getCurrentMenu();

            if (menu != null)
            {
                url = url.substring(0, MathUtils.clamp(url.length(), 0, 40));

                if (error == URLError.FFMPEG)
                {
                    menu.context.notifyError(UIKeys.TEXTURE_URL_ERROR_FFMPEG.format(url));
                }
                else if (error == URLError.HTTP_ERROR)
                {
                    menu.context.notifyError(UIKeys.TEXTURE_URL_ERROR_HTTP.format(url));
                }
            }
        });

        BBSRendering.setup();

        /* Network */
        ClientNetwork.setup();

        /* Register addons from FabricLoader (common + client-only entrypoints). */
        java.util.Set<String> registeredAddonIds = new java.util.HashSet<>();

        java.util.function.Consumer<net.fabricmc.loader.api.entrypoint.EntrypointContainer<BBSAddonMod>> registerCatalog =
            (container) ->
            {
                ModMetadata meta = container.getProvider().getMetadata();
                String id = meta.getId();

                if (!registeredAddonIds.add(id))
                {
                    return;
                }

                String name = meta.getName();
                String version = meta.getVersion().getFriendlyString();
                String description = meta.getDescription();
                List<String> authors = meta.getAuthors().stream().map(Person::getName).toList();

                Link icon = null;
                Optional<String> iconPath = meta.getIconPath(64);

                if (iconPath.isPresent())
                {
                    String path = iconPath.get();

                    if (path.startsWith("assets/"))
                    {
                        String relative = path.substring("assets/".length());

                        icon = new Link("mod_icons", relative);
                    }
                }

                ContactInformation contact = meta.getContact();
                String website = contact.get("homepage").orElse("");
                String issues = contact.get("issues").orElse("");
                String source = contact.get("sources").orElse("");

                registerAddon(new AddonInfo(id, name, version, description, authors, icon, website, issues, source));
            };

        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon", BBSAddonMod.class)
            .forEach(registerCatalog);
        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon-client", BBSAddonMod.class)
            .forEach(registerCatalog);

        /* Entity renderers */
        EntityRendererRegistry.register(BBSMod.ACTOR_ENTITY, ActorEntityRenderer::new);
        EntityRendererRegistry.register(BBSMod.GUN_PROJECTILE_ENTITY, GunProjectileEntityRenderer::new);

        BlockEntityRendererRegistry.register(BBSMod.MODEL_BLOCK_ENTITY, ModelBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(BBSMod.TRIGGER_BLOCK_ENTITY, TriggerBlockEntityRenderer::new);

        BuiltinItemRendererRegistry.INSTANCE.register(BBSMod.MODEL_BLOCK_ITEM, modelBlockItemRenderer);
        BuiltinItemRendererRegistry.INSTANCE.register(BBSMod.GUN_ITEM, gunItemRenderer);

        /* Create folders */
        BBSMod.getAudioFolder().mkdirs();
        BBSMod.getAssetsPath("textures").mkdirs();

        for (String path : List.of("alex", "alex_simple", "steve", "steve_simple"))
        {
            BBSMod.getAssetsPath("models/emoticons/" + path + "/").mkdirs();
        }

        for (String path : List.of("alex", "alex_bends", "eyes", "eyes_1px", "steve", "steve_bends"))
        {
            BBSMod.getAssetsPath("models/player/" + path + "/").mkdirs();
        }
    }

    private class_304 createKey(String id, int key)
    {
        return KeyBindingHelper.registerKeyBinding(new class_304(
            "key." + BBSMod.MOD_ID + "." + id,
            class_3675.class_307.field_1668,
            key,
            "category." + BBSMod.MOD_ID + ".main"
        ));
    }

    private class_304 createKeyMouse(String id, int button)
    {
        return KeyBindingHelper.registerKeyBinding(new class_304(
            "key." + BBSMod.MOD_ID + "." + id,
            class_3675.class_307.field_1672,
            button,
            "category." + BBSMod.MOD_ID + ".main"
        ));
    }

    private void keyOpenModelBlockEditor(class_310 mc)
    {
        class_1799 stack = mc.field_1724.method_6118(class_1304.field_6173);
        ModelBlockItemRenderer.Item item = modelBlockItemRenderer.get(stack);
        GunItemRenderer.Item gunItem = gunItemRenderer.get(stack);

        if (item != null)
        {
            UIScreen.open(new UIModelBlockEditorMenu(item.entity.getProperties()));
        }
        else if (gunItem != null)
        {
            UIScreen.open(new UIModelBlockEditorMenu(gunItem.properties));
        }
    }

    private void keyPlayFilm()
    {
        UIFilmPanel panel = getDashboard().getPanel(UIFilmPanel.class);

        if (panel.getData() != null)
        {
            Films.playFilm(panel.getData().getId(), false);
        }
    }

    private void keyPauseFilm()
    {
        UIFilmPanel panel = getDashboard().getPanel(UIFilmPanel.class);

        if (panel.getData() != null)
        {
            Films.pauseFilm(panel.getData().getId());
        }
    }

    private void keyRecordReplay()
    {
        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null)
        {
            Recorder recorder = getFilms().getRecorder();

            if (recorder != null)
            {
                recorder = BBSModClient.getFilms().stopRecording();

                if (recorder == null || recorder.hasNotStarted() || panel.getData() == null)
                {
                    return;
                }

                panel.applyRecordedKeyframes(recorder, panel.getData());
                panel.replayEditor.replays.replays.buildVisualList();
                panel.replayEditor.updateChannelsList();
                panel.save();
            }
            else
            {
                if (UIMobCaptureRecordOverlayPanel.isOpened())
                {
                    return;
                }

                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);

                if (filmPanel == null || filmPanel.getData() == null)
                {
                    return;
                }

                if (BBSSettings.recordingMobCaptureOnAlt.get())
                {
                    UIMobCaptureRecordOverlayPanel.openInGame((setup) ->
                    {
                        if (filmPanel.getData() == null)
                        {
                            return;
                        }

                        Replay replay = filmPanel.replayEditor.getReplay();

                        if (replay == null)
                        {
                            replay = getSelectedReplay();
                        }

                        int index = filmPanel.getData().replays.getList().indexOf(replay);

                        if (index >= 0)
                        {
                            getFilms().startRecording(filmPanel.getData(), index, 0);
                        }
                    });
                }
                else
                {
                    Replay replay = filmPanel.replayEditor.getReplay();

                    if (replay == null)
                    {
                        replay = getSelectedReplay();
                    }

                    int index = filmPanel.getData().replays.getList().indexOf(replay);

                    if (index >= 0)
                    {
                        getFilms().startRecording(filmPanel.getData(), index, 0);
                    }
                }
            }
        }
    }

    private void keyOpenReplays()
    {
        UIScreen.open(getDashboard());
    }

    private void keyOpenQuickReplays()
    {
        UIDashboard dashboard = getDashboard();

        Film quickReplayFilm = this.getQuickReplayFilm(dashboard);

        if (quickReplayFilm != null && !quickReplayFilm.replays.getList().isEmpty())
        {
            UIQuickReplayOverlayPanel.open(
                new UIQuickReplayOverlayPanel(
                    quickReplayFilm.replays.getList(),
                    getSelectedReplay(),
                    this::setQuickReplaySelection
                )
            );

            return;
        }
    }

    private void setQuickReplaySelection(Replay replay)
    {
        setSelectedReplay(replay);

        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null && panel.getData().replays.getList().contains(replay))
        {
            panel.replayEditor.setReplay(replay);
        }
    }

    private Film getQuickReplayFilm(UIDashboard dashboard)
    {
        Replay selected = getSelectedReplay();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);
        Film film = panel == null ? null : panel.getData();

        if (this.isFilmUsableForQuickSelection(film, selected))
        {
            return film;
        }

        Recorder recorder = getFilms().getRecorder();

        if (recorder != null && this.isFilmUsableForQuickSelection(recorder.film, selected))
        {
            return recorder.film;
        }

        for (BaseFilmController controller : getFilms().getControllers())
        {
            if (this.isFilmUsableForQuickSelection(controller.film, selected))
            {
                return controller.film;
            }
        }

        return null;
    }

    private boolean isFilmUsableForQuickSelection(Film film, Replay selected)
    {
        if (film == null || film.replays.getList().isEmpty())
        {
            return false;
        }

        return selected == null || film.replays.getList().contains(selected);
    }

    private void keyTeleport()
    {
        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null)
        {
            panel.replayEditor.teleport();
        }
    }

    public static void reloadFromSettings()
    {
        BBSSettings.syncAppliedAppearance();
        refreshModelEditorHover();
        CustomFontManager.invalidate();
        RtlFontManager.invalidate();

        for (Settings settings : BBSMod.getSettings().modules.values())
        {
            settings.save();
        }

        reloadLanguage(getLanguageKey());

        UIDashboard dashboard = getDashboard();

        if (dashboard != null)
        {
            UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);

            if (filmPanel != null)
            {
                filmPanel.fillData();
            }
        }

        class_310 mc = class_310.method_1551();
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (menu != null && mc != null)
        {
            int desiredScale = getGUIScale();
            mc.field_1690.method_42474().method_41748(desiredScale);
            mc.method_15993();
            menu.resize(mc.method_22683().method_4486(), mc.method_22683().method_4502());
        }
    }

    /** Reapplies the BBS UI scale to the currently open menu immediately (e.g. while a settings
     *  slider is being dragged), without the heavier work {@link #reloadFromSettings()} does
     *  (saving settings to disk, reloading language, etc). */
    public static void applyUIScaleLive()
    {
        class_310 mc = class_310.method_1551();
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (menu != null && mc != null)
        {
            mc.field_1690.method_42474().method_41748(getGUIScale());
            mc.method_15993();
            menu.resize(mc.method_22683().method_4486(), mc.method_22683().method_4502());
        }
    }

    /** Applies the model editor hover color/opacity immediately (settings live-preview),
     *  refreshing both the applied snapshot the renderers read and the model editor's
     *  cached geometry highlight. */
    public static void applyModelEditorHoverLive()
    {
        BBSSettings.syncAppliedAppearance();
        refreshModelEditorHover();
    }

    private static void refreshModelEditorHover()
    {
        UIDashboard dashboard = getDashboard();

        if (dashboard == null)
        {
            return;
        }

        UIDashboardPanel panel = dashboard.getPanels().panel;

        if (panel instanceof UIModelPanel modelPanel)
        {
            modelPanel.renderer.dirty();
        }
    }

    public static String getLanguageKey()
    {
        return getLanguageKey(BBSSettings.language.get());
    }

    public static String getLanguageKey(String key)
    {
        if (key == null || key.isEmpty())
        {
            class_310 client = class_310.method_1551();

            if (client == null || client.field_1690 == null)
            {
                return "";
            }

            key = client.field_1690.field_1883;
        }

        return key;
    }

    public static void reloadLanguage(String language)
    {
        l10n.reload(language, BBSMod.getProvider());
        RtlFontManager.ensureLoaded();
    }
}
