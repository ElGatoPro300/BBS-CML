package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.Operation;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.toolbar.KeyframeDuplicateInteractionState;
import mchorse.bbs_mod.ui.film.toolbar.KeyframeInsertInteractionState;
import mchorse.bbs_mod.ui.film.toolbar.KeyframePasteInteractionState;
import mchorse.bbs_mod.ui.film.toolbar.KeyframeSelectNeighborInteractionState;
import mchorse.bbs_mod.ui.film.toolbar.KeyframeSelectSameInteractionState;
import mchorse.bbs_mod.ui.film.toolbar.TimelineInteractionHints;
import mchorse.bbs_mod.ui.film.toolbar.TimelineInteractionState;
import mchorse.bbs_mod.ui.film.toolbar.TimelineToolbarPointerBlock;
import mchorse.bbs_mod.ui.film.toolbar.TimelineToolbarSettings;
import mchorse.bbs_mod.ui.film.toolbar.TimelineTrackEligibility;
import mchorse.bbs_mod.ui.film.toolbar.UIInteractionModeOverlay;
import mchorse.bbs_mod.ui.film.toolbar.UIKeyframeDuplicateInteraction;
import mchorse.bbs_mod.ui.film.toolbar.UIKeyframeInsertInteraction;
import mchorse.bbs_mod.ui.film.toolbar.UIKeyframePasteInteraction;
import mchorse.bbs_mod.ui.film.toolbar.UIKeyframeSelectNeighborInteraction;
import mchorse.bbs_mod.ui.film.toolbar.UIKeyframeSelectSameInteraction;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.KeyframeType;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.presets.PresetManager;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class UIKeyframes extends UIElement
{
    private static final int RULER_HEIGHT = 16;

    /* Editing states */

    private boolean selecting;
    private boolean navigating;
    private int dragging = -1;
    private Pair<Keyframe, KeyframeType> draggingData;
    private boolean scaling;
    private boolean scalingShowInteractionHints;
    private float scalingAnchor;
    private Map<Keyframe, Float> scaleTicks = new HashMap<>();
    private boolean single;

    private boolean stacking;
    private boolean stackingShowInteractionHints;
    private float stackOffset;

    private int lastX;
    private int lastY;
    private int originalX;
    private int originalY;
    private float originalT;
    private Object originalV;

    private Runnable changeCallback;

    /* Fields */

    protected final UIKeyframeDopeSheet dopeSheet = new UIKeyframeDopeSheet(this);
    protected IUIKeyframeGraph currentGraph = this.dopeSheet;

    private final Scale xAxis = new Scale(this.area, ScrollDirection.HORIZONTAL);

    private final Consumer<Keyframe> callback;
    private Consumer<UIContext> backgroundRender;
    private Supplier<Integer> duration;

    private SheetCache cache;

    private UICopyPasteController copyPasteController;
    private UIDraggable sidebarResizer;
    private final UIInteractionModeOverlay interactionOverlay = new UIInteractionModeOverlay();
    private final UIKeyframeInsertInteraction insertInteraction = new UIKeyframeInsertInteraction();
    private final UIKeyframeDuplicateInteraction duplicateInteraction = new UIKeyframeDuplicateInteraction();
    private final UIKeyframePasteInteraction pasteInteraction = new UIKeyframePasteInteraction();
    private final UIKeyframeSelectNeighborInteraction selectNeighborInteraction = new UIKeyframeSelectNeighborInteraction();
    private final UIKeyframeSelectSameInteraction selectSameInteraction = new UIKeyframeSelectSameInteraction();

    public UIKeyframes(Consumer<Keyframe> callback)
    {
        this.callback = callback;

        this.copyPasteController = new UICopyPasteController(PresetManager.KEYFRAMES, "_CopyKeyframes")
            .supplier(this::serializeKeyframes)
            .consumer((data, mouseX, mouseY) ->
            {
                double offset = Math.round(this.fromGraphX(mouseX));

                this.pasteKeyframes(parseKeyframes(data), (float) offset, mouseY);
            })
            .canCopy(() -> this.currentGraph.getSelected() != null);

        /* Context menu items */
        this.context((menu) ->
        {
            if (this.interactionOverlay.isActive() || this.insertInteraction.isActive()
                || this.duplicateInteraction.isActive() || this.pasteInteraction.isActive()
                || this.selectNeighborInteraction.isActive() || this.selectSameInteraction.isActive()
                || this.isTransformInteractionBlockingContext())
            {
                return;
            }

            UIContext context = this.getContext();
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            boolean hasSelected = this.currentGraph.getSelected() != null;

            UIKeyframeSheet currentSheet = this.dopeSheet.getSheet(this.getContext().mouseY);
            boolean isGroup = currentSheet != null && currentSheet.groupHeader;

            if (!isGroup)
            {
                menu.custom(new UIPresetContextMenu(this.copyPasteController, mouseX, mouseY)
                    .labels(UIKeys.KEYFRAMES_CONTEXT_COPY, UIKeys.KEYFRAMES_CONTEXT_PASTE));
            }

            if (!this.single)
            {
                if (this.isEditing())
                {
                    menu.action(Icons.CLOSE, UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK, () -> this.editSheet(null));
                }
                else
                {
                    UIKeyframeSheet sheet = this.dopeSheet.getSheet(this.getContext().mouseY);

                    if (sheet != null && KeyframeFactories.isNumeric(sheet.channel.getFactory()) && !sheet.groupHeader)
                    {
                        menu.action(Icons.EDIT, UIKeys.KEYFRAMES_CONTEXT_EDIT_TRACK.format(sheet.id), () -> this.editSheet(sheet));
                    }
                }
            }

            if (!isGroup)
            {
                menu.action(Icons.SEARCH, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES, () -> this.adjustValues());
                menu.action(Icons.ARROW_LEFT, UIKeys.KEYFRAMES_KEYS_SELECT_LEFT, () -> this.selectAfter(mouseX, mouseY, -1));
                menu.action(Icons.ARROW_RIGHT, UIKeys.KEYFRAMES_KEYS_SELECT_RIGHT, () -> this.selectAfter(mouseX, mouseY, 1));
            }

            menu.action(Icons.MAXIMIZE, UIKeys.KEYFRAMES_CONTEXT_MAXIMIZE, this::resetView);

            if (!isGroup)
            {
                menu.action(Icons.FULLSCREEN, UIKeys.KEYFRAMES_CONTEXT_SELECT_ALL, () -> this.currentGraph.selectAll());
            }

            if (hasSelected)
            {
                menu.action(Icons.CURVES, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION, this::interpolationMenu);
                menu.action(Icons.CONVERT, UIKeys.KEYFRAMES_CONTEXT_SPREAD, this::spreadKeyframes);
                menu.action(Icons.OUTLINE_SPHERE, UIKeys.KEYFRAMES_CONTEXT_ROUND, () ->
                {
                    for (UIKeyframeSheet sheet : this.getGraph().getSheets())
                    {
                        List<Keyframe> selected = sheet.selection.getSelected();

                        if (selected.isEmpty())
                        {
                            continue;
                        }

                        sheet.channel.preNotify();

                        for (Keyframe kf : selected)
                        {
                            kf.setTick(Math.round(kf.getTick()), false);
                        }

                        sheet.channel.postNotify();
                    }
                });
                menu.action(Icons.REMOVE, UIKeys.KEYFRAMES_CONTEXT_REMOVE, () -> this.currentGraph.removeSelected());
            }
        });

        /* Keys */
        IKey category = UIKeys.KEYFRAMES_KEYS_CATEGORY;
        Supplier<Boolean> canModify = () -> !this.scaling;

        this.keys().register(Keys.KEYFRAMES_MAXIMIZE, this::resetView).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_SELECT_ALL, () -> this.currentGraph.selectAll()).inside().category(category).active(canModify);
        this.keys().register(Keys.COPY, () ->
        {
            if (this.copyPasteController.copy()) UIUtils.playClick();
        }).inside().category(category);
        this.keys().register(Keys.CUT, () ->
        {
            if (this.currentGraph.getSelected() == null)
            {
                this.getContext().notifyError(UIKeys.GENERAL_CUT_EMPTY);
                return;
            }

            if (this.copyPasteController.copy())
            {
                this.currentGraph.removeSelected();
                UIUtils.playClick();
                this.getContext().notifyInfo(UIKeys.GENERAL_CUT);
            }
        }).inside().category(category).active(canModify);
        this.keys().register(Keys.PASTE, () ->
        {
            UIContext context = this.getContext();

            if (this.copyPasteController.paste(context.mouseX, context.mouseY)) UIUtils.playClick();
        }).inside().category(category).active(canModify);
        this.keys().register(Keys.PRESETS, () ->
        {
            UIContext context = this.getContext();

            if (this.copyPasteController.canPreviewPresets())
            {
                this.copyPasteController.openPresets(context, context.mouseX, context.mouseY);
                UIUtils.playClick();
            }
        }).category(category).active(canModify);
        this.keys().register(Keys.DELETE, () -> this.currentGraph.removeSelected()).inside().category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SELECT_LEFT, () ->
        {
            UIContext context = this.getContext();

            this.selectAfter(context.mouseX, context.mouseY, -1);
        }).category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SELECT_RIGHT, () ->
        {
            UIContext context = this.getContext();

            this.selectAfter(context.mouseX, context.mouseY, 1);
        }).category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SELECT_SAME, this::selectSame).inside().category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SCALE_TIME, () -> this.scaleTime(TimelineToolbarSettings.SHORTCUTS_USE_INTERACTION_HINTS)).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_STACK_KEYFRAMES, () -> this.stackKeyframes(false, TimelineToolbarSettings.SHORTCUTS_USE_INTERACTION_HINTS)).inside().category(category).active(this::hasSelectedKeyframes);
        this.keys().register(Keys.KEYFRAMES_SELECT_PREV, () -> this.selectNextKeyframe(-1)).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_SELECT_NEXT, () -> this.selectNextKeyframe(1)).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_SPREAD, this::spreadKeyframes).category(category);
        this.keys().register(Keys.KEYFRAMES_ADJUST_VALUES, this::adjustValues).category(category);

        Supplier<Boolean> poseLimbActive = this::hasOpenPoseLimbTracks;

        this.keys().register(Keys.POSE_LIMB_KEYFRAME, () -> UITransformKeyframeFactory.keyframeOpenPoseLimbs(this, this.getPlayheadTick(), false)).inside().category(UIKeys.POSE_LIMB_KEYS_CATEGORY).active(poseLimbActive);
        this.keys().register(Keys.POSE_LIMB_KEYFRAME_DEFAULT, () -> UITransformKeyframeFactory.keyframeOpenPoseLimbs(this, this.getPlayheadTick(), true)).inside().category(UIKeys.POSE_LIMB_KEYS_CATEGORY).active(poseLimbActive);

        this.sidebarResizer = new UIDraggable((context) ->
        {
            int width = context.mouseX - this.area.x;

            this.dopeSheet.setSidebarWidth(width);
            this.updateSidebarResizerState();
            this.resize();
        })
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                if (this.area.isInside(context) && context.mouseButton == 0 && Window.isCtrlPressed())
                {
                    UIKeyframes.this.dopeSheet.setSidebarWidth(IUIKeyframeGraph.SIDEBAR_WIDTH);
                    UIKeyframes.this.updateSidebarResizerState();
                    UIKeyframes.this.resize();
                    UIKeyframes.this.persistSidebarWidth();

                    return true;
                }

                return super.subMouseClicked(context);
            }
        }.rendering((context) ->
        {
            float alpha = (this.sidebarResizer.isDragging() || this.sidebarResizer.area.isInside(context)) ? 0.75F : 0.5F;
            int color = Colors.setA(BBSSettings.primaryColor.get(), alpha);

            context.batcher.box(this.sidebarResizer.area.x, this.sidebarResizer.area.y, this.sidebarResizer.area.ex(), this.sidebarResizer.area.ey(), color);
        }).dragEnd(this::persistSidebarWidth);
        this.add(this.sidebarResizer);

        if (BBSSettings.uiLayoutPreferences != null)
        {
            this.dopeSheet.setSidebarWidth(BBSSettings.uiLayoutPreferences.getKeyframeSidebarWidth(IUIKeyframeGraph.SIDEBAR_WIDTH));
        }

        this.updateSidebarResizerState();
    }

    public UIKeyframes single()
    {
        this.single = true;

        return this;
    }

    public boolean isSingleSheet()
    {
        return this.single;
    }

    private void adjustValues()
    {
        this.getContext().replaceContextMenu((menu2) ->
        {
            menu2.autoKeys();
            menu2.action(Icons.ARROW_LEFT, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT, () -> this.adjustValues(false));
            menu2.action(Icons.ARROW_RIGHT, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT, () -> this.adjustValues(true));
        });
    }

    private void interpolationMenu()
    {
        this.getContext().replaceContextMenu((menu2) ->
        {
            menu2.autoKeys();
            menu2.action(Icons.INTERP_LINEAR, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_LINEAR, () -> this.setInterpolation(Interpolations.LINEAR));
            menu2.action(Icons.ARC, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_BEZIER, () -> this.setInterpolation(Interpolations.BEZIER));
            menu2.action(Icons.CURVES, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_HERMITE, () -> this.setInterpolation(Interpolations.HERMITE));
            menu2.action(Icons.INTERP_STEP, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_STEP, () -> this.setInterpolation(Interpolations.STEP));
        });
    }

    private void setInterpolation(IInterp interp)
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            sheet.channel.preNotify();

            for (Keyframe keyframe : selected)
            {
                keyframe.getInterpolation().setInterp(interp);
            }

            sheet.channel.postNotify();
        }
    }

    private void adjustValues(boolean last)
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();
            IKeyframeFactory factory = sheet.channel.getFactory();

            if (selected.size() < 2 || !KeyframeFactories.isNumeric(factory))
            {
                continue;
            }

            sheet.channel.preNotify();

            int index = last ? selected.size() - 1 : 0;
            int previous = last ? selected.size() - 2 : 1;

            Keyframe kf = selected.get(index);
            Keyframe prevKf = selected.get(previous);

            double difference = factory.getY(kf.getValue()) - factory.getY(prevKf.getValue());

            selected.remove(index);

            for (Keyframe keyframe : selected)
            {
                keyframe.setValue(sheet.clampValue(factory.yToValue(factory.getY(keyframe.getValue()) + difference)));
            }

            sheet.channel.postNotify();
        }
    }

    public UIKeyframes changed(Runnable runnable)
    {
        if (this.changeCallback == null)
        {
            this.changeCallback = runnable;
        }
        else
        {
            Runnable previous = this.changeCallback;

            this.changeCallback = () ->
            {
                previous.run();
                runnable.run();
            };
        }

        return this;
    }

    public void triggerChange()
    {
        if (this.changeCallback != null)
        {
            this.changeCallback.run();

        }
    }

    public UIKeyframeDopeSheet getDopeSheet()
    {
        return this.dopeSheet;
    }

    protected float getPlayheadTick()
    {
        return 0F;
    }

    protected boolean hasOpenPoseLimbTracks()
    {
        for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
        {
            if (!sheet.groupHeader && UITransformKeyframeFactory.isPoseLimbTrack(sheet))
            {
                return true;
            }
        }

        return false;
    }

    protected void selectNextKeyframe(int direction)
    {
        IUIKeyframeGraph graph = this.getGraph();
        Keyframe keyframe = graph.getSelected();

        if (keyframe == null)
        {
            UIContext context = this.getContext();

            this.selectNextKeyframeAt(context.mouseX, context.mouseY, direction);

            return;
        }

        KeyframeChannel channel = (KeyframeChannel) keyframe.getParent();
        int existingIndex = channel.getKeyframes().indexOf(keyframe);
        int index = MathUtils.cycler(existingIndex + direction, channel.getAll());
        Keyframe nextKeyframe = channel.get(index);

        graph.clearSelection();
        graph.selectKeyframe(nextKeyframe);
    }

    public void selectNextKeyframeAt(int mouseX, int mouseY, int direction)
    {
        IUIKeyframeGraph graph = this.getGraph();
        UIKeyframeSheet sheet = graph.getSheet(mouseY);

        if (sheet == null || sheet.groupHeader)
        {
            return;
        }

        KeyframeSegment segment = sheet.channel.find((float) this.fromGraphX(mouseX));

        if (segment == null)
        {
            return;
        }

        Keyframe keyframe = direction < 0 ? segment.a : segment.b;

        graph.clearSelection();
        graph.selectKeyframe(keyframe);
    }

    private void selectAfter(int mouseX, int mouseY, int direction)
    {
        float tick = (float) this.fromGraphX(mouseX);

        if (!Window.isShiftPressed())
        {
            this.currentGraph.selectAfter(tick, direction);
        }
        else
        {
            this.currentGraph.getSheet(mouseY).selection.after(tick, direction);
            this.currentGraph.pickSelected();
        }
    }

    private void selectSame()
    {
        UIContext context = this.getContext();
        Pair<Keyframe, KeyframeType> keyframe = this.currentGraph.findKeyframe(context.mouseX, context.mouseY);

        if (keyframe != null)
        {
            this.selectSameValue(keyframe.a, this.currentGraph.getSheet(context.mouseY), false);
        }
    }

    private void cancelScalingMode()
    {
        for (Map.Entry<Keyframe, Float> entry : this.scaleTicks.entrySet())
        {
            entry.getKey().setTick(entry.getValue(), true);
        }

        this.scaling = false;
        this.scalingShowInteractionHints = false;
    }

    private void confirmScalingMode()
    {
        this.scaling = false;
        this.scalingShowInteractionHints = false;
    }

    private void scaleTime(boolean showInteractionHints)
    {
        if (this.scaling)
        {
            this.scaling = false;
            this.scalingShowInteractionHints = false;

            return;
        }

        UIContext context = this.getContext();

        this.scaling = true;
        this.scalingShowInteractionHints = showInteractionHints;
        this.scaleTicks.clear();
        this.scalingAnchor = Integer.MAX_VALUE;
        this.originalX = context.mouseX;
        this.originalY = context.mouseY;

        for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
        {
            for (Keyframe keyframe : sheet.selection.getSelected())
            {
                this.scaleTicks.put(keyframe, keyframe.getTick());
                this.scalingAnchor = Math.min(this.scalingAnchor, keyframe.getTick());
            }
        }
    }

    private void stackKeyframes(boolean cancel, boolean showInteractionHints)
    {
        if (this.stacking)
        {
            this.stacking = false;
            this.stackingShowInteractionHints = false;

            if (!cancel)
            {
                UIContext context = this.getContext();
                List<UIKeyframeSheet> sheets = new ArrayList<>();
                float currentTick = (float) this.fromGraphX(context.mouseX);

                for (UIKeyframeSheet sheet : this.getGraph().getSheets())
                {
                    if (sheet.selection.hasAny())
                    {
                        sheets.add(sheet);
                    }
                }

                for (UIKeyframeSheet current : sheets)
                {
                    List<Keyframe> selected = current.selection.getSelected();
                    float mMin = Integer.MAX_VALUE;
                    float mMax = Integer.MIN_VALUE;

                    for (Keyframe keyframe : selected)
                    {
                        mMin = Math.min(keyframe.getTick(), mMin);
                        mMax = Math.max(keyframe.getTick(), mMax);
                    }

                    float length = mMax - mMin + this.getStackOffset();
                    int times = (int) Math.max(1, Math.ceil((currentTick - mMax) / length));
                    float x = 0;

                    current.selection.clear();

                    for (int i = 0; i < times; i++)
                    {
                        for (Keyframe keyframe : selected)
                        {
                            float tick = mMax + this.getStackOffset() + (keyframe.getTick() - mMin) + x;
                            int index = current.channel.insert(tick, keyframe.getFactory().copy(keyframe.getValue()));
                            Keyframe kf = current.channel.get(index);
                            
                            kf.copy(keyframe);
                            kf.setTick(tick);
                            current.selection.add(index);
                        }

                        x += length;
                    }
                }
            }

            return;
        }

        if (!this.hasSelectedKeyframes())
        {
            return;
        }

        this.stacking = true;
        this.stackingShowInteractionHints = showInteractionHints;
        this.stackOffset = 1;
    }

    private boolean isTransformInteractionBlockingContext()
    {
        return (this.scaling && this.scalingShowInteractionHints)
            || (this.stacking && this.stackingShowInteractionHints);
    }

    public boolean isStacking()
    {
        return this.stacking;
    }

    public float getStackOffset()
    {
        return this.stackOffset;
    }

    private void spreadKeyframes()
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (Keyframe keyframe : selected)
            {
                int index = sheet.channel.getKeyframes().indexOf(keyframe);

                min = Math.min(min, index);
                max = Math.max(max, index);
            }

            Keyframe minKf = sheet.channel.get(min);
            Keyframe maxKf = sheet.channel.get(max);
            int count = max - min;
            float distance = (maxKf.getTick() - minKf.getTick()) / count;

            sheet.channel.preNotify();

            for (int i = 1; i < count; i++)
            {
                int index = i + min;
                Keyframe kf = sheet.channel.get(index);

                kf.setTick(minKf.getTick() + i * distance);
            }

            sheet.channel.postNotify();

            sheet.selection.clear();

            for (int i = min; i <= max; i++)
            {
                sheet.selection.add(i);
            }
        }

        this.getGraph().pickSelected();
    }

    /* Sheet editing */

    public boolean isEditing()
    {
        return this.currentGraph != this.dopeSheet;
    }

    /**
     * Whether keyframe editing shortcuts / toolbar actions are allowed.
     * Blocked while scaling keyframes in time ({@code V} mode).
     */
    public boolean isModifyingKeyframes()
    {
        return !this.scaling;
    }

    public boolean isDraggingKeyframes()
    {
        return this.dragging > 0;
    }

    public boolean hasSelectedKeyframes()
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            if (!sheet.selection.getSelected().isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Whether selected keyframes span more than one track (Alt+click column-style
     * duplicate: horizontal placement only, same tracks).
     */
    public boolean hasMultiTrackKeyframeSelection()
    {
        int sheetsWithSelection = 0;

        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            if (!sheet.selection.getSelected().isEmpty())
            {
                sheetsWithSelection++;

                if (sheetsWithSelection > 1)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canSpreadSelectedKeyframes()
    {
        if (!this.isModifyingKeyframes())
        {
            return false;
        }

        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            if (sheet.selection.getSelected().size() >= 2)
            {
                return true;
            }
        }

        return false;
    }

    public boolean canAdjustSelectedValues()
    {
        if (!this.isModifyingKeyframes())
        {
            return false;
        }

        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();

            if (selected.size() >= 2 && KeyframeFactories.isNumeric(sheet.channel.getFactory()))
            {
                return true;
            }
        }

        return false;
    }

    public void toolbarCopy()
    {
        if (this.copyPasteController.copy())
        {
            UIUtils.playClick();
        }
    }

    public void toolbarCut()
    {
        if (this.currentGraph.getSelected() == null)
        {
            this.getContext().notifyError(UIKeys.GENERAL_CUT_EMPTY);

            return;
        }

        if (this.copyPasteController.copy())
        {
            this.currentGraph.removeSelected();
            UIUtils.playClick();
            this.getContext().notifyInfo(UIKeys.GENERAL_CUT);
        }
    }

    public void toolbarRemoveSelected()
    {
        this.currentGraph.removeSelected();
    }

    public void toolbarRoundSelectedTicks()
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            sheet.channel.preNotify();

            for (Keyframe kf : selected)
            {
                kf.setTick(Math.round(kf.getTick()), false);
            }

            sheet.channel.postNotify();
        }
    }

    public void toolbarSpreadKeyframes()
    {
        this.spreadKeyframes();
    }

    public void toolbarApplyInterpolation(IInterp interp)
    {
        this.setInterpolation(interp);
    }

    public void toolbarAdjustValues(boolean last)
    {
        this.adjustValues(last);
    }

    public void toolbarShowAdjustValuesMenu()
    {
        this.adjustValues();
    }

    public void toolbarSelectLeft()
    {
        this.selectRelativeToTimelineCursor(-1);
    }

    public void toolbarSelectRight()
    {
        this.selectRelativeToTimelineCursor(1);
    }

    /**
     * Select keyframes left/right of the film cursor tick (not the mouse
     * position), matching timeline behaviour when invoked from the toolbar.
     */
    private void selectRelativeToTimelineCursor(int direction)
    {
        int graphX;
        int mouseY;

        if (this instanceof UIFilmKeyframes filmKeyframes)
        {
            graphX = this.toGraphX(filmKeyframes.getOffset());
            mouseY = this.area.my();
        }
        else
        {
            UIContext context = this.getContext();

            graphX = context.mouseX;
            mouseY = context.mouseY;
        }

        this.selectAfter(graphX, mouseY, direction);
    }

    public void toolbarSelectSame()
    {
        this.enterSelectSameInteraction(false);
    }

    public void toolbarSelectSameCurrentTrack()
    {
        this.enterSelectSameInteraction(true);
    }

    public void enterSelectSameInteraction(boolean currentTrackOnly)
    {
        if (!this.isModifyingKeyframes())
        {
            return;
        }

        IKey hint = currentTrackOnly
            ? UIKeys.TIMELINE_INTERACTION_SELECT_SAME_TRACK
            : UIKeys.TIMELINE_INTERACTION_SELECT_SAME_ALL;

        this.enterSelectSameInteraction(new KeyframeSelectSameInteractionState(hint, currentTrackOnly));
    }

    public void enterSelectSameInteraction(KeyframeSelectSameInteractionState state)
    {
        if (state == null || !this.isModifyingKeyframes())
        {
            return;
        }

        this.insertInteraction.cancel();
        this.duplicateInteraction.cancel();
        this.pasteInteraction.cancel();
        this.selectNeighborInteraction.cancel();
        this.interactionOverlay.cancel();
        this.selectSameInteraction.enter(state);
    }

    public boolean isSelectSameInteractionActive()
    {
        return this.selectSameInteraction.isActive();
    }

    public void selectSameValue(Keyframe target, UIKeyframeSheet pickedSheet, boolean currentTrackOnly)
    {
        if (target == null)
        {
            return;
        }

        if (!Window.isShiftPressed())
        {
            this.currentGraph.clearSelection();
        }

        for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
        {
            if (currentTrackOnly && sheet != pickedSheet)
            {
                continue;
            }

            List<Keyframe> list = sheet.channel.getList();

            for (int i = 0; i < list.size(); i++)
            {
                Keyframe kf = list.get(i);

                if (kf.getFactory().compare(target.getValue(), kf.getValue()))
                {
                    sheet.selection.add(i);
                }
            }
        }

        this.currentGraph.pickSelected();
    }

    public void toolbarSelectPrevKeyframe()
    {
        if (this.getGraph().getSelected() != null)
        {
            this.selectNextKeyframe(-1);
        }
        else
        {
            this.enterSelectNeighborInteraction(-1);
        }
    }

    public void toolbarSelectNextKeyframe()
    {
        if (this.getGraph().getSelected() != null)
        {
            this.selectNextKeyframe(1);
        }
        else
        {
            this.enterSelectNeighborInteraction(1);
        }
    }

    public void enterSelectNeighborInteraction(int direction)
    {
        if (!this.isModifyingKeyframes())
        {
            return;
        }

        IKey hint = direction < 0
            ? UIKeys.TIMELINE_INTERACTION_SELECT_PREV_KEYFRAME
            : UIKeys.TIMELINE_INTERACTION_SELECT_NEXT_KEYFRAME;

        this.enterSelectNeighborInteraction(new KeyframeSelectNeighborInteractionState(hint, direction));
    }

    public void enterSelectNeighborInteraction(KeyframeSelectNeighborInteractionState state)
    {
        if (state == null || !this.isModifyingKeyframes())
        {
            return;
        }

        this.insertInteraction.cancel();
        this.duplicateInteraction.cancel();
        this.pasteInteraction.cancel();
        this.selectSameInteraction.cancel();
        this.interactionOverlay.cancel();
        this.selectNeighborInteraction.enter(state);
    }

    public boolean isSelectNeighborInteractionActive()
    {
        return this.selectNeighborInteraction.isActive();
    }

    public UIKeyframeSheet getSelectNeighborHoverSheet()
    {
        return this.selectNeighborInteraction.getHoverSheet();
    }

    public boolean canToolbarPaste()
    {
        return this.copyPasteController.canPaste();
    }

    public void toolbarPaste()
    {
        if (this.copyPasteController.paste(this.getToolbarPasteGraphX(), this.getToolbarPasteMouseY()))
        {
            UIUtils.playClick();
        }
    }

    public void toolbarPasteAtCursor()
    {
        this.toolbarEnterPasteAtCursor();
    }

    public void toolbarEnterPasteAtCursor()
    {
        this.enterKeyframePaste(KeyframePasteInteractionState.atCursor(
            UIKeys.TIMELINE_INTERACTION_PASTE_CURSOR));
    }

    public void toolbarPasteAtTimeline()
    {
        this.toolbarEnterPasteAtTimeline();
    }

    public void toolbarEnterPasteAtTimeline()
    {
        if (!this.isModifyingKeyframes() || !this.canToolbarPaste())
        {
            return;
        }

        int tick = this instanceof UIFilmKeyframes filmKeyframes ? filmKeyframes.getOffset() : 0;
        Map<String, PastedKeyframes> clipboard = this.getClipboardKeyframes();

        if (clipboard.size() > 1)
        {
            this.pasteClipboardAt(tick, this.getToolbarPasteMouseY());

            return;
        }

        this.enterKeyframePaste(KeyframePasteInteractionState.atPlayhead(
            UIKeys.TIMELINE_INTERACTION_PASTE_TIMELINE, tick));
    }

    public void toolbarOpenPresets()
    {
        UIContext context = this.getContext();
        int anchorX = this.getToolbarPasteGraphX();
        int anchorY = this.getToolbarPasteMouseY();

        if (this.copyPasteController.canPreviewPresets())
        {
            this.copyPasteController.openPresets(context, anchorX, anchorY);
            UIUtils.playClick();
        }
    }

    public void toolbarInsertIndividual(UIKeyframeSheet sheet, float tick)
    {
        if (!this.isModifyingKeyframes() || sheet == null || sheet.groupHeader)
        {
            return;
        }

        int index = sheet.channel.insertInterpolated(tick);
        Keyframe keyframe = sheet.channel.get(index);

        if (keyframe != null)
        {
            this.currentGraph.selectKeyframe(keyframe);
        }
    }

    public void enterKeyframeInsert(KeyframeInsertInteractionState state)
    {
        this.duplicateInteraction.cancel();
        this.pasteInteraction.cancel();
        this.selectNeighborInteraction.cancel();
        this.selectSameInteraction.cancel();
        this.interactionOverlay.cancel();
        this.insertInteraction.enter(state);
    }

    public void cancelKeyframeInsert()
    {
        this.insertInteraction.cancel();
    }

    @Override
    public UIContextMenu createContextMenu(UIContext context)
    {
        if (this.interactionOverlay.isActive() || this.insertInteraction.isActive()
            || this.duplicateInteraction.isActive() || this.pasteInteraction.isActive()
            || this.selectNeighborInteraction.isActive() || this.selectSameInteraction.isActive()
            || this.isTransformInteractionBlockingContext())
        {
            return null;
        }

        return super.createContextMenu(context);
    }

    public boolean isKeyframeInsertActive()
    {
        return this.insertInteraction.isActive();
    }

    public void renderKeyframeInsertPreviews(UIContext context)
    {
        this.insertInteraction.renderPreviews(this, context);
    }

    public boolean isKeyframeDuplicateActive()
    {
        return this.duplicateInteraction.isActive();
    }

    public void enterKeyframeDuplicate(KeyframeDuplicateInteractionState state)
    {
        if (!this.isModifyingKeyframes())
        {
            return;
        }

        if (!this.hasSelectedKeyframes())
        {
            this.getContext().notifyError(UIKeys.GENERAL_CUT_EMPTY);

            return;
        }

        this.insertInteraction.cancel();
        this.interactionOverlay.cancel();
        this.pasteInteraction.cancel();
        this.selectNeighborInteraction.cancel();
        this.selectSameInteraction.cancel();
        this.duplicateInteraction.enter(state);
    }

    public void cancelKeyframeDuplicate()
    {
        this.duplicateInteraction.cancel();
    }

    public void renderKeyframeDuplicatePreviews(UIContext context)
    {
        this.duplicateInteraction.renderPreviews(this, context);
    }

    public boolean isKeyframePasteActive()
    {
        return this.pasteInteraction.isActive();
    }

    public void enterKeyframePaste(KeyframePasteInteractionState state)
    {
        if (!this.isModifyingKeyframes() || !this.canToolbarPaste())
        {
            return;
        }

        this.insertInteraction.cancel();
        this.duplicateInteraction.cancel();
        this.interactionOverlay.cancel();
        this.selectNeighborInteraction.cancel();
        this.selectSameInteraction.cancel();
        this.pasteInteraction.enter(state);
    }

    public void cancelKeyframePaste()
    {
        this.pasteInteraction.cancel();
    }

    public void renderKeyframePastePreviews(UIContext context)
    {
        this.pasteInteraction.renderPreviews(this, context);
    }

    public void renderKeyframeSelectSamePreview(UIContext context)
    {
        this.selectSameInteraction.renderPreview(this, context);
    }

    public Map<String, PastedKeyframes> getClipboardKeyframes()
    {
        if (!this.canToolbarPaste())
        {
            return Collections.emptyMap();
        }

        return parseKeyframes(Window.getClipboardMap(this.copyPasteController.copyPrefix));
    }

    public void pasteClipboardAt(float tick, int mouseY)
    {
        Map<String, PastedKeyframes> data = this.getClipboardKeyframes();

        if (!data.isEmpty())
        {
            this.pasteKeyframes(data, tick, mouseY);
        }
    }

    public void duplicateSelectedKeyframes(float tick, int mouseY)
    {
        if (!this.hasSelectedKeyframes())
        {
            return;
        }

        this.pasteKeyframes(this.parseKeyframes(this.serializeKeyframes()), tick, mouseY);
    }

    public void toolbarEnterDuplicateAtCursor()
    {
        this.enterKeyframeDuplicate(KeyframeDuplicateInteractionState.atCursor(
            UIKeys.TIMELINE_INTERACTION_DUPLICATE_CURSOR));
    }

    public void toolbarEnterDuplicateAtPlayhead()
    {
        if (!this.isModifyingKeyframes())
        {
            return;
        }

        if (!this.hasSelectedKeyframes())
        {
            this.getContext().notifyError(UIKeys.GENERAL_CUT_EMPTY);

            return;
        }

        int tick = this instanceof UIFilmKeyframes filmKeyframes ? filmKeyframes.getOffset() : 0;

        if (this.hasMultiTrackKeyframeSelection())
        {
            this.duplicateSelectedKeyframes(tick, this.getToolbarPasteMouseY());

            return;
        }

        this.enterKeyframeDuplicate(KeyframeDuplicateInteractionState.atPlayhead(
            UIKeys.TIMELINE_INTERACTION_DUPLICATE_TIMELINE, tick));
    }

    public void toolbarDuplicateAtCursor()
    {
        this.toolbarEnterDuplicateAtCursor();
    }

    public void toolbarSelectColumn()
    {
        if (!this.isModifyingKeyframes())
        {
            return;
        }

        this.currentGraph.selectByX(this.getToolbarPasteGraphX());
    }

    public void toolbarToggleInterpolation()
    {
        if (!this.isModifyingKeyframes() || this.currentGraph.getSelected() == null)
        {
            return;
        }

        UIElement parent = this.getParent();

        while (parent != null)
        {
            if (parent instanceof UIKeyframeEditor keyframeEditor && keyframeEditor.editor != null)
            {
                keyframeEditor.editor.interp.clickItself();

                return;
            }

            parent = parent.getParent();
        }

        this.interpolationMenu();
    }

    public void toolbarScaleTime()
    {
        this.cancelTrackInteraction();
        this.scaleTime(true);
    }

    public void toolbarStackKeyframes()
    {
        if (!this.hasSelectedKeyframes())
        {
            return;
        }

        this.cancelTrackInteraction();
        this.stackKeyframes(false, true);
    }

    public void toolbarEditTrack()
    {
        this.enterTrackInteraction(
            UIKeys.TIMELINE_INTERACTION_PICK_TRACK,
            TimelineTrackEligibility::canPickEditTrack,
            this::confirmEditTrack);
    }

    public void confirmEditTrack(UIKeyframeSheet sheet)
    {
        this.editSheet(sheet);
    }

    public boolean canToolbarEditTrack()
    {
        return this.isModifyingKeyframes() && TimelineTrackEligibility.hasEditableTrack(this);
    }

    public void enterTrackInteraction(IKey hint, Predicate<UIKeyframeSheet> eligible,
        Consumer<UIKeyframeSheet> onConfirm)
    {
        this.insertInteraction.cancel();
        this.duplicateInteraction.cancel();
        this.pasteInteraction.cancel();
        this.selectNeighborInteraction.cancel();
        this.selectSameInteraction.cancel();
        this.interactionOverlay.enter(new TimelineInteractionState(hint, eligible, onConfirm));
    }

    public void cancelTrackInteraction()
    {
        this.interactionOverlay.cancel();
        this.insertInteraction.cancel();
        this.duplicateInteraction.cancel();
        this.pasteInteraction.cancel();
        this.selectNeighborInteraction.cancel();
        this.selectSameInteraction.cancel();

        if (this.scaling)
        {
            this.cancelScalingMode();
        }

        if (this.stacking)
        {
            this.stackKeyframes(true, false);
        }
    }

    /**
     * @return {@code true} when timeline interaction modes should consume clicks
     * instead of normal keyframe editing
     */
    private boolean isInteractionModeConsumingClicks()
    {
        return this.interactionOverlay.isActive()
            || this.insertInteraction.isActive()
            || this.duplicateInteraction.isActive()
            || this.pasteInteraction.isActive()
            || this.selectNeighborInteraction.isActive()
            || this.selectSameInteraction.isActive()
            || (this.scaling && this.scalingShowInteractionHints)
            || (this.stacking && this.stackingShowInteractionHints);
    }

    public TimelineInteractionState getTrackInteractionState()
    {
        return this.interactionOverlay.isActive() ? this.interactionOverlay.getState() : null;
    }

    public KeyframeInsertInteractionState getInsertInteractionState()
    {
        return this.insertInteraction.isActive() ? this.insertInteraction.getState() : null;
    }

    public KeyframeDuplicateInteractionState getDuplicateInteractionState()
    {
        return this.duplicateInteraction.isActive() ? this.duplicateInteraction.getState() : null;
    }

    public KeyframePasteInteractionState getPasteInteractionState()
    {
        return this.pasteInteraction.isActive() ? this.pasteInteraction.getState() : null;
    }

    public KeyframeSelectNeighborInteractionState getSelectNeighborInteractionState()
    {
        return this.selectNeighborInteraction.isActive() ? this.selectNeighborInteraction.getState() : null;
    }

    public KeyframeSelectSameInteractionState getSelectSameInteractionState()
    {
        return this.selectSameInteraction.isActive() ? this.selectSameInteraction.getState() : null;
    }

    public boolean isScalingWithInteractionHints()
    {
        return this.scaling && this.scalingShowInteractionHints;
    }

    public boolean isStackingWithInteractionHints()
    {
        return this.stacking && this.stackingShowInteractionHints;
    }

    public void restoreScalingInteractionHints()
    {
        if (!this.scaling)
        {
            this.scaleTime(true);
        }
    }

    public void restoreStackingInteractionHints()
    {
        if (!this.stacking)
        {
            this.stackKeyframes(false, true);
        }
    }

    private static final int DOPE_SHEET_RULER_HEIGHT = 16;

    public void renderInteractionTickPulse(UIContext context)
    {
        float tick = this.getInteractionPreviewTick();

        if (tick < 0F)
        {
            return;
        }

        int x = this.toGraphX(tick);

        TimelineInteractionHints.renderPulsingTickColumn(context, x, this.area.y + DOPE_SHEET_RULER_HEIGHT, this.area.ey());
    }

    private float getInteractionPreviewTick()
    {
        if (this.duplicateInteraction.isActive())
        {
            return this.duplicateInteraction.getAnchorTick();
        }

        if (this.pasteInteraction.isActive())
        {
            return this.pasteInteraction.getAnchorTick();
        }

        if (this.selectNeighborInteraction.isActive())
        {
            return this.selectNeighborInteraction.getPreviewTick(this, this.getContext());
        }

        return -1F;
    }

    public void renderTransformModeHints(UIContext context)
    {
        if (this.scaling && this.scalingShowInteractionHints)
        {
            TimelineInteractionHints.renderHint(context, this.area, UIKeys.TIMELINE_INTERACTION_SCALE_TIME, this);
        }
        else if (this.stacking && this.stackingShowInteractionHints)
        {
            TimelineInteractionHints.renderHint(context, this.area, UIKeys.TIMELINE_INTERACTION_STACK_KEYFRAMES, this);
        }
    }

    public boolean isTrackInteractionActive()
    {
        return this.interactionOverlay.isActive();
    }

    public boolean isTrackInteractionEligible(UIKeyframeSheet sheet)
    {
        return this.interactionOverlay.isSheetEligible(sheet);
    }

    public boolean anyTrackMatches(Predicate<UIKeyframeSheet> predicate)
    {
        if (!(this.currentGraph instanceof UIKeyframeDopeSheet))
        {
            return false;
        }

        for (UIKeyframeSheet sheet : this.dopeSheet.getSheets())
        {
            if (predicate.test(sheet))
            {
                return true;
            }
        }

        return false;
    }

    private int getToolbarPasteGraphX()
    {
        if (this instanceof UIFilmKeyframes filmKeyframes)
        {
            return this.toGraphX(filmKeyframes.getOffset());
        }

        return this.toGraphX(0);
    }

    private int getToolbarPasteMouseY()
    {
        return this.area.my() + this.area.h / 2;
    }

    public void editSheet(UIKeyframeSheet sheet)
    {
        if (sheet == null)
        {
            this.currentGraph = this.dopeSheet;
        }
        else
        {
            this.dopeSheet.clearSelection();
            this.dopeSheet.pickSelected();

            this.currentGraph = new UIKeyframeGraph(this, sheet);

            this.resetView();
        }
    }

    /* Caching keyframes */

    public void cacheKeyframes()
    {
        this.cache = new SheetCache(this.currentGraph.getSheets());
    }

    public void submitKeyframes()
    {
        if (this.cache == null)
        {
            return;
        }

        /* Cache selection indices */
        Map<UIKeyframeSheet, Pair<List<Integer>, List<Integer>>> selection = new HashMap<>();

        for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
        {
            List<Integer> last = sheet.sort();

            selection.put(sheet, new Pair<>(last, new ArrayList<>(sheet.selection.getIndices())));
        }

        /* Apply the data in order and submit to pre-/post-handlers */
        SheetCache cache = new SheetCache(this.currentGraph.getSheets());

        for (Pair<KeyframeChannel, UIKeyframeSheet> pair : this.cache.data)
        {
            pair.b.channel.copyKeyframes(pair.a);
            pair.b.selection.clear();

            if (selection.containsKey(pair.b))
            {
                pair.b.selection.addAll(selection.get(pair.b).a);
            }

            pair.b.channel.preNotify(IValueListener.FLAG_UNMERGEABLE);
        }

        for (Pair<KeyframeChannel, UIKeyframeSheet> pair : cache.data)
        {
            pair.b.channel.copyKeyframes(pair.a);
            pair.b.selection.clear();

            if (selection.containsKey(pair.b))
            {
                pair.b.selection.addAll(selection.get(pair.b).b);
            }

            pair.b.channel.postNotify(IValueListener.FLAG_UNMERGEABLE);
        }

        this.cache = null;
    }

    /* Copy-pasting */

    /**
     * Parses keyframe data from the given map structure into a map of keyframes corresponding to their keys.
     *
     * @param data the map data containing keyframe information. Each key in the map represents
     *             a collection of keyframe data, where the associated value contains type and keyframe details.
     *             If null, the method returns an empty map.
     * @return a map where each key corresponds to a collection of parsed {@link PastedKeyframes}.
     *         If no keyframe data is provided or the input is null, an empty map is returned.
     */
    public static Map<String, PastedKeyframes> parseKeyframes(MapType data)
    {
        if (data == null)
        {
            return Collections.emptyMap();
        }

        Map<String, PastedKeyframes> temp = new HashMap<>();

        for (String key : data.keys())
        {
            MapType map = data.getMap(key);
            ListType list = map.getList("keyframes");
            IKeyframeFactory serializer = KeyframeFactories.FACTORIES.get(map.getString("type"));

            for (int i = 0, c = list.size(); i < c; i++)
            {
                PastedKeyframes pastedKeyframes = temp.computeIfAbsent(key, k -> new PastedKeyframes(serializer));
                Keyframe keyframe = new Keyframe("", serializer);

                keyframe.fromData(list.getMap(i));
                pastedKeyframes.keyframes.add(keyframe);
            }
        }

        return temp;
    }

    private MapType serializeKeyframes()
    {
        return this.serializeKeyframesByFactories();
    }

    public MapType serializeKeyframesByFactories(IKeyframeFactory... allowedFactories)
    {
        Set<IKeyframeFactory> allow = new HashSet<>();

        if (allowedFactories != null && allowedFactories.length > 0)
        {
            allow.addAll(Arrays.asList(allowedFactories));
        }

        MapType keyframes = new MapType();

        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            List<Keyframe> selected = property.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            if (!allow.isEmpty() && !allow.contains(property.channel.getFactory()))
            {
                continue;
            }

            MapType data = new MapType();
            ListType list = new ListType();

            data.putString("type", CollectionUtils.getKey(KeyframeFactories.FACTORIES, property.channel.getFactory()));
            data.put("keyframes", list);

            for (Keyframe keyframe : selected)
            {
                list.add(keyframe.toData());
            }

            if (!list.isEmpty())
            {
                keyframes.put(property.id, data);
            }
        }

        return keyframes;
    }

    public MapType serializeKeyframesByPropertySuffixes(String... suffixes)
    {
        Set<String> allow = new HashSet<>();

        if (suffixes != null && suffixes.length > 0)
        {
            allow.addAll(Arrays.asList(suffixes));
        }

        MapType keyframes = new MapType();

        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            List<Keyframe> selected = property.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            if (!allow.isEmpty() && !matchesPropertySuffix(property.id, allow))
            {
                continue;
            }

            MapType data = new MapType();
            ListType list = new ListType();

            data.putString("type", CollectionUtils.getKey(KeyframeFactories.FACTORIES, property.channel.getFactory()));
            data.put("keyframes", list);

            for (Keyframe keyframe : selected)
            {
                list.add(keyframe.toData());
            }

            if (!list.isEmpty())
            {
                keyframes.put(property.id, data);
            }
        }

        return keyframes;
    }

    private boolean matchesPropertySuffix(String property, Set<String> allow)
    {
        for (String suffix : allow)
        {
            if (property.equals(suffix) || property.endsWith("/" + suffix))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Paste copied keyframes to clipboard
     */
    protected void pasteKeyframes(Map<String, PastedKeyframes> keyframes, float offset, int mouseY)
    {
        List<UIKeyframeSheet> sheets = this.currentGraph.getSheets();

        this.currentGraph.clearSelection();

        if (keyframes.size() == 1)
        {
            UIKeyframeSheet current = this.currentGraph.getSheet(mouseY);

            if (current == null)
            {
                current =  sheets.get(0);
            }

            this.pasteKeyframesTo(current, keyframes.get(keyframes.keySet().iterator().next()), offset);
        }
        else
        {
            float min = Float.MAX_VALUE;

            for (Map.Entry<String, PastedKeyframes> entry : keyframes.entrySet())
            {
                if (entry.getValue().keyframes.isEmpty())
                {
                    continue;
                }

                entry.getValue().keyframes.sort((a, b) -> Float.compare(a.getTick(), b.getTick()));

                min = Math.min(min, entry.getValue().keyframes.get(0).getTick());
            }

            for (Map.Entry<String, PastedKeyframes> entry : keyframes.entrySet())
            {
                float entryMin = entry.getValue().keyframes.get(0).getTick();

                for (UIKeyframeSheet property : sheets)
                {
                    if (!property.id.equals(entry.getKey()))
                    {
                        continue;
                    }

                    float d = min == Float.MAX_VALUE ? 0F : entryMin - min;

                    this.pasteKeyframesTo(property, entry.getValue(), offset + d);
                }
            }
        }

        this.currentGraph.pickSelected();
    }

    private void pasteKeyframesTo(UIKeyframeSheet sheet, PastedKeyframes pastedKeyframes, float offset)
    {
        if (sheet.channel.getFactory() != pastedKeyframes.factory)
        {
            return;
        }

        float firstX = pastedKeyframes.keyframes.get(0).getTick();
        List<Keyframe> toSelect = new ArrayList<>();

        for (Keyframe keyframe : pastedKeyframes.keyframes)
        {
            keyframe.setTick(keyframe.getTick() - firstX + offset);

            int index = sheet.channel.insert(keyframe.getTick(), keyframe.getValue());
            Keyframe inserted = sheet.channel.get(index);

            inserted.copy(keyframe);
            toSelect.add(inserted);
        }

        for (Keyframe select : toSelect)
        {
            sheet.selection.add(sheet.channel.getKeyframes().indexOf(select));
        }
    }

    /* Getters & setters */

    public UIKeyframes backgroundRenderer(Consumer<UIContext> backgroundRender)
    {
        this.backgroundRender = backgroundRender;

        return this;
    }

    public UIKeyframes duration(Supplier<Integer> duration)
    {
        this.duration = duration;

        return this;
    }

    public IUIKeyframeGraph getGraph()
    {
        return this.currentGraph;
    }

    public UIKeyframes setPresetsPreview(UICopyPasteController.IPresetPreview preview)
    {
        this.copyPasteController.preview(preview);

        return this;
    }

    public Scale getXAxis()
    {
        return this.xAxis;
    }

    public int getDuration()
    {
        return this.duration == null ? 0 : this.duration.get();
    }

    public boolean isSelecting()
    {
        return this.selecting;
    }

    public boolean isNavigating()
    {
        return this.navigating;
    }

    /* Sheet management */

    public void removeAllSheets()
    {
        this.dopeSheet.removeAllSheets();
    }

    public void addSheet(UIKeyframeSheet sheet)
    {
        this.dopeSheet.addSheet(sheet);
    }

    public void pickKeyframe(Keyframe keyframe)
    {
        this.getGraph().onCallback(keyframe);

        if (this.callback != null)
        {
            this.callback.accept(keyframe);
        }
    }

    /* Graphing */

    public int toGraphX(double tick)
    {
        return (int) this.xAxis.to(tick);
    }

    public double fromGraphX(int mouseX)
    {
        return this.xAxis.from(mouseX);
    }

    public void resetView()
    {
        this.currentGraph.resetView();
    }

    public void resetViewX()
    {
        int c = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        /* Find minimum and maximum */
        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            List keyframes = property.channel.getKeyframes();

            for (Object object : keyframes)
            {
                Keyframe frame = (Keyframe) object;

                min = Integer.min((int) frame.getTick(), min);
                max = Integer.max((int) frame.getTick(), max);
            }

            c = Math.max(c, keyframes.size());
        }

        if (c <= 1)
        {
            min = 0;
            max = this.getDuration();
        }

        int sidebar = this.currentGraph instanceof UIKeyframeDopeSheet ? this.currentGraph.getSidebarWidth() : 0;

        if (Math.abs(max - min) > 0.01F)
        {
            this.xAxis.viewOffset(min, max, this.area.w - sidebar, 30);
            this.xAxis.setShift(this.xAxis.getShift() - sidebar / this.xAxis.getZoom());
        }
        else
        {
            this.xAxis.set(0, 2);
            this.xAxis.setShift(this.xAxis.getShift() - sidebar / this.xAxis.getZoom());
        }
    }

    public Area getGrabbingArea(UIContext context)
    {
        Area area = new Area();

        area.setPoints(this.originalX, this.originalY, context.mouseX, context.mouseY, 3);

        return area;
    }

    /* User input */

    @Override
    public void resize()
    {
        /* Save horizontal view range, and restore it after resize */
        double minValue = this.xAxis.getMinValue();
        double maxValue = this.xAxis.getMaxValue();

        super.resize();

        this.currentGraph.resize();
        this.updateSidebarResizerState();

        if (!Operation.equals(minValue, maxValue))
        {
            this.xAxis.view(minValue, maxValue);
        }
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.isInteractionModeConsumingClicks()
            && this.currentGraph instanceof UIKeyframeDopeSheet dopeSheet
            && dopeSheet.tryHandleSidebarToggleClick(context))
        {
            return true;
        }

        if (this.selectSameInteraction.handleMouseClicked(this, context))
        {
            return true;
        }

        if (this.selectNeighborInteraction.handleMouseClicked(this, context))
        {
            return true;
        }

        if (this.pasteInteraction.handleMouseClicked(this, context))
        {
            return true;
        }

        if (this.duplicateInteraction.handleMouseClicked(this, context))
        {
            return true;
        }

        if (this.insertInteraction.handleMouseClicked(this, context))
        {
            return true;
        }

        if (this.interactionOverlay.handleMouseClicked(this, context))
        {
            return true;
        }

        if (this.currentGraph.mouseClicked(context))
        {
            return true;
        }

        if (this.scaling)
        {
            if (context.mouseButton == 1 && this.scalingShowInteractionHints)
            {
                this.cancelScalingMode();
            }
            else if (context.mouseButton != 1 || !this.scalingShowInteractionHints)
            {
                this.confirmScalingMode();
            }

            return true;
        }

        if (this.stacking)
        {
            this.stackKeyframes(context.mouseButton == 1, this.stackingShowInteractionHints);

            return true;
        }

        if (this.area.isInside(context) && !TimelineToolbarPointerBlock.blocksPointer(context))
        {
            this.lastX = this.originalX = context.mouseX;
            this.lastY = this.originalY = context.mouseY;

            if (Window.isCtrlPressed() && context.mouseButton == 0)
            {
                this.removeOrCreateKeyframe(context);
            }
            else if (Window.isAltPressed() && context.mouseButton == 0)
            {
                this.duplicateOrSelectColumn(context);
            }
            else if (context.mouseButton == 0)
            {
                this.pickOrStartSelectingKeyframes(context);
            }
            else if (context.mouseButton == 2)
            {
                this.navigating = true;
            }

            return context.mouseButton != 1;
        }

        return super.subMouseClicked(context);
    }

    private void removeOrCreateKeyframe(UIContext context)
    {
        Pair<Keyframe, KeyframeType> keyframe = this.currentGraph.findKeyframe(context.mouseX, context.mouseY);

        if (keyframe != null)
        {
            this.currentGraph.removeKeyframe(keyframe.a);
        }
        else
        {
            this.currentGraph.addKeyframe(context.mouseX, context.mouseY);
        }
    }

    private void duplicateOrSelectColumn(UIContext context)
    {
        if (this.currentGraph.getSelected() != null && !Window.isShiftPressed())
        {
            /* Duplicate */
            int tick = (int) Math.round(this.fromGraphX(context.mouseX));

            this.pasteKeyframes(this.parseKeyframes(this.serializeKeyframes()), tick, context.mouseY);

            return;
        }

        /* Select a column */
        this.currentGraph.selectByX(context.mouseX);
    }

    private void pickOrStartSelectingKeyframes(UIContext context)
    {
        /* Picking keyframe or initiating selection */
        Pair<Keyframe, KeyframeType> pair = this.currentGraph.findKeyframe(context.mouseX, context.mouseY);
        Keyframe found = pair == null ? null : pair.a;
        boolean shift = Window.isShiftPressed();

        if (shift && found == null)
        {
            this.selecting = true;
        }

        if (found != null)
        {
            UIKeyframeSheet sheet = this.currentGraph.getSheet(found);

            if (!shift && !sheet.selection.has(found))
            {
                this.currentGraph.clearSelection();
            }

            sheet.selection.add(found);

            found = this.currentGraph.getSelected();

            this.pickKeyframe(found);
        }
        else if (!this.selecting)
        {
            this.currentGraph.clearSelection();
            this.pickKeyframe(null);
        }

        if (!this.selecting)
        {
            this.dragging = 0;
            this.draggingData = pair;

            if (pair != null && pair.b != KeyframeType.REGULAR)
            {
                found = pair.a;
            }

            this.cacheKeyframes();

            if (found != null)
            {
                this.originalT = found.getTick();
                this.originalV = found.getFactory().copy(found.getValue());
            }
        }
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        this.currentGraph.mouseReleased(context);

        if (this.selecting)
        {
            this.currentGraph.selectInArea(this.getGrabbingArea(context));
        }

        if (this.dragging > 0)
        {
            this.submitKeyframes();
            this.currentGraph.pickSelected();
        }

        this.navigating = false;
        this.selecting = false;
        this.dragging = -1;

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        if ((this.insertInteraction.isActive() || this.interactionOverlay.isActive()
            || this.duplicateInteraction.isActive() || this.pasteInteraction.isActive()
            || this.selectNeighborInteraction.isActive() || this.selectSameInteraction.isActive()) && this.area.isInside(context))
        {
            this.currentGraph.mouseScrolled(context);

            return true;
        }

        if (this.area.isInside(context) && this.stacking)
        {
            this.stackOffset = (float) Math.max(0.05F, this.stackOffset + Math.copySign(Window.isShiftPressed() ? 0.05F : 1, context.mouseWheel));

            return true;
        }

        if (this.area.isInside(context) && !this.navigating && !this.scaling
            && !TimelineToolbarPointerBlock.blocksPointer(context))
        {
            this.currentGraph.mouseScrolled(context);

            return true;
        }

        return super.subMouseScrolled(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.selectSameInteraction.handleKeyPressed(context))
        {
            return true;
        }

        if (this.selectNeighborInteraction.handleKeyPressed(context))
        {
            return true;
        }

        if (this.pasteInteraction.handleKeyPressed(context))
        {
            return true;
        }

        if (this.duplicateInteraction.handleKeyPressed(context))
        {
            return true;
        }

        if (this.insertInteraction.handleKeyPressed(context))
        {
            return true;
        }

        if (this.interactionOverlay.handleKeyPressed(context))
        {
            return true;
        }

        if (this.currentGraph != this.dopeSheet && context.isPressed(GLFW.GLFW_KEY_ESCAPE) && !this.single)
        {
            this.editSheet(null);

            return true;
        }

        if ((this.scaling || this.stacking) && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            if (this.scaling)
            {
                this.cancelScalingMode();
            }

            if (this.stacking)
            {
                this.stackKeyframes(true, this.stackingShowInteractionHints);
            }

            return true;
        }

        return super.subKeyPressed(context);
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        this.updateSidebarResizerState();
        super.render(context);

        this.handleMouse(context);

        context.batcher.clip(this.area, context);

        this.renderBackground(context);
        this.currentGraph.render(context);

        if (this.selecting)
        {
            context.batcher.normalizedBox(this.originalX, this.originalY, context.mouseX, context.mouseY, Colors.setA(Colors.ACTIVE, 0.25F));
        }

        this.currentGraph.postRender(context);

        context.batcher.unclip(context);

        this.selectSameInteraction.renderHint(context, this.area, this);
        this.selectNeighborInteraction.renderHint(context, this.area, this);
        this.pasteInteraction.renderHint(context, this.area, this);
        this.duplicateInteraction.renderHint(context, this.area, this);
        this.insertInteraction.renderHint(context, this.area, this);
        this.interactionOverlay.renderHint(context, this.area, this);
        this.renderTransformModeHints(context);
    }

    /**
     * Handle any related mouse logic during rendering
     */
    protected void handleMouse(UIContext context)
    {
        this.currentGraph.handleMouse(context, this.lastX, this.lastY);

        int mouseX = context.mouseX;
        int mouseY = context.mouseY;
        boolean mouseHasMoved = Math.abs(mouseX - this.originalX) > 2 || Math.abs(mouseY - this.originalY) > 2;

        if (this.scaling)
        {
            float tick = (float) this.fromGraphX(context.mouseX);
            float originalTick = (float) this.fromGraphX(this.originalX);
            float ratio = (tick - this.scalingAnchor) / (originalTick - this.scalingAnchor);

            for (Map.Entry<Keyframe, Float> entry : this.scaleTicks.entrySet())
            {
                Keyframe keyframe = entry.getKey();
                float oldTick = entry.getValue();
                float newTick = this.scalingAnchor + (oldTick - this.scalingAnchor) * ratio;

                if (Window.isCtrlPressed())
                {
                    newTick = Math.round(newTick);
                }

                keyframe.setTick(newTick, true);
            }
        }
        else if (this.dragging == 0 && mouseHasMoved)
        {
            this.dragging = 1;
        }
        else if (this.dragging == 1)
        {
            if (this.currentGraph.getSelected() != null)
            {
                this.currentGraph.dragKeyframes(context, this.draggingData, this.originalX, this.originalY, this.originalT, this.originalV);
            }
            else
            {
                this.moveNoKeyframes(context);
            }
        }

        this.lastX = mouseX;
        this.lastY = mouseY;

        this.selectSameInteraction.updatePreview(this, context);
        this.selectNeighborInteraction.updatePreview(this, context);
        this.pasteInteraction.updatePreview(this, context);
        this.duplicateInteraction.updatePreview(this, context);
        this.insertInteraction.updatePreview(this, context);
    }

    protected void moveNoKeyframes(UIContext context)
    {}

    /**
     * Render background, specifically backdrop and borders if the duration is present
     */
    protected void renderBackground(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xee0b0d12);
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + RULER_HEIGHT, 0xff111115);
        context.batcher.box(this.area.x, this.area.y + RULER_HEIGHT - 1, this.area.ex(), this.area.y + RULER_HEIGHT, 0x44ffffff);

        int duration = this.getDuration();

        if (duration > 0)
        {
            int leftBorder = this.toGraphX(0);
            int rightBorder = this.toGraphX(duration);
            int sidebarX = this.area.x + (this.currentGraph instanceof UIKeyframeDopeSheet ? this.currentGraph.getSidebarWidth() : 0);

            if (leftBorder > sidebarX) context.batcher.box(sidebarX, this.area.y, Math.min(this.area.ex(), leftBorder), this.area.y + this.area.h, 0x9905070b);
            if (rightBorder < this.area.ex()) context.batcher.box(Math.max(sidebarX, rightBorder), this.area.y, this.area.ex() , this.area.y + this.area.h, 0x9905070b);
        }

        if (this.backgroundRender != null)
        {
            this.backgroundRender.accept(context);
        }
    }

    private void updateSidebarResizerState()
    {
        if (this.sidebarResizer == null)
        {
            return;
        }

        boolean dopeSheet = this.currentGraph == this.dopeSheet;
        boolean locked = BBSSettings.editorLayoutSettings != null && BBSSettings.editorLayoutSettings.isLayoutLocked();
        boolean visible = dopeSheet && !locked;

        this.sidebarResizer.setVisible(visible);
        this.sidebarResizer.setEnabled(visible);

        if (!visible)
        {
            return;
        }

        this.dopeSheet.setSidebarWidth(this.dopeSheet.getSidebarWidth());

        int x = this.area.x + this.dopeSheet.getSidebarWidth() - 3;
        int y = this.area.my() - 20;

        this.sidebarResizer.relative(this).x(x - this.area.x).y(y - this.area.y).w(6).h(40);
        this.sidebarResizer.resize();
    }

    private void persistSidebarWidth()
    {
        if (BBSSettings.uiLayoutPreferences == null)
        {
            return;
        }

        BBSSettings.uiLayoutPreferences.setKeyframeSidebarWidth(this.dopeSheet.getSidebarWidth());
    }

    /* Caching state */

    public KeyframeState cacheState()
    {
        KeyframeState state = new KeyframeState();

        state.extra.putDouble("x_min", this.xAxis.getMinValue());
        state.extra.putDouble("x_max", this.xAxis.getMaxValue());
        this.currentGraph.saveState(state.extra);

        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            state.selected.add(new ArrayList<>(property.selection.getIndices()));
        }

        return state;
    }

    public void applyState(KeyframeState state)
    {
        this.xAxis.view(state.extra.getDouble("x_min"), state.extra.getDouble("x_max"));
        this.currentGraph.restoreState(state.extra);

        List<UIKeyframeSheet> properties = this.currentGraph.getSheets();

        for (int i = 0; i < properties.size(); i++)
        {
            if (CollectionUtils.inRange(state.selected, i))
            {
                properties.get(i).selection.clear();
                properties.get(i).selection.addAll(state.selected.get(i));
            }
        }

        this.currentGraph.pickSelected();
    }

    public void copyViewport(UIKeyframes lastEditor)
    {
        this.getDopeSheet().setSidebarWidth(lastEditor.getDopeSheet().getSidebarWidth());
        this.getDopeSheet().setTrackHeight(lastEditor.getDopeSheet().getTrackHeight());
        this.getXAxis().copy(lastEditor.getXAxis());
        this.getDopeSheet().getYAxis().copy(lastEditor.getDopeSheet().getYAxis());
    }

    public static class PastedKeyframes
    {
        public IKeyframeFactory factory;
        public List<Keyframe> keyframes = new ArrayList<>();

        public PastedKeyframes(IKeyframeFactory factory)
        {
            this.factory = factory;
        }
    }

    private static class SheetCache
    {
        public List<Pair<KeyframeChannel, UIKeyframeSheet>> data = new ArrayList<>();

        public SheetCache(Collection<UIKeyframeSheet> sheets)
        {
            for (UIKeyframeSheet sheet : sheets)
            {
                if (sheet.selection.hasAny())
                {
                    KeyframeChannel channel = new KeyframeChannel("", sheet.channel.getFactory());

                    channel.copyKeyframes(sheet.channel);
                    this.data.add(new Pair<>(channel, sheet));
                }
            }
        }
    }
}
