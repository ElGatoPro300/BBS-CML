package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.clips.UIBossBarColorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.IKeyframeShapeRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.KeyframeShapeRenderers;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class UIKeyframeFactory <T> extends UIElement
{
    public static final Map<IKeyframeFactory, IUIKeyframeFactoryFactory> FACTORIES = new HashMap<>();
    private static final Map<IKeyframeFactory, Integer> SCROLLS = new HashMap<>();

    public UIScrollView scroll;
    public UITrackpad tick;
    public UITrackpad duration;
    public UIIcon interp;

    public UIIcon shape;
    public UIColor color;

    protected Keyframe<T> keyframe;
    protected UIKeyframes editor;

    static
    {
        register(KeyframeFactories.ANCHOR, UIAnchorKeyframeFactory::new);
        register(KeyframeFactories.MOUNT_LINK, UIMountLinkKeyframeFactory::new);
        register(KeyframeFactories.LOOK_AT, UILookAtKeyframeFactory::new);
        register(KeyframeFactories.INVERSE_KINEMATICS, UIInverseKinematicsKeyframeFactory::new);
        register(KeyframeFactories.ILLUSION, UIIllusionKeyframeFactory::new);
        register(KeyframeFactories.BOOLEAN, UIBooleanKeyframeFactory::new);
        register(KeyframeFactories.COLOR, UIColorKeyframeFactory::new);
        register(KeyframeFactories.FLOAT, UIFloatKeyframeFactory::new);
        register(KeyframeFactories.DOUBLE, UIDoubleKeyframeFactory::new);
        register(KeyframeFactories.INTEGER, UIIntegerKeyframeFactory::new);
        register(KeyframeFactories.LINK, UILinkKeyframeFactory::new);
        register(KeyframeFactories.POSE, UIPoseKeyframeFactory::new);
        register(KeyframeFactories.STRING, UIStringKeyframeFactory::new);
        register(KeyframeFactories.TRANSFORM, UITransformKeyframeFactory::new);
        register(KeyframeFactories.VECTOR4F, UIVector4fKeyframeFactory::new);
        register(KeyframeFactories.BLOCK_STATE, UIBlockStateKeyframeFactory::new);
        register(KeyframeFactories.ITEM_STACK, UIItemStackKeyframeFactory::new);
        register(KeyframeFactories.ACTIONS_CONFIG, UIActionsConfigKeyframeFactory::new);
        register(KeyframeFactories.SHAPE_KEYS, UIShapeKeysKeyframeFactory::new);
        register(KeyframeFactories.PARTICLE_SETTINGS, UIParticleSettingsKeyframeFactory::new);
        register(KeyframeFactories.STRUCTURE_LIGHT_SETTINGS, UIStructureLightSettingsKeyframeFactory::new);
        register(KeyframeFactories.GLOW_SETTINGS, UIGlowSettingsKeyframeFactory::new);
        register(KeyframeFactories.PAINT_SETTINGS, UIPaintSettingsKeyframeFactory::new);
        register(KeyframeFactories.SHADOW_SETTINGS, UIShadowSettingsKeyframeFactory::new);
        register(KeyframeFactories.CHROMA_SKY_SETTINGS, UIChromaSkyCurveSettingsKeyframeFactory::new);
    }

    public static <T> void register(IKeyframeFactory<T> clazz, IUIKeyframeFactoryFactory<T> factory)
    {
        FACTORIES.put(clazz, factory);
    }

    private static boolean isFormColorPropertyId(String id)
    {
        if (id == null)
        {
            return false;
        }

        String name = id;
        int slash = id.lastIndexOf('/');

        if (slash >= 0 && slash + 1 < id.length())
        {
            name = id.substring(slash + 1);
        }

        return "color".equals(name) || "color_overlay".equals(name)
            || (name.startsWith("color_overlay") && name.length() > "color_overlay".length());
    }

    public static void saveScroll(UIKeyframeFactory editor)
    {
        if (editor != null)
        {
            SCROLLS.put(editor.keyframe.getFactory(), (int) editor.scroll.scroll.getScroll());
            editor.saveUiState();
        }
    }

    public static <T> UIKeyframeFactory createPanel(Keyframe<T> keyframe, UIKeyframes editor)
    {
        if (keyframe.getFactory() == KeyframeFactories.BOOLEAN && editor != null)
        {
            UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

            if (sheet != null && FormUtils.isVisiblePropertyPath(sheet.id))
            {
                @SuppressWarnings("unchecked")
                Keyframe<Boolean> booleanKeyframe = (Keyframe<Boolean>) keyframe;

                return new UIVisibleKeyframeFactory(booleanKeyframe, editor);
            }
        }

        if (keyframe.getFactory() == KeyframeFactories.DOUBLE && editor != null)
        {
            UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

            /* Eye blink only — Letterbox also has height+color and must stay unbounded. */
            if (sheet != null && "height".equals(sheet.id)
                && editor.getGraph().getSheet("color") != null
                && editor.getGraph().getSheet("color_opacity") != null)
            {
                @SuppressWarnings("unchecked")
                Keyframe<Double> doubleKeyframe = (Keyframe<Double>) keyframe;

                return new UIEyeBlinkKeyframeFactory(doubleKeyframe, editor);
            }

            if (sheet != null && "particles".equals(sheet.id))
            {
                @SuppressWarnings("unchecked")
                Keyframe<Double> doubleKeyframe = (Keyframe<Double>) keyframe;

                return new UIParticlesKeyframeFactory(doubleKeyframe, editor);
            }

            if (sheet != null && ("using_item".equals(sheet.id) || sheet.id.endsWith("/using_item")))
            {
                @SuppressWarnings("unchecked")
                Keyframe<Double> doubleKeyframe = (Keyframe<Double>) keyframe;

                return new UIUsingItemKeyframeFactory(doubleKeyframe, editor);
            }
        }

        if (keyframe.getFactory() == KeyframeFactories.COLOR && editor != null)
        {
            UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

            if (sheet != null && "color".equals(sheet.id) && "bar".equals(sheet.groupKey))
            {
                @SuppressWarnings("unchecked")
                Keyframe<Color> colorKeyframe = (Keyframe<Color>) keyframe;

                return new UIBossBarColorKeyframeFactory(colorKeyframe, editor);
            }

            /* Form Color track (property-bound). Camera clips like Letterbox/Eye use simple color+alpha. */
            if (sheet != null && sheet.property != null && isFormColorPropertyId(sheet.id))
            {
                @SuppressWarnings("unchecked")
                Keyframe<Color> colorKeyframe = (Keyframe<Color>) keyframe;

                return new UIFormColorKeyframeFactory(colorKeyframe, editor);
            }
        }

        if (keyframe.getFactory() == KeyframeFactories.FLOAT && editor != null)
        {
            UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

            if (sheet != null && "opacity".equals(sheet.id))
            {
                @SuppressWarnings("unchecked")
                Keyframe<Float> opacityKeyframe = (Keyframe<Float>) keyframe;

                return new UIOpacityKeyframeFactory(opacityKeyframe, editor);
            }
        }

        IUIKeyframeFactoryFactory<T> factory = FACTORIES.get(keyframe.getFactory());
        UIKeyframeFactory uiEditor = factory == null ? null : factory.create(keyframe, editor);

        if (uiEditor != null)
        {
            uiEditor.scroll.scroll.setScroll(SCROLLS.getOrDefault(keyframe.getFactory(), 0));
        }

        return uiEditor;
    }

    public UIKeyframeFactory(Keyframe<T> keyframe, UIKeyframes editor)
    {
        this.keyframe = keyframe;
        this.editor = editor;

        this.scroll = UI.scrollView(5, 10);
        this.scroll.scroll.cancelScrolling();
        this.scroll.full(this);

        this.tick = new UITrackpad(this::setTick)
        {
            @Override
            public void focus(UIContext context)
            {
                UIKeyframeFactory.this.syncTickFromKeyframe();
                super.focus(context);
            }
        };
        this.tick.tooltip(UIKeys.KEYFRAMES_TICK);
        this.tick.getEvents().register(UITrackpadDragStartEvent.class, (e) ->
        {
            /* Must run before lastValue is captured (see UITrackpad). */
            this.syncTickFromKeyframe();
            this.editor.cacheKeyframes();
        });
        this.tick.getEvents().register(UITrackpadDragEndEvent.class, (e) ->
        {
            this.editor.submitKeyframes();
            /* submitKeyframes() replaces channel keyframe instances — rebind. */
            this.rebindKeyframe();
            this.syncTickFromKeyframe();
        });
        this.duration = new UITrackpad((v) -> this.setDuration(v.floatValue()))
        {
            @Override
            public void focus(UIContext context)
            {
                this.setValue(TimeUtils.toTime(UIKeyframeFactory.this.getKeyframe().getDuration()));
                super.focus(context);
            }
        };
        this.duration.limit(0, Float.MAX_VALUE).tooltip(UIKeys.KEYFRAMES_FORCED_DURATION);
        this.duration.getEvents().register(UITrackpadDragStartEvent.class, (e) ->
        {
            this.rebindKeyframe();
            this.duration.setValue(TimeUtils.toTime(this.keyframe.getDuration()));
            this.editor.cacheKeyframes();
        });
        this.duration.getEvents().register(UITrackpadDragEndEvent.class, (e) ->
        {
            this.editor.submitKeyframes();
            this.rebindKeyframe();
            this.duration.setValue(TimeUtils.toTime(this.keyframe.getDuration()));
        });
        this.interp = new UIIcon(Icons.GRAPH, (b) ->
        {
            Interpolation interp = this.keyframe.getInterpolation();
            UIInterpolationContextMenu menu = new UIInterpolationContextMenu(interp);

            this.getContext().replaceContextMenu(menu.callback(() -> this.editor.getGraph().setInterpolation(interp)));
        });
        this.interp.tooltip(new InterpolationTooltip(0F, 0.5F, () -> this.keyframe.getInterpolation()));
        this.interp.keys().register(Keys.KEYFRAMES_INTERP, this.interp::clickItself).category(UIKeys.KEYFRAMES_KEYS_CATEGORY);

        this.color = new UIColor((c) ->
        {
            for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
            {
                /* RGB picker has no alpha channel; force opaque so the keyframe stops provisional blinking. */
                for (Keyframe kf : sheet.selection.getSelected()) kf.setColor(new Color().set(c, false));
            }
        });
        this.color.setColor(keyframe.getColor() == null ? 0 : keyframe.getColor().getRGBColor());
        this.color.tooltip(UIKeys.KEYFRAMES_CHANGE_COLOR);
        this.color.context((menu) ->
        {
            menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, () ->
            {
                for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
                {
                    for (Keyframe kf : sheet.selection.getSelected()) kf.setColor(null);
                }

                this.color.setColor(0);
            });
        });

        this.shape = new UIIcon(Icons.SHAPES, (b) ->
        {
            KeyframeShape currentShape = keyframe.getShape() == null ? KeyframeShape.SQUARE : keyframe.getShape();

            this.getContext().replaceContextMenu((menu) ->
            {
                for (KeyframeShape shape : KeyframeShape.values())
                {
                    IKeyframeShapeRenderer shapeRenderer = KeyframeShapeRenderers.SHAPES.get(shape);

                    menu.action(shapeRenderer.getIcon(), shapeRenderer.getLabel(), shape == currentShape, () ->
                    {
                        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
                        {
                            for (Keyframe kf : sheet.selection.getSelected())
                            {
                                kf.setShape(shape);
                            }
                        }
                    });
                }
            });
        });
        this.shape.tooltip(UIKeys.KEYFRAMES_CHANGE_SHAPE);

        this.scroll.add(UI.row(this.interp, this.tick, this.duration));
        this.scroll.add(UI.row(this.shape, this.color));

        this.add(this.scroll);

        /* Fill data */
        this.tick.setValue(TimeUtils.toTime(keyframe.getTick()));
        this.duration.setValue(TimeUtils.toTime(keyframe.getDuration()));
    }

    public Keyframe<T> getKeyframe()
    {
        this.rebindKeyframe();

        return this.keyframe;
    }

    public void setTick(double tick)
    {
        this.rebindKeyframe();

        if (this.keyframe == null)
        {
            return;
        }

        float time = (float) TimeUtils.fromTime(tick);
        float diff = time - this.keyframe.getTick();

        if (Math.abs(diff) > 1.0E-6F)
        {
            boolean movedSelection = false;
            UIKeyframeSheet hostSheet = this.editor.getGraph().getSheet(this.keyframe);

            if (hostSheet != null && "color".equals(hostSheet.id))
            {
                if (hostSheet.selection.hasAny())
                {
                    mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils.moveCompanionPaintForSelectedColor(this.editor, diff);
                }
                else
                {
                    mchorse.bbs_mod.ui.film.replays.UIReplaysEditor replays = this.editor.getParent(mchorse.bbs_mod.ui.film.replays.UIReplaysEditor.class);

                    if (replays != null && replays.getReplay() != null)
                    {
                        replays.getReplay().properties.moveCompanionPaintBy(diff, java.util.Collections.singletonList(this.keyframe.getTick()));
                    }
                }
            }

            /* Diff is relative to the panel's (live) keyframe so the trackpad
             * absolute value stays the source of truth. */
            for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
            {
                if (sheet.selection.hasAny())
                {
                    sheet.setTickBy(diff, false);
                    movedSelection = true;
                }
            }

            if (!movedSelection || Math.abs(this.keyframe.getTick() - time) > 1.0E-5F)
            {
                this.keyframe.setTick(time, !movedSelection);
            }
        }

        if (!this.tick.isActivelyEditing() && !this.tick.isDragging())
        {
            this.syncTickFromKeyframe();
            this.editor.triggerChange();
        }
    }

    public void setDuration(float value)
    {
        this.rebindKeyframe();
        this.editor.getGraph().setDuration(value);

        if (!this.duration.isActivelyEditing() && !this.duration.isDragging())
        {
            this.editor.triggerChange();
        }
    }

    public void setValue(Object value)
    {
        this.rebindKeyframe();
        this.editor.getGraph().setValue(value, true);
        this.editor.triggerChange();
    }

    /**
     * Persist ephemeral UI (collapse open/closed, etc.) before the panel is rebuilt
     * for another keyframe of the same factory type.
     */
    public void saveUiState()
    {}

    /**
     * Re-apply {@link #saveUiState()} after the new panel is parented and resized
     * (collapse shells need a parent to open).
     */
    public void restoreUiState()
    {}

    public void update()
    {
        this.rebindKeyframe();

        if (!this.tick.isActivelyEditing() && !this.tick.isDragging())
        {
            this.syncTickFromKeyframe();
        }

        if (!this.duration.isActivelyEditing() && !this.duration.isDragging())
        {
            this.duration.setValue(TimeUtils.toTime(this.keyframe.getDuration()));
        }
    }

    private void syncTickFromKeyframe()
    {
        this.rebindKeyframe();

        if (this.keyframe != null)
        {
            this.tick.setValue(TimeUtils.toTime(this.keyframe.getTick()));
        }
    }

    /**
     * {@link UIKeyframes#submitKeyframes()} replaces channel keyframe instances
     * via {@code copyKeyframes}. The factory must point at the live selected
     * keyframe or {@link #update()} will keep resetting inputs from an orphan.
     */
    @SuppressWarnings("unchecked")
    protected void rebindKeyframe()
    {
        if (this.keyframe == null || this.editor == null)
        {
            return;
        }

        if (this.keyframe.getParent() instanceof KeyframeChannel<?> channel
            && channel.getKeyframes().contains(this.keyframe))
        {
            return;
        }

        Keyframe<?> selected = this.editor.getGraph().getSelected();

        if (selected != null && selected.getFactory() == this.keyframe.getFactory())
        {
            this.keyframe = (Keyframe<T>) selected;

            return;
        }

        UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);

        if (sheet != null && sheet.selection.hasAny())
        {
            List<Keyframe> selectedList = sheet.selection.getSelected();

            if (!selectedList.isEmpty())
            {
                this.keyframe = (Keyframe<T>) selectedList.get(0);
            }
        }
    }

    /**
     * Wire undo cache/submit and live rebind for a keyframe value trackpad.
     */
    protected void registerValueTrackpad(UITrackpad trackpad)
    {
        trackpad.getEvents().register(UITrackpadDragStartEvent.class, (e) ->
        {
            this.rebindKeyframe();
            this.editor.cacheKeyframes();
        });
        trackpad.getEvents().register(UITrackpadDragEndEvent.class, (e) ->
        {
            this.editor.submitKeyframes();
            this.rebindKeyframe();
        });
    }

    public static interface IUIKeyframeFactoryFactory <T>
    {
        public UIKeyframeFactory<T> create(Keyframe<T> keyframe, UIKeyframes editor);
    }
}
