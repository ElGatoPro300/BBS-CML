package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.audio.Waveform;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.film.replays.overlays.UIAnimationToPoseOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UIReplaysEditor extends UIElement {

    private static final Map<String, Integer> COLORS = new HashMap<>();
    private static final Map<String, Icon> ICONS = new HashMap<>();
    private static String lastFilm = "";
    private static int lastReplay;

    public UIReplaysListPanel replaysList;
    public UIReplayPropertiesPanel replayProperties;

    public UIElement iconBar;
    public Map<ReplayCategory, UIIcon> tabButtons = new HashMap<>();
    private ReplayCategory category = ReplayCategory.PLAYER;

    /* Keyframes */
    public UIKeyframeEditor keyframeEditor;

    /* Clips */
    private UIFilmPanel filmPanel;
    private Film film;
    private Replay replay;
    private boolean timelineVisible = true;
    private boolean propertiesVisible = true;
    private Set<String> keys = new LinkedHashSet<>();
    private final Map<String, Set<String>> expandedPoseTabsByReplay = new HashMap<>();

    public enum ReplayCategory {
        PLAYER(
                Icons.PLAYER,
                L10n.lang("bbs.ui.film.replays.category.player"),
                L10n.lang("bbs.ui.film.replays.category.player.tooltip")
        ),
        MODEL(
                Icons.BLOCK,
                L10n.lang("bbs.ui.film.replays.category.model"),
                L10n.lang("bbs.ui.film.replays.category.model.tooltip")
        ),
        POSE(
                Icons.POSE,
                L10n.lang("bbs.ui.film.replays.category.pose"),
                L10n.lang("bbs.ui.film.replays.category.pose.tooltip")
        );

        public final Icon icon;
        public final IKey label;
        public final IKey tooltip;

        private ReplayCategory(Icon icon, IKey label, IKey tooltip) {
            this.icon = icon;
            this.label = label;
            this.tooltip = tooltip;
        }
    }

    static {
        COLORS.put("x", Colors.RED);
        COLORS.put("y", Colors.GREEN);
        COLORS.put("z", Colors.BLUE);
        COLORS.put("vX", Colors.RED);
        COLORS.put("vY", Colors.GREEN);
        COLORS.put("vZ", Colors.BLUE);
        COLORS.put("yaw", Colors.YELLOW);
        COLORS.put("pitch", Colors.CYAN);
        COLORS.put("bodyYaw", Colors.MAGENTA);

        COLORS.put("stick_lx", Colors.RED);
        COLORS.put("stick_ly", Colors.GREEN);
        COLORS.put("stick_rx", Colors.RED);
        COLORS.put("stick_ry", Colors.GREEN);
        COLORS.put("trigger_l", Colors.RED);
        COLORS.put("trigger_r", Colors.GREEN);
        COLORS.put("extra1_x", Colors.RED);
        COLORS.put("extra1_y", Colors.GREEN);
        COLORS.put("extra2_x", Colors.RED);
        COLORS.put("extra2_y", Colors.GREEN);

        COLORS.put("visible", Colors.WHITE & Colors.RGB);
        COLORS.put("pose", Colors.RED);
        COLORS.put("pose_overlay", Colors.ORANGE);
        COLORS.put("transform", Colors.GREEN);
        COLORS.put("transform_overlay", 0xaaff00);
        COLORS.put("color", Colors.INACTIVE);
        COLORS.put("lighting", Colors.YELLOW);
        COLORS.put("shape_keys", Colors.PINK);
        COLORS.put("actions", Colors.MAGENTA);

        COLORS.put("item_main_hand", Colors.ORANGE);
        COLORS.put("item_off_hand", Colors.ORANGE);
        COLORS.put("item_head", Colors.ORANGE);
        COLORS.put("item_chest", Colors.ORANGE);
        COLORS.put("item_legs", Colors.ORANGE);
        COLORS.put("item_feet", Colors.ORANGE);

        COLORS.put("user1", Colors.RED);
        COLORS.put("user2", Colors.ORANGE);
        COLORS.put("user3", Colors.GREEN);
        COLORS.put("user4", Colors.BLUE);
        COLORS.put("user5", Colors.RED);
        COLORS.put("user6", Colors.ORANGE);

        COLORS.put("frequency", Colors.RED);
        COLORS.put("count", Colors.GREEN);

        COLORS.put("settings", Colors.MAGENTA);
        COLORS.put("offset_x", Colors.RED);
        COLORS.put("offset_y", Colors.GREEN);
        COLORS.put("offset_z", Colors.BLUE);

        ICONS.put("x", Icons.X);
        ICONS.put("y", Icons.Y);
        ICONS.put("z", Icons.Z);

        ICONS.put("pitch", Icons.VERTICAL);
        ICONS.put("headYaw", Icons.HORIZONTAL);

        ICONS.put("visible", Icons.VISIBLE);
        ICONS.put("texture", Icons.MATERIAL);
        ICONS.put("pose", Icons.POSE);
        ICONS.put("transform", Icons.ALL_DIRECTIONS);
        ICONS.put("color", Icons.BUCKET);
        ICONS.put("lighting", Icons.LIGHT);
        ICONS.put("actions", Icons.CONVERT);
        ICONS.put("shape_keys", Icons.HEART_ALT);
        ICONS.put("text", Icons.FONT);

        ICONS.put("stick_lx", Icons.LEFT_STICK);
        ICONS.put("stick_rx", Icons.RIGHT_STICK);
        ICONS.put("trigger_l", Icons.TRIGGER);
        ICONS.put("extra1_x", Icons.CURVES);
        ICONS.put("extra2_x", Icons.CURVES);
        ICONS.put("item_main_hand", Icons.LIMB);

        ICONS.put("user1", Icons.PARTICLE);

        ICONS.put("paused", Icons.TIME);
        ICONS.put("frequency", Icons.STOPWATCH);
        ICONS.put("count", Icons.BUCKET);

        ICONS.put("settings", Icons.GEAR);
    }

    public static Icon getIcon(String key) {
        String topLevel = StringUtils.fileName(key);

        return ICONS.getOrDefault(topLevel, Icons.NONE);
    }

    public static int getColor(String key) {
        String topLevel = StringUtils.fileName(key);

        if (topLevel.startsWith("pose_overlay")) {
            return COLORS.get("pose_overlay");
        }
        if (topLevel.startsWith("transform_overlay")) {
            return COLORS.get("transform_overlay");
        }

        if (COLORS.containsKey(topLevel)) {
            return COLORS.get(topLevel);
        }
        return Colors.BLUE;
    }

    public static boolean renderBackground(
            UIContext context,
            UIKeyframes keyframes,
            Clips camera,
            int clipOffset
    ) {
        if (!BBSSettings.audioWaveformVisibleInKeyframes.get()) {
            return false;
        }

        Scale scale = keyframes.getXAxis();
        boolean renderedOnce = false;

        for (Clip clip : camera.get()) {
            if (clip instanceof AudioClip audioClip) {
                Link link = audioClip.audio.get();

                if (link == null) {
                    continue;
                }

                SoundBuffer buffer = BBSModClient.getSounds().get(link, true);

                if (buffer == null || buffer.getWaveform() == null) {
                    continue;
                }

                Waveform wave = buffer.getWaveform();

                if (wave != null) {
                    int audioOffset = audioClip.offset.get();
                    float offset = audioClip.tick.get() - clipOffset;
                    int duration = Math.min((int) (wave.getDuration() * 20), clip.duration.get());

                    int x1 = (int) scale.to(offset);
                    int x2 = (int) scale.to(offset + duration);

                    wave.render(
                            context.batcher,
                            Colors.WHITE,
                            x1,
                            keyframes.area.y + 15,
                            x2 - x1,
                            20,
                            TimeUtils.toSeconds(audioOffset),
                            TimeUtils.toSeconds(audioOffset + duration)
                    );

                    renderedOnce = true;
                }
            }
        }

        return renderedOnce;
    }

    public UIReplaysEditor(UIFilmPanel filmPanel) {
        this.filmPanel = filmPanel;
        this.replayProperties = new UIReplayPropertiesPanel(filmPanel);
        this.replaysList = new UIReplaysListPanel(filmPanel, (l) -> this.setReplay(l.isEmpty() ? null : l.get(0), false, false), this.replayProperties.getFormConsumer());
        this.replayProperties.attachReplayList(this.replaysList.replays);

        this.iconBar = new UIElement();
        this.iconBar.relative(this).x(0).y(0).h(20).row(0).resize();

        this.iconBar.add(
                new UIRenderable(context -> {
                    /* Render background matching track names container */
                    int labelWidth = this.getLabelWidth();
                    Area area = this.iconBar.area;

                    context.batcher.box(area.x, area.y, area.x + labelWidth, area.ey(), Colors.A100);

                    /* Render active tab indicator */
                    UIIcon activeIcon = this.tabButtons.get(this.category);

                    if (activeIcon != null) {
                        int color = BBSSettings.primaryColor.get();
                        Area iconArea = activeIcon.area;

                        context.batcher.box(
                                iconArea.x,
                                iconArea.ey() - 2,
                                iconArea.ex(),
                                iconArea.ey(),
                                Colors.A100 | color
                        );
                        context.batcher.gradientVBox(
                                iconArea.x,
                                iconArea.y,
                                iconArea.ex(),
                                iconArea.ey() - 2,
                                color,
                                Colors.A75 | color
                        );
                    }
                })
        );

        for (ReplayCategory category : ReplayCategory.values()) {
            UIIcon button = new UIIcon(category.icon, b -> this.setCategory(category));

            button.tooltip(category.tooltip, Direction.RIGHT);
            this.iconBar.add(button);
            this.tabButtons.put(category, button);
        }

        this.setCategory(ReplayCategory.PLAYER);

        this.keys()
                .register(Keys.REPLAYS_TAB_1, () -> this.setCategory(ReplayCategory.PLAYER))
                .category(UIKeys.FILM_REPLAY_TITLE);
        this.keys()
                .register(Keys.REPLAYS_TAB_2, () -> this.setCategory(ReplayCategory.MODEL))
                .category(UIKeys.FILM_REPLAY_TITLE);
        this.keys()
                .register(Keys.REPLAYS_TAB_3, () -> this.setCategory(ReplayCategory.POSE))
                .category(UIKeys.FILM_REPLAY_TITLE);

        this.add(this.iconBar);
        this.markContainer();
    }

    private void setCategory(ReplayCategory c) {
        this.category = c;
        this.updateChannelsList();
    }

    public ReplayCategory getCategory() {
        return this.category;
    }

    public void pickPlayerCategory()
    {
        if (this.category != ReplayCategory.PLAYER)
        {
            this.setCategory(ReplayCategory.PLAYER);
        }
    }

    private int getLabelWidth() {
        return this.keyframeEditor != null ? this.keyframeEditor.view.getLabelWidth() : UIKeyframes.LABEL_WIDTH_DEFAULT;
    }

    public void setFilm(Film film) {
        this.savePoseTabState(this.replay);
        this.expandedPoseTabsByReplay.clear();
        this.film = film;
        this.filmPanel.getController().orbit.clearReplayStates();

        if (film != null) {
            List<Replay> replays = film.replays.getList();
            int index = film.getId().equals(lastFilm) ? lastReplay : 0;

            if (!CollectionUtils.inRange(replays, index)) {
                index = 0;
            }

            this.replaysList.replays.refreshReplayList();
            this.setReplay(replays.isEmpty() ? null : replays.get(index), true, false);
        }
    }

    public Replay getReplay() {
        return this.replay;
    }

    public void setReplay(Replay replay) {
        this.setReplay(replay, true, false);
    }

    public void setReplay(Replay replay, boolean select, boolean resetOrbit) {
        this.savePoseTabState(this.replay);
        Replay previousReplay = this.replay;

        this.filmPanel.getController().orbit.saveReplayState(previousReplay);
        this.replay = replay;

        if (resetOrbit) {
            this.filmPanel.getController().orbit.reset();
        }
        else
        {
            this.filmPanel.getController().orbit.restoreReplayState(replay, true);
        }

        this.replayProperties.setReplay(replay);
        this.filmPanel.actionEditor.setClips(replay == null ? null : replay.actions);
        this.updateChannelsList();

        if (select && replay != null) {
            this.replaysList.replays.scrollToReplay(replay);
        }
    }

    public void moveReplay(double x, double y, double z) {
        if (this.replay != null) {
            int cursor = this.filmPanel.getCursor();

            this.replay.keyframes.x.insert(cursor, x);
            this.replay.keyframes.y.insert(cursor, y);
            this.replay.keyframes.z.insert(cursor, z);
        }
    }

    public void updateChannelsList() {
        UIKeyframes lastEditor = this.keyframeEditor != null ? this.keyframeEditor.view : null;

        if (this.keyframeEditor != null) {
            this.keyframeEditor.removeFromParent();
            this.keyframeEditor = null;
        }

        if (this.replay == null) {
            return;
        }

        List<UIKeyframeSheet> sheets = new ArrayList<>();
        Map<UIKeyframeSheet, List<UIKeyframeSheet>> poseTabs = new HashMap<>();
        Map<UIKeyframeSheet, Integer> poseTabDepths = new HashMap<>();
        boolean tabsEnabled = BBSSettings.editorReplayTabs.get();

        if (!tabsEnabled || this.category == ReplayCategory.PLAYER) {
            for (String key : ReplayKeyframes.CURATED_CHANNELS) {
                BaseValue value = this.replay.keyframes.get(key);
                KeyframeChannel channel = (KeyframeChannel) value;

                sheets.add(
                        new UIKeyframeSheet(getColor(key), false, channel, null).icon(ICONS.get(key))
                );
            }
        }

        /* Form properties */
        Form lastForm = null;
        List<UIKeyframeSheet> formSheets = new ArrayList<>();

        for (String key : FormUtils.collectPropertyPaths(this.replay.form.get())) {
            KeyframeChannel property = this.replay.properties.getOrCreate(this.replay.form.get(), key);
            String name = StringUtils.fileName(key);
            boolean isPose
                    = name.startsWith("transform")
                    || name.startsWith("pose")
                    || name.startsWith("pose_overlay")
                    || name.startsWith("shape_keys");

            if (property != null
                    && (!tabsEnabled
                    || (this.category == ReplayCategory.MODEL && !isPose)
                    || (this.category == ReplayCategory.POSE && isPose))) {
                BaseValueBasic formProperty = FormUtils.getProperty(this.replay.form.get(), key);
                Form form
                        = formProperty.getParent() instanceof Form ? (Form) formProperty.getParent() : null;

                if (form != lastForm) {
                    if (lastForm != null) {
                        this.flushForm(sheets, formSheets, lastForm, tabsEnabled, poseTabs, poseTabDepths);
                    }

                    lastForm = form;
                }

                UIKeyframeSheet sheet = new UIKeyframeSheet(
                        getColor(key),
                        false,
                        property,
                        formProperty
                );

                formSheets.add(sheet.icon(getIcon(key)));
            }
        }

        if (lastForm != null) {
            this.flushForm(sheets, formSheets, lastForm, tabsEnabled, poseTabs, poseTabDepths);
        }

        this.keys.clear();

        for (UIKeyframeSheet sheet : sheets) {
            this.keys.add(sheet.isBoneTrack ? sheet.title.get() : StringUtils.fileName(sheet.id));
        }

        Set<String> disabled = BBSSettings.disabledSheets.get();

        sheets.removeIf(v -> {
            String filterKey = v.isBoneTrack ? v.title.get() : StringUtils.fileName(v.id);
            for (String s : disabled) {
                if (filterKey.equals(s) || v.id.equals(s) || v.id.endsWith("/" + s)) {
                    return true;
                }
            }

            return false;
        });

        lastForm = null;

        for (UIKeyframeSheet sheet : sheets) {
            Form form = sheet.property == null ? null : FormUtils.getForm(sheet.property);

            if (!Objects.equals(lastForm, form)) {
                sheet.separator = true;
            }

            lastForm = form;
        }

        if (!sheets.isEmpty()) {
            this.keyframeEditor = new UIKeyframeEditor(consumer
                    -> new UIFilmKeyframes(this.filmPanel.cameraEditor, consumer).absolute()
            )
                    .target(this.filmPanel.editArea)
                    .editPanelTopOffset(this.filmPanel::getEditPanelTopOffsetPx);
            this.keyframeEditor.relative(this).x(0).y(0).w(1F).h(1F);
            this.keyframeEditor.setUndoId("replay_keyframe_editor");
            this.keyframeEditor.setTimelineVisible(this.timelineVisible);
            this.keyframeEditor.setPropertiesVisible(this.propertiesVisible);

            /* Update iconBar width to match label width */
            int labelWidth = this.keyframeEditor.view.getLabelWidth();
            this.iconBar.relative(this).x(0).y(0).w(labelWidth).h(20);

            /* Reset */
            if (lastEditor != null) {
                this.keyframeEditor.view.copyViewport(lastEditor);
            }

            this.keyframeEditor.view.backgroundRenderer(context -> {
                UIKeyframes view = this.keyframeEditor.view;
                boolean yes = renderBackground(context, view, this.film.camera, 0);
                int shift = yes ? 35 : 15;

                UIClipsPanel cameraEditor = this.filmPanel.cameraEditor;
                Clip clip = cameraEditor.getClip();

                if (clip != null && BBSSettings.editorClipPreview.get()) {
                    IUIClipRenderer<Clip> renderer = cameraEditor.clips.getRenderers().get(clip);
                    Scale scale = view.getXAxis();
                    Area area = new Area();

                    float offset = clip.tick.get();
                    int duration = clip.duration.get();
                    int x1 = (int) scale.to(offset);
                    int x2 = (int) scale.to(offset + duration);

                    area.setPoints(x1, view.area.y + shift, x2, view.area.y + shift + 20);
                    renderer.renderClip(context, cameraEditor.clips, clip, area, true, true);
                }
            });
            this.keyframeEditor.view.duration(() -> this.film.camera.calculateDuration());
            this.keyframeEditor.view.context(menu -> {
                if (this.replay.form.get() instanceof ModelForm modelForm) {
                    int mouseY = this.getContext().mouseY;
                    UIKeyframeSheet sheet = this.keyframeEditor.view.getGraph().getSheet(mouseY);

                    if (sheet != null
                            && sheet.channel.getFactory() == KeyframeFactories.POSE
                            && sheet.id.equals("pose")) {
                        menu.action(Icons.POSE, UIKeys.FILM_REPLAY_CONTEXT_ANIMATION_TO_KEYFRAMES, () -> {
                            ModelInstance model = ModelFormRenderer.getModel(modelForm);

                            if (model != null) {
                                UIOverlay.addOverlay(
                                        this.getContext(),
                                        new UIAnimationToPoseOverlayPanel(
                                                (animationKey, onlyKeyframes, length, step) -> {
                                                    int current = this.filmPanel.getCursor();
                                                    IEntity entity = this.filmPanel.getController().getCurrentEntity();

                                                    UIReplaysEditorUtils.animationToPoseKeyframes(
                                                            this.keyframeEditor,
                                                            sheet,
                                                            modelForm,
                                                            entity,
                                                            current,
                                                            animationKey,
                                                            onlyKeyframes,
                                                            length,
                                                            step
                                                    );
                                                },
                                                modelForm,
                                                sheet
                                        ),
                                        200,
                                        197
                                );
                            }
                        });
                    }

                    boolean isPoseTrack
                            = sheet != null
                            && sheet.channel.getFactory() == KeyframeFactories.POSE
                            && (sheet.id.equals("pose")
                            || sheet.id.endsWith(FormUtils.PATH_SEPARATOR + "pose"))
                            && !sheet.id.contains("pose_overlay");

                    Form sheetForm = sheet != null && sheet.property != null ? FormUtils.getForm(sheet.property) : null;
                    boolean limbTracksOn = sheetForm instanceof ModelForm m && m.boneTracks.get();

                    if (isPoseTrack && sheet.selection.hasAny() && limbTracksOn) {
                        ModelForm poseModelForm = sheetForm instanceof ModelForm m ? m : modelForm;
                        menu.action(Icons.LIMB, UIKeys.FILM_REPLAY_CONTEXT_POSES_TO_LIMBS, () -> {
                            UIReplaysEditorUtils.posesToLimbTracks(this.replay, sheet, poseModelForm);

                            sheet.selection.removeSelected();
                            this.updateChannelsList();
                        });
                    }
                }

                if (this.keyframeEditor.view.getGraph() instanceof UIKeyframeDopeSheet) {
                    menu.action(Icons.FILTER, UIKeys.FILM_REPLAY_FILTER_SHEETS, () -> {
                        Set<String> disabledSet = BBSSettings.disabledSheets.get();
                        Map<String, Integer> keyToColor = new HashMap<>();
                        for (UIKeyframeSheet sheet : this.keyframeEditor.view.getGraph().getSheets()) {
                            String k = sheet.isBoneTrack
                                    ? sheet.title.get()
                                    : StringUtils.fileName(sheet.id);
                            keyToColor.put(k, sheet.color);
                        }
                        UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(
                                disabledSet,
                                this.keys,
                                keyToColor
                        );

                        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);

                        panel.onClose(e -> {
                            BBSSettings.disabledSheets.set(disabledSet);
                            this.updateChannelsList();
                        });
                    });
                }
            });

            for (UIKeyframeSheet sheet : sheets) {
                this.keyframeEditor.view.addSheet(sheet);
            }

            Set<String> expandedPoseIds = this.expandedPoseTabsByReplay.getOrDefault(
                this.replay == null ? "" : this.replay.getId(),
                Collections.emptySet()
            );
            this.keyframeEditor.view.getDopeSheet().configurePoseTabs(poseTabs, poseTabDepths, expandedPoseIds);

            this.add(this.keyframeEditor);
            /* Icon bar on top so it overlays the track names column (left labelWidth pixels) */
            if (this.iconBar.getParent() != null) {
                this.iconBar.removeFromParent();
            }
            this.add(this.iconBar);
        }

        this.resize();

        if (this.keyframeEditor != null && lastEditor == null) {
            this.keyframeEditor.view.resetView();
        }
    }

    private void flushForm(
            List<UIKeyframeSheet> sheets,
            List<UIKeyframeSheet> formSheets,
            Form form,
            boolean tabsEnabled,
            Map<UIKeyframeSheet, List<UIKeyframeSheet>> poseTabs,
            Map<UIKeyframeSheet, Integer> poseTabDepths
    ) {
        String path = FormUtils.getPath(form);
        String poseId = path.isEmpty() ? "pose" : path + FormUtils.PATH_SEPARATOR + "pose";
        UIKeyframeSheet poseSheet = null;

        for (UIKeyframeSheet sheet : formSheets)
        {
            if (poseId.equals(sheet.id) && sheet.channel.getFactory() == KeyframeFactories.POSE)
            {
                poseSheet = sheet;
                break;
            }
        }

        List<UIKeyframeSheet> orderedFormSheets = new ArrayList<>(formSheets);
        formSheets.clear();

        if ((!tabsEnabled || this.category == ReplayCategory.POSE)
                && form instanceof ModelForm modelForm) {
            List<UIKeyframeSheet> boneSheets = new ArrayList<>();
            Map<String, Integer> depthBySheetId = new HashMap<>();
            UIReplaysEditorUtils.addBoneTrackSheets(modelForm, this.replay.properties, boneSheets, depthBySheetId);

            for (UIKeyframeSheet boneSheet : boneSheets)
            {
                Integer depth = depthBySheetId.get(boneSheet.id);
                poseTabDepths.put(boneSheet, depth == null ? 0 : depth);
            }

            if (poseSheet != null && !boneSheets.isEmpty())
            {
                poseTabs.put(poseSheet, boneSheets);

                int poseIndex = orderedFormSheets.indexOf(poseSheet);

                if (poseIndex >= 0)
                {
                    orderedFormSheets.addAll(poseIndex + 1, boneSheets);
                }
                else
                {
                    orderedFormSheets.addAll(boneSheets);
                }
            }
            else
            {
                orderedFormSheets.addAll(boneSheets);
            }
        }

        sheets.addAll(orderedFormSheets);
    }

    private void savePoseTabState(Replay replay)
    {
        if (replay == null || this.keyframeEditor == null)
        {
            return;
        }

        this.expandedPoseTabsByReplay.put(replay.getId(), this.keyframeEditor.view.getDopeSheet().getExpandedPoseTabIds());
    }

    /**
     * Re-applies keyframe parameters panel position (e.g. after layout lock
     * toggle).
     */
    public void refreshEditPanelOffset() {
        if (this.keyframeEditor != null) {
            this.keyframeEditor.refreshEditPanelOffset();
        }

        if (this.replaysList != null)
        {
            this.replaysList.refreshEditPanelOffset();
        }

        if (this.replayProperties != null)
        {
            this.replayProperties.refreshEditPanelOffset();
        }
    }

    public void setTimelineVisible(boolean visible)
    {
        this.timelineVisible = visible;

        if (this.keyframeEditor != null)
        {
            this.keyframeEditor.setTimelineVisible(visible);
        }
    }

    public void setPropertiesVisible(boolean visible)
    {
        this.propertiesVisible = visible;

        if (this.keyframeEditor != null)
        {
            this.keyframeEditor.setPropertiesVisible(visible);
        }
    }

    public void pickForm(Form form, String bone) {
        UIReplaysEditorUtils.pickForm(this.keyframeEditor, this.filmPanel, form, bone);
    }

    public boolean clickViewport(UIContext context, Area area) {
        if (this.filmPanel.isFlying() && area.isInside(context)) {
            if (context.mouseButton == 0 && this.filmPanel.getController().orbit.enabled) {
                this.filmPanel.getController().orbit.start(context);

                return true;
            }
            if (context.mouseButton == 2) {
                if (Window.isKeyPressed(Keys.FLIGHT_ORBIT.getMainKey())
                        && this.filmPanel.getController().orbit.enabled) {
                    this.filmPanel.getController().orbit.start(context);
                } else {
                    this.filmPanel.dashboard.orbit.start(2, context.mouseX, context.mouseY);
                }

                return true;
            }
        }

        if (this.filmPanel.isFlying()) {
            return false;
        }

        StencilFormFramebuffer stencil = this.filmPanel.getController().getStencil();

        if (stencil.hasPicked()) {
            Pair<Form, String> pair = stencil.getPicked();

            if (pair != null && context.mouseButton < 2) {
                if (!this.isVisible()) {
                    this.filmPanel.showPanel(this);
                }

                if (Gizmo.INSTANCE.start(
                        stencil.getIndex(),
                        context.mouseX,
                        context.mouseY,
                        UIReplaysEditorUtils.getEditableTransform(this.keyframeEditor)
                )) {
                    return true;
                }

                if (context.mouseButton == 0) {
                    if (Window.isCtrlPressed()) {
                        UIReplaysEditorUtils.offerAdjacent(
                                this.getContext(),
                                pair.a,
                                pair.b,
                                bone -> this.pickForm(pair.a, bone)
                        ); 
                    }else if (Window.isShiftPressed()) {
                        UIReplaysEditorUtils.offerHierarchy(
                                this.getContext(),
                                pair.a,
                                pair.b,
                                bone -> this.pickForm(pair.a, bone)
                        ); 
                    }else {
                        this.pickForm(pair.a, pair.b);
                    }

                    return true;
                } else if (context.mouseButton == 1) {
                    if (Window.isCtrlPressed()) {
                        UIReplaysEditorUtils.offerAdjacent(this.getContext(), pair.a, pair.b, bone
                                -> UIReplaysEditorUtils.pickForm(
                                        this.keyframeEditor,
                                        this.filmPanel,
                                        pair.a,
                                        bone,
                                        true
                                )
                        );

                        return true;
                    } else if (Window.isShiftPressed()) {
                        UIReplaysEditorUtils.offerHierarchy(this.getContext(), pair.a, pair.b, bone
                                -> UIReplaysEditorUtils.pickForm(
                                        this.keyframeEditor,
                                        this.filmPanel,
                                        pair.a,
                                        bone,
                                        true
                                )
                        );

                        return true;
                    } else {
                        UIReplaysEditorUtils.pickForm(
                                this.keyframeEditor,
                                this.filmPanel,
                                pair.a,
                                pair.b,
                                true
                        );

                        return true;
                    }
                }
            }
        } else if (context.mouseButton == 1 && this.isVisible()) {
            World world = MinecraftClient.getInstance().world;
            Camera camera = this.filmPanel.getCamera();

            BlockHitResult blockHitResult = RayTracing.rayTrace(
                    world,
                    RayTracing.fromVector3d(camera.position),
                    RayTracing.fromVector3f(
                            CameraUtils.getMouseDirection(
                                    camera.projection,
                                    camera.view,
                                    context.mouseX,
                                    context.mouseY,
                                    area.x,
                                    area.y,
                                    area.w,
                                    area.h
                            )
                    ),
                    256F
            );

            if (blockHitResult.getType() != HitResult.Type.MISS) {
                Vector3d vec = new Vector3d(
                        blockHitResult.getPos().x,
                        blockHitResult.getPos().y,
                        blockHitResult.getPos().z
                );

                if (Window.isShiftPressed()) {
                    vec = new Vector3d(
                            Math.floor(vec.x) + 0.5D,
                            Math.round(vec.y),
                            Math.floor(vec.z) + 0.5D
                    );
                }

                final Vector3d finalVec = vec;

                context.replaceContextMenu(menu -> {
                    float pitch = 0F;
                    float yaw = MathUtils.toDeg(camera.rotation.y);

                    menu.action(Icons.ADD, UIKeys.FILM_REPLAY_CONTEXT_ADD, ()
                            -> this.replaysList.replays.addReplay(finalVec, pitch, yaw)
                    );
                    menu.action(Icons.POINTER, UIKeys.FILM_REPLAY_CONTEXT_MOVE_HERE, ()
                            -> this.moveReplay(finalVec.x, finalVec.y, finalVec.z)
                    );
                });

                return true;
            }
        }

        if (area.isInside(context) && this.filmPanel.getController().orbit.enabled) {
            this.filmPanel.getController().orbit.start(context);

            return true;
        }

        return false;
    }

    public void close() {
        if (this.film != null) {
            lastFilm = this.film.getId();
            Replay r = this.getReplay();

            lastReplay = r == null ? 0 : this.film.replays.getList().indexOf(r);
        }
    }

    public void teleport() {
        if (this.filmPanel.getData() == null) {
            return;
        }

        Replay replay = this.getReplay();

        if (replay != null) {
            int tick = this.filmPanel.getCursor();
            double x = replay.keyframes.x.interpolate(tick);
            double y = replay.keyframes.y.interpolate(tick);
            double z = replay.keyframes.z.interpolate(tick);
            float yaw = replay.keyframes.yaw.interpolate(tick).floatValue();
            float headYaw = replay.keyframes.headYaw.interpolate(tick).floatValue();
            float bodyYaw = replay.keyframes.bodyYaw.interpolate(tick).floatValue();
            float pitch = replay.keyframes.pitch.interpolate(tick).floatValue();
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            PlayerUtils.teleport(x, y, z, headYaw, pitch);
            player.setYaw(yaw);
            player.setHeadYaw(headYaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        }
    }

    @Override
    public void render(UIContext context) {
        /* Hide category bar when tabs are disabled or "edit track" overlay is open */
        this.iconBar.setVisible(
            this.timelineVisible
            && BBSSettings.editorReplayTabs.get()
                && (this.keyframeEditor == null || !this.keyframeEditor.view.isEditing())
        );

        super.render(context);
    }

    @Override
    public void resize() {
        super.resize();

        /* Update iconBar width when resizing */
        if (this.keyframeEditor != null) {
            int labelWidth = this.keyframeEditor.view.getLabelWidth();
            this.iconBar.relative(this).x(0).y(0).w(labelWidth).h(20);
        }
    }

    @Override
    public void applyUndoData(MapType data) {
        super.applyUndoData(data);

        List<Integer> selection = DataStorageUtils.intListFromData(data.getList("selection"));
        List<Integer> currentIndices = this.replaysList.replays.getCurrentIndices();

        this.setReplay(
                CollectionUtils.getSafe(this.film.replays.getList(), data.getInt("replay")),
                true,
                false
        );

        currentIndices.clear();
        currentIndices.addAll(selection);
        this.replaysList.replays.update();
    }

    @Override
    public void collectUndoData(MapType data) {
        super.collectUndoData(data);

        int index = this.film.replays.getList().indexOf(this.getReplay());

        data.putInt("replay", index);
        data.put(
                "selection",
                DataStorageUtils.intListToData(this.replaysList.replays.getCurrentIndices())
        );
    }
}
