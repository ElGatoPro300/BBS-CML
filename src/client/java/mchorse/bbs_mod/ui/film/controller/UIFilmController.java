package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.film.MobCaptureRecordingSetup;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.RecorderMobCapture;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.ui.ValueOnionSkin;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.FilmPoseGizmoDrag;
import mchorse.bbs_mod.ui.film.replays.UIRecordOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.gizmo.TransformOrientation;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyAction;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

public class UIFilmController extends UIElement
{
    public static final int CAMERA_MODE_CAMERA = 0;
    public static final int CAMERA_MODE_FREE = 1;
    public static final int CAMERA_MODE_ORBIT = 2;
    public static final int CAMERA_MODE_FIRST_PERSON = 3;
    public static final int CAMERA_MODE_THIRD_PERSON_BACK = 4;
    public static final int CAMERA_MODE_THIRD_PERSON_FRONT = 5;

    public final UIFilmPanel panel;

    public FilmEditorController editorController;
    private Map<String, Integer> actors;

    /* Character control */
    private IEntity controlled;
    private final Vector2i lastMouse = new Vector2i();
    private int mouseMode;
    private final Vector2f mouseStick = new Vector2f();

    /* Recording state */
    private IEntity previousEntity;
    private Form playerForm;
    private int recordingTick;
    private boolean recording;
    private int recordingCountdown;
    private List<String> recordingGroups;
    private BaseType recordingOld;
    private boolean instantKeyframes;
    private boolean countdownControl;

    private boolean wasFlying;
    private boolean wasAllowFlying;
    private boolean flightModified;

    /* Replay and group picking */
    private IEntity hoveredEntity;
    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();

    public final OrbitFilmCameraController orbit = new OrbitFilmCameraController(this);
    private int pov;
    private boolean paused;

    private WorldRenderContext worldRenderContext;
    private final Matrix4f gizmoInterfaceMatrix = new Matrix4f();

    public UIFilmController(UIFilmPanel panel)
    {
        this.panel = panel;

        IKey category = UIKeys.FILM_CONTROLLER_KEYS_CATEGORY;

        Supplier<Boolean> hasActor = () -> this.getCurrentEntity() != null;
        Supplier<Boolean> hasTwoOrMoreReplays = () -> this.panel.getData() != null && this.panel.getData().replays.getList().size() >= 2;
        Supplier<Boolean> hasFilm = () -> this.panel.getData() != null;

        this.keys().register(Keys.FILM_CONTROLLER_START_RECORDING, this::pickRecording).active(hasActor).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_INSERT_FRAME, () ->
        {
            this.insertFrame();
            UIUtils.playClick();
        }).active(hasActor).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_TOGGLE_CONTROL, this::toggleControl).active(hasFilm).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_TOGGLE_ORBIT_MODE, this::toggleOrbitMode).active(hasFilm).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_MOVE_REPLAY_TO_CURSOR, () ->
        {
            Area area = this.panel.preview.getViewport();
            UIContext context = this.getContext();
            Vector3d hit = this.panel.replayEditor.rayTraceViewportFromContext(context, area);

            if (hit != null)
            {
                this.panel.replayEditor.moveReplay(hit.x, hit.y, hit.z);
            }
        }).active(hasActor).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_RESTART_ACTIONS, () ->
        {
            this.panel.notifyServer(ActionState.RESTART);
            this.createEntities();
        }).active(hasFilm).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_TOGGLE_ONION_SKIN, () ->
        {
            this.getOnionSkin().enabled.toggle();

            UIUtils.playClick();
        }).active(hasFilm).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_OPEN_REPLAYS, () ->
        {
            this.panel.preview.openReplays();
        }).active(hasFilm).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_PREV_REPLAY, () -> this.switchReplay(-1)).active(hasTwoOrMoreReplays).category(category);
        this.keys().register(Keys.FILM_CONTROLLER_NEXT_REPLAY, () -> this.switchReplay(1)).active(hasTwoOrMoreReplays).category(category);

        this.noCulling();
    }

    private void switchReplay(int direction)
    {
        if (this.panel.getData() == null)
        {
            return;
        }

        List<Replay> list = this.panel.getData().replays.getList();

        int index = list.indexOf(this.getReplay());
        int newIndex = MathUtils.cycler(index + direction, list);
        Replay replay = list.get(newIndex);

        this.panel.replayEditor.setReplay(replay);
        UIUtils.playClick();
    }

    public boolean isInstantKeyframes()
    {
        return this.instantKeyframes;
    }

    public void toggleInstantKeyframes()
    {
        this.instantKeyframes = !this.instantKeyframes;
    }

    public boolean isCountdownControlEnabled()
    {
        return this.countdownControl;
    }

    public void toggleCountdownControl()
    {
        this.countdownControl = !this.countdownControl;
    }

    public boolean isPaused()
    {
        return this.paused;
    }

    public void setPaused(boolean paused)
    {
        this.paused = paused;
    }

    private void toggleMousePointer(boolean disable)
    {
        net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();

        if (disable)
        {
            GLFW.glfwSetInputMode(window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        }
        else
        {
            GLFW.glfwSetInputMode(window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    public ValueOnionSkin getOnionSkin()
    {
        return BBSSettings.editorOnionSkin;
    }

    private int getTick()
    {
        return this.panel.getCursor();
    }

    private Replay getReplay()
    {
        return this.panel.replayEditor.replays.replays.getCurrentFirst();
    }

    public StencilFormFramebuffer getStencil()
    {
        return this.stencil;
    }

    public IEntity getCurrentEntity()
    {
        if (this.panel.getData() == null)
        {
            return null;
        }

        Replay replay = this.panel.replayEditor.getReplay();

        if (replay == null)
        {
            return null;
        }

        int index = this.panel.getData().replays.getList().indexOf(replay);

        return this.getEntities().get(index);
    }

    public int getPovMode()
    {
        return this.pov % 6;
    }

    /**
     * Free camera modes do not write to camera clips or keyframes while flying.
     */
    public boolean isFreeCameraMode()
    {
        int mode = this.getPovMode();

        return mode == CAMERA_MODE_FREE || (mode == CAMERA_MODE_ORBIT && this.panel.isFlying());
    }

    public void setPov(int pov)
    {
        this.pov = pov;
        this.orbit.enabled = this.getPovMode() > 1;
    }

    private int getMouseMode()
    {
        return this.mouseMode % 6;
    }

    private void setMouseMode(int mode)
    {
        if (!ClientNetwork.isIsBBSModOnServer() && mode == 0)
        {
            mode = 1;

            this.getContext().notifyError(UIKeys.FILM_CONTROLLER_SERVER_WARNING);
        }

        this.mouseMode = mode;

        if (this.controlled != null)
        {
            /* Restore value of the mouse stick */
            int index = this.getMouseMode() - 1;

            if (index >= 0)
            {
                float[] variables = this.controlled.getExtraVariables();

                this.mouseStick.set(variables[index * 2 + 1], variables[index * 2]);
            }
        }
    }

    private boolean isMouseLookMode()
    {
        return this.getMouseMode() == 0;
    }

    public void createEntities()
    {
        this.stopRecording();

        if (this.controlled != null)
        {
            this.toggleControl();
        }

        if (this.panel.getData() == null)
        {
            this.editorController = null;
            return;
        }

        this.editorController = new FilmEditorController(this.panel.getData(), this);
        this.editorController.createEntities();

        IntObjectMap<IEntity> entities = this.panel.getRunner().getContext().entities;

        entities.clear();
        entities.putAll(this.editorController.getEntities());
    }

    public void createEntitiesNow()
    {
        this.createEntities();
    }

    public IntObjectMap<IEntity> getEntities()
    {
        return this.editorController == null ? new IntObjectHashMap<>() : this.editorController.getEntities();
    }

    public Map<String, Integer> getActors()
    {
        return this.actors;
    }

    public void updateActors(Map<String, Integer> actors)
    {
        this.actors = actors;
    }

    /* Character control state */

    public IEntity getControlled()
    {
        return this.controlled;
    }

    public boolean isControlling()
    {
        return this.controlled != null;
    }

    public void toggleControl()
    {
        this.getContext().unfocus();

        boolean replacePlayer = ClientNetwork.isIsBBSModOnServer();
        IntObjectMap<IEntity> entities = this.getEntities();

        if (this.controlled != null)
        {
            if (replacePlayer && this.previousEntity != null)
            {
                this.controlled.setForm(this.playerForm);

                entities.put(CollectionUtils.getKey(entities, this.controlled), this.previousEntity);
                this.previousEntity = null;
            }

            this.controlled = null;
        }
        else if (this.panel.replayEditor.replays.replays.isSelected())
        {
            this.controlled = this.getCurrentEntity();

            if (replacePlayer && this.controlled != null)
            {
                MCEntity player = Morph.getMorph(MinecraftClient.getInstance().player).entity;

                this.playerForm = player.getForm();
                this.previousEntity = this.controlled;

                player.copy(this.controlled);
                PlayerUtils.teleport(this.controlled.getX(), this.controlled.getY(), this.controlled.getZ(), this.controlled.getHeadYaw(), this.controlled.getBodyYaw(), this.controlled.getPitch());
                entities.put(CollectionUtils.getKey(entities, this.controlled), player);

                this.controlled = player;
            }
        }

        this.setMouseMode(this.mouseMode);
        this.toggleMousePointer(this.controlled != null);

        if (this.controlled == null && this.recording)
        {
            this.stopRecording();
        }
    }

    private boolean canControl()
    {
        UIContext context = this.getContext();

        return this.controlled != null && context != null && !this.hasBlockingOverlay();
    }

    /* Recording */

    public boolean isPlaying()
    {
        boolean playing = !this.hasBlockingOverlay() && this.panel.isRunning();

        if (this.isPaused())
        {
            playing = true;
        }

        return playing;
    }

    private boolean hasBlockingOverlay()
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return false;
        }

        List<UIOverlayPanel> overlays = context.menu.getRoot().getChildren(UIOverlayPanel.class);

        for (UIOverlayPanel panel : overlays)
        {
            if (!(panel instanceof UIReplaysOverlayPanel))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isRecording()
    {
        return this.recording;
    }

    public int getRecordingCountdown()
    {
        return this.recordingCountdown;
    }

    public List<String> getRecordingGroups()
    {
        return this.recordingGroups;
    }

    public void startRecording(List<String> groups)
    {
        if (this.panel.getData() == null)
        {
            return;
        }

        MobCaptureRecordingSetup setup = MobCaptureRecordingSetup.pending;
        MobCaptureRecordingSetup.pending = null;

        if (setup != null)
        {
            BBSModClient.getFilms().getEditorMobCapture().applyRecordingSetup(setup);
        }

        if (groups != null && groups.contains("outside"))
        {
            if (setup != null)
            {
                MobCaptureRecordingSetup.pending = setup;
            }

            MinecraftClient.getInstance().setScreen(null);

            Replay replay = this.panel.replayEditor.getReplay();
            int index = this.panel.getData().replays.getList().indexOf(replay);

            if (index >= 0)
            {
                BBSModClient.getFilms().startRecording(this.panel.getData(), index, this.panel.getCursor());
            }

            return;
        }

        if (setup != null && setup.shouldCapture())
        {
            BBSModClient.getFilms().getEditorMobCapture().bulkCapture(this.panel.getData(), this.panel.getCursor(), setup, this.panel);
        }

        this.recordingTick = this.getTick();
        this.recording = true;
        this.recordingCountdown = 30;
        this.recordingGroups = groups;

        this.recordingOld = this.getReplay().keyframes.toData();

        if (groups != null)
        {
            if (groups.contains(ReplayKeyframes.GROUP_LEFT_STICK))
            {
                this.setMouseMode(1);
            }
            else if (groups.contains(ReplayKeyframes.GROUP_RIGHT_STICK))
            {
                this.setMouseMode(2);
            }
            else if (groups.contains(ReplayKeyframes.GROUP_TRIGGERS))
            {
                this.setMouseMode(3);
            }
            else if (groups.contains(ReplayKeyframes.GROUP_EXTRA1))
            {
                this.setMouseMode(4);
            }
            else if (groups.contains(ReplayKeyframes.GROUP_EXTRA2))
            {
                this.setMouseMode(5);
            }
            else
            {
                this.setMouseMode(0);
            }
        }

        if (this.controlled == null)
        {
            this.toggleControl();
        }

        if (groups != null && !groups.contains(ReplayKeyframes.GROUP_POSITION))
        {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            this.wasAllowFlying = player.getAbilities().allowFlying;
            this.wasFlying = player.getAbilities().flying;
            this.flightModified = true;

            player.getAbilities().allowFlying = true;
            player.getAbilities().flying = true;
        }

        this.toggleMousePointer(this.controlled != null);
    }

    public void stopRecording()
    {
        if (!this.recording)
        {
            return;
        }

        this.recording = false;
        this.recordingGroups = null;

        if (this.controlled != null)
        {
            this.toggleControl();
        }

        if (this.flightModified)
        {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            player.getAbilities().allowFlying = this.wasAllowFlying;
            player.getAbilities().flying = this.wasFlying;
            this.flightModified = false;
        }

        this.panel.setCursor(this.recordingTick);

        if (this.panel.getRunner().isRunning())
        {
            this.panel.togglePlayback();
        }

        if (this.recordingCountdown > 0)
        {
            return;
        }

        Replay replay = this.getReplay();

        if (replay != null && this.recordingOld != null)
        {
            for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
            {
                channel.simplify();
            }

            BaseType newData = replay.keyframes.toData();

            replay.keyframes.fromData(this.recordingOld);
            replay.keyframes.preNotify();
            replay.keyframes.fromData(newData);
            replay.keyframes.postNotify();

            this.recordingOld = null;
        }

        if (this.panel.getData() != null)
        {
            BBSModClient.getFilms().getEditorMobCapture().simplify(this.panel.getData());
            BBSModClient.getFilms().getEditorProjectileCapture().simplify(this.panel.getData());
        }

        BBSModClient.getFilms().getEditorMobCapture().clear();
        BBSModClient.getFilms().getEditorProjectileCapture().clear();

        this.setMouseMode(ClientNetwork.isIsBBSModOnServer() ? 0 : 1);
    }

    /* Input handling */

    /**
     * Character control should capture mouse input only over the 3D preview viewport.
     * Clicks on editor panels (e.g. replay keyframe timeline) must still reach those widgets.
     */
    private boolean shouldConsumeControlMouse(UIContext context)
    {
        return this.panel.preview.getViewport().isInside(context);
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.canControl())
        {
            return this.shouldConsumeControlMouse(context);
        }

        if (this.tryPickHoveredReplay(context))
        {
            return true;
        }

        return super.subMouseClicked(context);
    }

    /**
     * Alt-hover replay selection click. {@code hoveredEntity} is only populated while Alt is held
     * during the picking preview pass (same as the backup src behaviour).
     */
    public boolean tryPickHoveredReplay(UIContext context)
    {
        if (this.canControl() || context.mouseButton != 0 || this.hoveredEntity == null)
        {
            return false;
        }

        this.pickEntity(this.hoveredEntity);

        return true;
    }

    private void pickEntity(IEntity entity)
    {
        if (this.panel.getData() == null)
        {
            return;
        }

        int index = CollectionUtils.getKey(this.getEntities(), entity);

        this.panel.replayEditor.setReplay(this.panel.getData().replays.getList().get(index));

        if (!this.panel.replayEditor.isVisible())
        {
            this.panel.showPanel(this.panel.replayEditor);
        }
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        if (this.canControl())
        {
            return this.shouldConsumeControlMouse(context);
        }

        this.orbit.stop();

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.canControl())
        {
            if (this.isControlling() && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.toggleControl();
                UIUtils.playClick();

                return true;
            }
            else if (context.getKeyAction() == KeyAction.PRESSED && context.getKeyCode() >= GLFW.GLFW_KEY_1 && context.getKeyCode() <= GLFW.GLFW_KEY_6)
            {
                /* Switch mouse input mode */
                this.setMouseMode(context.getKeyCode() - GLFW.GLFW_KEY_1);

                return true;
            }

            InputUtil.Key utilKey = InputUtil.fromKeyCode(context.getKeyCode(), context.getScanCode());

            if (this.canControlWithKeyboard(utilKey) && !(this.recording && this.recordingCountdown > 0 && !this.countdownControl))
            {
                return true;
            }
        }

        return super.subKeyPressed(context);
    }

    private boolean canControlWithKeyboard(InputUtil.Key utilKey)
    {
        if (!ClientNetwork.isIsBBSModOnServer())
        {
            return false;
        }

        GameOptions options = MinecraftClient.getInstance().options;

        return options.forwardKey.getDefaultKey() == utilKey
            || options.backKey.getDefaultKey() == utilKey
            || options.leftKey.getDefaultKey() == utilKey
            || options.rightKey.getDefaultKey() == utilKey
            || options.sneakKey.getDefaultKey() == utilKey
            || options.sprintKey.getDefaultKey() == utilKey
            || options.jumpKey.getDefaultKey() == utilKey;
    }

    public void pickRecording()
    {
        if (this.panel.replayEditor.getReplay() == null)
        {
            return;
        }

        if (this.recording)
        {
            this.stopRecording();

            return;
        }

        this.toggleMousePointer(false);

        this.openRecordOverlay();
    }

    private void openRecordOverlay()
    {
        UIRecordOverlayPanel panel = new UIRecordOverlayPanel(
            UIKeys.FILM_CONTROLLER_RECORD_TITLE,
            UIKeys.FILM_CONTROLLER_RECORD_DESCRIPTION,
            this::startRecording,
            true
        );
        /* Outside uses the same submit path as other groups: mob-capture panel only
         * when "Mob to morph" is toggled on. */
        UIIcon icon = new UIIcon(Icons.UPLOAD, (b) -> panel.submit(Arrays.asList("outside")));

        icon.tooltip(UIKeys.FILM_GROUPS_OUTSIDE);
        panel.bar.add(icon);
        panel.keys().register(Keys.RECORDING_GROUP_OUTSIDE, icon::clickItself);

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    public Icon getOrbitModeIcon()
    {
        return this.getOrbitModeIcon(this.getPovMode());
    }

    public Icon getOrbitModeIcon(int povMode)
    {
        if (povMode == UIFilmController.CAMERA_MODE_FREE) return Icons.REFRESH;
        else if (povMode == UIFilmController.CAMERA_MODE_ORBIT) return Icons.ORBIT;
        else if (povMode == UIFilmController.CAMERA_MODE_FIRST_PERSON) return Icons.VISIBLE;
        else if (povMode == UIFilmController.CAMERA_MODE_THIRD_PERSON_BACK) return Icons.ARROW_UP;
        else if (povMode == UIFilmController.CAMERA_MODE_THIRD_PERSON_FRONT) return Icons.ARROW_DOWN;

        return Icons.CAMERA;
    }

    public void toggleOrbitMode()
    {
        if (this.controlled != null)
        {
            this.setPov(this.pov + (Window.isShiftPressed() ? -1 : 1));

            return;
        }

        this.getContext().replaceContextMenu((menu) ->
        {
            menu.autoKeys();

            menu.action(this.getOrbitModeIcon(0), UIKeys.FILM_REPLAY_ORBIT_CAMERA, this.pov == CAMERA_MODE_CAMERA, () -> this.setPov(0));
            menu.action(this.getOrbitModeIcon(1), UIKeys.FILM_REPLAY_ORBIT_FREE, this.pov == CAMERA_MODE_FREE, () -> this.setPov(1));
            menu.action(this.getOrbitModeIcon(2), UIKeys.FILM_REPLAY_ORBIT_ORBIT, this.pov == CAMERA_MODE_ORBIT, () -> this.setPov(2));
            menu.action(this.getOrbitModeIcon(3), UIKeys.FILM_REPLAY_ORBIT_FIRST_PERSON, this.pov == CAMERA_MODE_FIRST_PERSON, () -> this.setPov(3));
            menu.action(this.getOrbitModeIcon(4), UIKeys.FILM_REPLAY_ORBIT_THIRD_PERSON_BACK, this.pov == CAMERA_MODE_THIRD_PERSON_BACK, () -> this.setPov(4));
            menu.action(this.getOrbitModeIcon(5), UIKeys.FILM_REPLAY_ORBIT_THIRD_PERSON_FRONT, this.pov == CAMERA_MODE_THIRD_PERSON_FRONT, () -> this.setPov(5));
        });
    }

    public void handleCamera(Camera camera, float transition)
    {
        if (this.orbit.enabled)
        {
            int mode = this.getPovMode();

            if (mode == CAMERA_MODE_ORBIT)
            {
                this.orbit.setup(camera, transition);

                camera.fov = BBSSettings.getFov();
            }
            else if (mode != CAMERA_MODE_FREE)
            {
                this.handleFirstThirdPerson(camera, transition, mode);
            }
        }
    }

    private void handleFirstThirdPerson(Camera camera, float transition, int mode)
    {
        IEntity controller = this.getCurrentEntity();

        if (controller == null)
        {
            return;
        }

        Vector3d position = new Vector3d();
        Vector3f rotation = new Vector3f();
        float distance = 5F;

        position.set(controller.getPrevX(), controller.getPrevY(), controller.getPrevZ());
        position.lerp(new Vector3d(controller.getX(), controller.getY(), controller.getZ()), transition);
        position.y += controller.getEyeHeight();

        rotation.set(controller.getPrevPitch(), controller.getPrevHeadYaw(), 0);
        rotation.lerp(new Vector3f(controller.getPitch(), controller.getHeadYaw(), 0), transition);

        rotation.x = MathUtils.toRad(rotation.x);
        rotation.y = MathUtils.toRad(rotation.y);

        if (mode == CAMERA_MODE_FIRST_PERSON)
        {
            camera.position.set(position);
            camera.rotation.set(rotation.x, rotation.y + MathUtils.PI, 0F);
            camera.fov = BBSSettings.getFov();

            return;
        }

        boolean back = mode == CAMERA_MODE_THIRD_PERSON_BACK;
        Vector3f rotate = Matrices.rotation(rotation.x * (back ? 1 : -1), (back ? 0F : MathUtils.PI) - rotation.y);
        World world = MinecraftClient.getInstance().world;

        HitResult result = RayTracing.rayTraceEntity(
            world,
            RayTracing.fromVector3d(position),
            RayTracing.fromVector3f(rotate),
            distance
        );

        if (result.getType() == HitResult.Type.BLOCK)
        {
            distance = (float) position.distance(result.getPos().x, result.getPos().y, result.getPos().z) - 0.1F;
        }

        rotate.mul(distance);
        position.add(rotate);

        camera.position.set(position);
        camera.rotation.set(rotation.x * (back ? -1 : 1), rotation.y + (back ? 0 : MathUtils.PI), 0);
        camera.fov = BBSSettings.getFov();
    }

    public void insertFrame()
    {
        Replay replay = this.getReplay();

        if (replay == null)
        {
            return;
        }

        if (Window.isCtrlPressed())
        {
            this.toggleMousePointer(false);

            UIRecordOverlayPanel panel = new UIRecordOverlayPanel(
                UIKeys.FILM_CONTROLLER_INSERT_FRAME_TITLE,
                UIKeys.FILM_CONTROLLER_INSERT_FRAME_DESCRIPTION,
                (groups) ->
                {
                    BaseValue.edit(replay.keyframes, (keyframes) ->
                    {
                        keyframes.record(this.getTick(), this.getCurrentEntity(), groups);
                    });
                }
            );

            panel.onClose((event) -> this.toggleMousePointer(this.controlled != null));

            UIOverlay.addOverlay(this.getContext(), panel);
        }
        else
        {
            List<String> chosenGroups = Arrays.asList(ReplayKeyframes.GROUP_POSITION, ReplayKeyframes.GROUP_ROTATION);

            if (this.mouseMode == 1) chosenGroups = Collections.singletonList(ReplayKeyframes.GROUP_LEFT_STICK);
            else if (this.mouseMode == 2) chosenGroups = Collections.singletonList(ReplayKeyframes.GROUP_RIGHT_STICK);
            else if (this.mouseMode == 3) chosenGroups = Collections.singletonList(ReplayKeyframes.GROUP_TRIGGERS);
            else if (this.mouseMode == 4) chosenGroups = Collections.singletonList(ReplayKeyframes.GROUP_EXTRA1);
            else if (this.mouseMode == 5) chosenGroups = Collections.singletonList(ReplayKeyframes.GROUP_EXTRA2);

            final List<String> groups = chosenGroups;

            BaseValue.edit(replay.keyframes, (keyframes) ->
            {
                List<Replay> replays = this.panel.getData().replays.getList();
                int index = replays.indexOf(replay);

                keyframes.record(this.getTick(), this.getCurrentEntity(), groups);
                RecorderMobCapture.recordMountKeyframes(replays, index, keyframes, this.getCurrentEntity(), this.getTick());
            });
        }
    }

    /* Update */

    public void update()
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        RunnerCameraController runner = this.panel.getRunner();

        this.handleRecording(runner);

        if (this.recording && this.recordingCountdown <= 0 && this.panel.isRunning())
        {
            BBSModClient.getFilms().getEditorMobCapture().recordTickForFilm(this.panel.getData(), this.panel.getCursor());
        }

        if (this.editorController != null)
        {
            this.editorController.update();
        }

        if (this.canControl())
        {
            this.updateControls();
        }
    }

    private void handleRecording(RunnerCameraController runner)
    {
        if (this.recording)
        {
            if (this.recordingCountdown > 0)
            {
                this.recordingCountdown -= 1;

                if (this.recordingCountdown <= 0)
                {
                    this.panel.togglePlayback();
                }
            }

            if (this.recordingCountdown <= 0)
            {
                boolean stopped = !runner.isRunning();

                if (BBSSettings.editorLoop.get())
                {
                    Vector2i loop = this.panel.getLoopingRange();
                    int min = loop.x;
                    int max = loop.y;
                    int ticks = this.panel.getCursor();

                    if (min >= 0 && max >= 0 && min < max && (ticks >= max - 1 || ticks < min) || stopped)
                    {
                        this.stopRecording();
                    }
                }
                else if (stopped)
                {
                    this.stopRecording();
                }
            }
        }
    }

    private void updateControls()
    {
        IEntity controller = this.controlled;

        if (!this.isMouseLookMode())
        {
            int index = this.getMouseMode() - 1;
            float[] extraVariables = controller.getExtraVariables();

            extraVariables[index * 2] = this.mouseStick.y;
            extraVariables[index * 2 + 1] = this.mouseStick.x;
        }

        if (this.instantKeyframes && this.panel.isRunning())
        {
            this.insertFrame();
        }
    }

    /* Render */

    public void renderHUD(UIContext context, Area area)
    {
        FontRenderer font = context.batcher.getFont();
        int mode = this.getMouseMode();

        if (this.controlled != null)
        {
            /* Render helpful guides for sticks and triggers controls */
            if (mode > 0)
            {
                String label = UIKeys.FILM_GROUPS_LEFT_STICK.get();

                if (mode == 2)
                {
                    label = UIKeys.FILM_GROUPS_RIGHT_STICK.get();
                }
                else if (mode == 3)
                {
                    label = UIKeys.FILM_GROUPS_TRIGGERS.get();
                }
                else if (mode == 4)
                {
                    label = UIKeys.FILM_GROUPS_EXTRA_1.get();
                }
                else if (mode == 5)
                {
                    label = UIKeys.FILM_GROUPS_EXTRA_2.get();
                }

                context.batcher.textCard(label, area.x + 5, area.ey() - 5 - font.getHeight(), Colors.WHITE, BBSSettings.primaryColor(Colors.A100));

                int ww = (int) (Math.min(area.w, area.h) * 0.75F);
                int hh = ww;
                int x = area.x + (area.w - ww) / 2;
                int y = area.y + (area.h - hh) / 2;
                int color = Colors.setA(Colors.WHITE, 0.5F);

                context.batcher.outline(x, y, x + ww, y + hh, color);

                int bx = area.x + area.w / 2 + (int) ((this.mouseStick.y) * ww / 2);
                int by = area.y + area.h / 2 + (int) ((this.mouseStick.x) * hh / 2);

                context.batcher.box(bx - 4, by - 4, bx + 4, by + 4, color);
            }

            /* Render recording overlay */
            if (this.recording)
            {
                int x = area.x + 5 + 16;
                int y = area.y + 5;

                context.batcher.icon(Icons.SPHERE, Colors.RED | Colors.A100, x, y, 1F, 0F);

                if (this.recordingCountdown <= 0)
                {
                    context.batcher.textCard(UIKeys.FILM_CONTROLLER_TICKS.format(this.getTick()).get(), x + 3, y + 4, Colors.WHITE, Colors.A50);
                }
                else
                {
                    context.batcher.textCard(String.valueOf(this.recordingCountdown / 20F), x + 3, y + 4, Colors.WHITE, Colors.A50);
                }
            }
        }

        int x = area.ex() - 4;
        int y = area.y + 5;

        if (this.panel.isFlying())
        {
            String label = UIKeys.FILM_CONTROLLER_SPEED.format(this.panel.dashboard.orbit.speed.getValue()).get();
            int w = font.getWidth(label);

            context.batcher.textCard(label, x - w, y, Colors.WHITE, Colors.A50);

            y += font.getHeight() + 7;
        }

        if (BBSSettings.editorFilmOverlayVisible.get())
        {
            Replay replay = this.panel.replayEditor.getReplay();

            if (replay != null)
            {
                String label = replay.getName();
                int w = font.getWidth(label);

                context.batcher.textCard(label, x - w, y, Colors.WHITE, Colors.A50);

                Form form = replay.form.get();

                if (form != null)
                {
                    x -= w + 35;
                    y -= 5;

                    context.batcher.clip(x, y - 10, 40, 40, context);

                    y -= 10;

                    FormUtilsClient.renderUI(form, context, x, y, x + 40, y + 40);

                    context.batcher.unclip(context);
                }
            }
        }

        if (this.canShowGizmo())
        {
            if (this.panel.hasLastGizmoMatrix)
            {
                /* Resolve camera-baked vs camera-free capture so the colored gizmo stays
                 * on the bone instead of sticking to the screen when orbiting. */
                Gizmo.composeVisualMatrix(this.panel.lastGizmoMatrix, BBSRendering.camera, this.panel.lastProjection, this.gizmoInterfaceMatrix);
                Gizmo.INSTANCE.lastGizmoMatrix.set(this.gizmoInterfaceMatrix);
                Gizmo.INSTANCE.hasGizmoMatrix = true;
                Gizmo.INSTANCE.renderInterface(context, this.panel.lastProjection, this.panel.preview.getViewport());
            }
        }
        else if (!Gizmo.INSTANCE.isDragging())
        {
            this.panel.hasLastGizmoMatrix = false;
        }

        this.renderPickingPreview(context, area);

        this.orbit.handleOrbiting(context);
    }

    private void renderPickingPreview(UIContext context, Area area)
    {
        if (this.panel.isFlying())
        {
            return;
        }

        if (this.worldRenderContext == null)
        {
            return;
        }

        boolean altPressed = Window.isAltPressed();

        RenderSystem.depthFunc(GL11.GL_LESS);

        /* Cache the global stuff */
        MatrixStackUtils.cacheMatrices();

        RenderSystem.setProjectionMatrix(this.panel.lastProjection, VertexSorter.BY_Z);

        /* Render the stencil */
        MatrixStack worldStack = this.worldRenderContext.matrixStack();
        if (worldStack != null)
        {
            worldStack.push();
            worldStack.loadIdentity();
            MatrixStackUtils.multiply(worldStack, BBSRendering.camera);
            this.renderStencil(this.worldRenderContext, context, altPressed);
            worldStack.pop();
        }
        else
        {
            Matrix4fStack mvStack = RenderSystem.getModelViewStack();
            mvStack.pushMatrix();
            mvStack.identity();
            mvStack.set(BBSRendering.camera);
            RenderSystem.applyModelViewMatrix();

            this.renderStencil(this.worldRenderContext, context, altPressed);

            mvStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

        /* Return back to orthographic projection */
        MatrixStackUtils.restoreMatrices();

        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        this.hoveredEntity = null;

        if (!this.stencil.hasPicked())
        {
            return;
        }

        int index = this.stencil.getIndex();
        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        Pair<Form, String> pair = this.stencil.getPicked();
        int w = texture.width;
        int h = texture.height;

        if (BBSSettings.replayMarkedBonesOnly.get() && !altPressed && !Window.isShiftPressed() && pair != null && pair.a instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);
            String poseGroup = model == null ? modelForm.model.get() : model.poseGroup;

            if (poseGroup == null || poseGroup.isEmpty())
            {
                poseGroup = model == null ? modelForm.model.get() : model.id;
            }

            if (UIPoseEditor.hasMarkedBones(poseGroup) && !UIPoseEditor.isMarkedBone(poseGroup, pair.b))
            {
                return;
            }
        }

        RenderSystem.enableBlend();

        int paletteIndex = altPressed ? this.stencil.getIndex() - Gizmo.STENCIL_HANDLE_MAX - 1 : 0;
        int highlight = altPressed
            ? BBSSettings.modelEditorAltHoverHighlight(paletteIndex)
            : BBSSettings.modelEditorHoverHighlight();

        context.batcher.drawPickerPreview(texture.id, index, highlight, area.x, area.y, area.w, area.h, w, h);

        if (altPressed)
        {
            int stencilIndex = this.stencil.getIndex() - Gizmo.STENCIL_HANDLE_MAX - 1;
            Replay replay = CollectionUtils.getSafe(this.panel.getData().replays.getList(), stencilIndex);

            if (replay != null && this.editorController != null && this.editorController.isReplayVisible(replay, replay.getTick(this.getTick())))
            {
                this.hoveredEntity = this.getEntities().get(stencilIndex);

                if (this.hoveredEntity != null)
                {
                    String label = replay.getName();

                    context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
                }
            }
        }
        else if (pair != null && pair.a != null)
        {
            String label = pair.a.getFormIdOrName();

            if (!pair.b.isEmpty())
            {
                label += " - " + pair.b;
            }

            context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
        }
    }

    public void startRenderFrame(float tickDelta)
    {
        if (this.editorController != null)
        {
            this.editorController.startRenderFrame(tickDelta);
        }
    }

    public void renderFrame(WorldRenderContext context)
    {
        this.worldRenderContext = context;

        RenderSystem.enableDepthTest();

        if (this.editorController != null)
        {
            this.editorController.render(context);
            this.renderDropItemTrajectory(context);

            int povMode = this.panel.getController().getPovMode();

            if (povMode != UIFilmController.CAMERA_MODE_CAMERA && BBSSettings.recordingCameraPreview.get())
            {
                RunnerCameraController runner = this.panel.getRunner();
                int tick = runner.ticks;
                int duration = runner.getContext().clips == null ? 0 : runner.getContext().clips.calculateDuration();

                Recorder.renderCameraPreviewTimeline(runner.getContext().clips, tick, context.tickCounter().getTickDelta(true), duration, runner.getPosition(), context.camera(), context.matrixStack());
            }
        }

        Mouse mouse = MinecraftClient.getInstance().mouse;
        int x = (int) mouse.getX();
        int y = (int) mouse.getY();

        if (this.canControl())
        {
            if (this.isMouseLookMode() && ClientNetwork.isIsBBSModOnServer())
            {
                float cursorDeltaX = (x - this.lastMouse.x) / 2F;
                float cursorDeltaY = (y - this.lastMouse.y) / 2F;

                MinecraftClient.getInstance().player.changeLookDirection(cursorDeltaX, cursorDeltaY);
            }
            else
            {
                /* Control sticks and triggers variables */
                float sensitivity = 100F;

                float xx = (y - this.lastMouse.y) / sensitivity;
                float yy = (x - this.lastMouse.x) / sensitivity;

                this.mouseStick.add(xx, yy);
                this.mouseStick.x = MathUtils.clamp(this.mouseStick.x, -1F, 1F);
                this.mouseStick.y = MathUtils.clamp(this.mouseStick.y, -1F, 1F);
            }
        }

        this.lastMouse.set(x, y);

        RenderSystem.disableDepthTest();
    }

    private void renderDropItemTrajectory(WorldRenderContext context)
    {
        Clip clip = this.panel.actionEditor == null ? null : this.panel.actionEditor.getClip();

        if (!(clip instanceof ItemDropActionClip itemDrop) || !itemDrop.trajectoryPreview.get())
        {
            return;
        }

        Replay replay = this.getReplay();
        World world = MinecraftClient.getInstance().world;

        if (replay == null || world == null)
        {
            return;
        }

        int actionTick = replay.getTick(itemDrop.tick.get());
        ReplayKeyframes keyframes = replay.keyframes;
        double replayX = keyframes.x.interpolate(actionTick);
        double replayY = keyframes.y.interpolate(actionTick);
        double replayZ = keyframes.z.interpolate(actionTick);
        double x = itemDrop.relative.get() ? replayX + itemDrop.posX.get() : itemDrop.posX.get();
        double y = itemDrop.relative.get() ? replayY + itemDrop.posY.get() : itemDrop.posY.get();
        double z = itemDrop.relative.get() ? replayZ + itemDrop.posZ.get() : itemDrop.posZ.get();
        double vx = itemDrop.velocityX.get();
        double vy = itemDrop.velocityY.get();
        double vz = itemDrop.velocityZ.get();
        double cx = context.camera().getPos().x;
        double cy = context.camera().getPos().y;
        double cz = context.camera().getPos().z;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        /* Preview path follows ItemEntity-like drag and gravity and stops on first block hit. */
        int primaryColor = BBSSettings.primaryColor.get() & 0x00FFFFFF;
        float baseR = ((primaryColor >> 16) & 0xFF) / 255F;
        float baseG = ((primaryColor >> 8) & 0xFF) / 255F;
        float baseB = (primaryColor & 0xFF) / 255F;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        MatrixStack stack = context.matrixStack();

        final int maxSteps = 80;
        final int subSteps = 4;
        final float thickness = 0.05F;
        boolean hit = false;
        float prevX = (float) (x - cx);
        float prevY = (float) (y - cy);
        float prevZ = (float) (z - cz);

        for (int i = 0; i < maxSteps; i++)
        {
            for (int s = 0; s < subSteps; s++)
            {
                double nextX = x + vx / subSteps;
                double nextY = y + vy / subSteps;
                double nextZ = z + vz / subSteps;
                Vec3d from = new Vec3d(x, y, z);
                Vec3d to = new Vec3d(nextX, nextY, nextZ);
                BlockHitResult hitResult = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, MinecraftClient.getInstance().player));

                if (hitResult.getType() == HitResult.Type.BLOCK)
                {
                    Vec3d pos = hitResult.getPos();

                    nextX = pos.x;
                    nextY = pos.y;
                    nextZ = pos.z;
                    hit = true;
                }

                float progress = Math.min(1F, (i + s / (float) subSteps) / (float) maxSteps);
                float fade = 1F - progress;
                float alpha = Math.max(0.16F, fade * 0.85F);
                float r = Math.min(1F, baseR * (1F + 0.18F * fade));
                float g = Math.min(1F, baseG * (1F + 0.18F * fade));
                float b = Math.min(1F, baseB * (1F + 0.18F * fade));
                float nextRenderX = (float) (nextX - cx);
                float nextRenderY = (float) (nextY - cy);
                float nextRenderZ = (float) (nextZ - cz);

                if ((nextRenderX - prevX) * (nextRenderX - prevX) + (nextRenderY - prevY) * (nextRenderY - prevY) + (nextRenderZ - prevZ) * (nextRenderZ - prevZ) < 0.000001F)
                {
                    hit = true;
                    break;
                }

                Draw.fillBoxTo(
                    builder,
                    stack,
                    prevX, prevY, prevZ,
                    nextRenderX, nextRenderY, nextRenderZ,
                    thickness,
                    r, g, b, alpha
                );

                x = nextX;
                y = nextY;
                z = nextZ;
                prevX = nextRenderX;
                prevY = nextRenderY;
                prevZ = nextRenderZ;

                if (hit)
                {
                    break;
                }
            }

            if (hit)
            {
                break;
            }

            vy -= 0.04D;
            vx *= 0.98D;
            vy *= 0.98D;
            vz *= 0.98D;
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public Pair<String, TransformOrientation> getBone()
    {
        UIKeyframeEditor keyframeEditor = this.panel.replayEditor.keyframeEditor;

        return keyframeEditor != null ? keyframeEditor.getBone() : null;
    }

    private boolean canShowGizmo()
    {
        return UIBaseMenu.renderAxes && !this.recording && this.getBone() != null;
    }

    private void renderStencil(WorldRenderContext renderContext, UIContext context, boolean altPressed)
    {
        if (this.panel.getData() == null)
        {
            this.stencil.clearPicking();

            return;
        }

        Area viewport = this.panel.preview.getAbsoluteViewport();

        if (!viewport.isInside(context.mouseX(), context.mouseY()) || this.controlled != null)
        {
            this.stencil.clearPicking();

            return;
        }

        IEntity entity = this.getCurrentEntity();

        if ((entity == null || (this.pov == CAMERA_MODE_FIRST_PERSON && entity == this.getCurrentEntity())) && !altPressed)
        {
            return;
        }

        this.ensureStencilFramebuffer();

        boolean isPlaying = this.isPlaying();
        Texture mainTexture = this.stencil.getFramebuffer().getMainTexture();
        int cursorTick = this.getTick();

        this.stencilMap.setup();
        this.stencilMap.setIncrement(!altPressed);
        this.stencilMap.allowedBones = null;

        /* stencil.apply() sets glViewport to the film/video size; save so UI scale stays correct. */
        int[] prevViewport = new int[4];

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
        this.stencil.apply();

        /* Closest bone along the cursor ray must win; glow/gizmo passes can leave depthMask off. */
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        if (altPressed)
        {
            for (Map.Entry<Integer, IEntity> entry : this.getEntities().entrySet())
            {
                Replay replay = CollectionUtils.getSafe(this.panel.getData().replays.getList(), entry.getKey());

                if (replay == null || this.editorController == null || !this.editorController.isReplayVisible(replay, replay.getTick(cursorTick)))
                {
                    continue;
                }

                this.stencilMap.objectIndex = entry.getKey() + Gizmo.STENCIL_HANDLE_MAX + 1;

                BaseFilmController.renderEntity(FilmControllerContext.instance
                    .setup(this.getEntities(), entry.getValue(), replay, renderContext)
                    .film(this.panel.getData())
                    .filmTick(cursorTick)
                    .transition(isPlaying ? renderContext.tickCounter().getTickDelta(false) : 0)
                    .stencil(this.stencilMap)
                    .relative(replay.relative.get()));
            }
        }
        else
        {
            /* Bone pick only the selected replay. Without Alt, limbs on other actors
             * must not be clickable (Alt is the way to target/switch other replays). */
            Pair<String, TransformOrientation> bone = this.getBone();
            int currentIndex = this.panel.replayEditor.replays.replays.getIndex();
            Replay currentReplay = CollectionUtils.getSafe(this.panel.getData().replays.getList(), currentIndex);
            boolean markedBonesOnly = BBSSettings.replayMarkedBonesOnly.get() && !Window.isShiftPressed();

            if (currentReplay != null && this.editorController != null
                && this.editorController.isReplayVisible(currentReplay, currentReplay.getTick(cursorTick)))
            {
                IEntity currentEntity = this.getEntities().get(currentIndex);

                if (currentEntity != null)
                {
                    this.stencilMap.allowedBones = null;

                    if (markedBonesOnly)
                    {
                        Form form = currentReplay.form.get();

                        if (form instanceof ModelForm modelForm)
                        {
                            ModelInstance model = ModelFormRenderer.getModel(modelForm);
                            String poseGroup = model == null ? modelForm.model.get() : model.poseGroup;

                            if (poseGroup == null || poseGroup.isEmpty())
                            {
                                poseGroup = model == null ? modelForm.model.get() : model.id;
                            }

                            if (UIPoseEditor.hasMarkedBones(poseGroup))
                            {
                                this.stencilMap.allowedBones = UIPoseEditor.getMarkedBones(poseGroup);
                            }
                        }
                    }

                    BaseFilmController.renderEntity(FilmControllerContext.instance
                        .setup(this.getEntities(), currentEntity, currentReplay, renderContext)
                        .film(this.panel.getData())
                        .filmTick(cursorTick)
                        .transition(isPlaying ? renderContext.tickCounter().getTickDelta(false) : 0)
                        .stencil(this.stencilMap)
                        .relative(currentReplay.relative.get())
                        .bone(bone != null ? bone.a : null, bone != null ? bone.b : TransformOrientation.PARENT));
                }
            }
        }

        int x = (int) ((context.mouseX() - viewport.x) / (float) viewport.w * mainTexture.width);
        int y = (int) ((1F - (context.mouseY() - viewport.y) / (float) viewport.h) * mainTexture.height);

        this.stencil.pick(x, y);
        this.stencil.unbind(this.stencilMap);
        this.panel.replayEditor.updateGizmoHover();

        /* Rebind the main target without clearing — beginWrite(true) wiped the film
         * preview every mouse move over the viewport (deferred translucents looked like flicker).
         * beginWrite(false) alone may not restore glViewport, which made the whole UI look zoomed. */
        BBSRendering.ensureMainFramebuffer();
        MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
    }

    private void ensureStencilFramebuffer()
    {
        this.stencil.setup(Link.bbs("stencil_film"));

        Texture mainTexture = this.stencil.getFramebuffer().getMainTexture();
        int w = BBSRendering.getVideoWidth();
        int h = BBSRendering.getVideoHeight();

        if (mainTexture.width != w || mainTexture.height != h)
        {
            this.stencil.resizeGUI(w, h);
        }
    }
}
