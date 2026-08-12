package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UICustomInterpolationKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.InterpolationUtils;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.CustomInterpolation;
import mchorse.bbs_mod.utils.interps.CustomInterpolationManager;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.InterpContext;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.utils.undo.UndoManager;

import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.function.Consumer;

public class UICustomInterpolationPanel extends UIOverlayPanel
{
    public UITextbox name;
    public UIToggle continuous;
    public UITrackpad samples;
    public UIToggle clamp;
    public UIButton useBase;
    public UICustomInterpolationKeyframes keyframes;
    public UIButton save;

    private CustomInterpolation interpolation;
    private boolean firstResize = true;
    private Consumer<CustomInterpolation> saveCallback;

    private UndoManager<UICustomInterpolationPanel> undoManager;
    private MapType lastState;

    public UICustomInterpolationPanel()
    {
        super(UIKeys.INTERPOLATIONS_CUSTOM_TITLE);

        this.interpolation = new CustomInterpolation("custom");
        this.undoManager = new UndoManager<>(50);

        this.name = new UITextbox(1000, (t) -> {});
        this.name.setText("custom");

        this.continuous = new UIToggle(UIKeys.INTERPOLATIONS_CUSTOM_CONTINUOUS, (b) -> this.toggleContinuous());
        
        this.samples = new UITrackpad((v) -> {});
        this.samples.limit(2, 500).integer().setValue(10);
        this.samples.tooltip(UIKeys.INTERPOLATIONS_CUSTOM_SAMPLES);

        this.clamp = new UIToggle(UIKeys.INTERPOLATIONS_CUSTOM_CLAMP, (b) -> {});
        this.clamp.setValue(false);
        this.clamp.tooltip(UIKeys.INTERPOLATIONS_CUSTOM_CLAMP_TOOLTIP);

        this.useBase = new UIButton(UIKeys.INTERPOLATIONS_CUSTOM_USE_BASE, (b) -> this.pickBaseInterpolation());

        this.keyframes = new UICustomInterpolationKeyframes((k) -> {});
        this.keyframes.single().duration(() -> 1);
        this.keyframes.changed(this::markUndo);

        KeyframeChannel<Double> channel = new KeyframeChannel<>("interp", KeyframeFactories.DOUBLE);
        channel.insert(0, 0D);
        channel.insert(1, 1D);

        for (Keyframe keyframe : channel.getKeyframes())
        {
            keyframe.getInterpolation().setInterp(Interpolations.BEZIER);
            keyframe.lx = 0.15f;
            keyframe.rx = 0.15f;
        }

        UIKeyframeSheet sheet = new UIKeyframeSheet("interp", UIKeys.RAW_INTERP, Colors.ACTIVE, false, channel, null);

        this.keyframes.addSheet(sheet);
        this.keyframes.editSheet(sheet);
        this.keyframes.resetView();

        this.save = new UIButton(UIKeys.INTERPOLATIONS_CUSTOM_SAVE, (b) -> this.saveInterpolation());

        UILabel label = UI.label(UIKeys.INTERPOLATIONS_CUSTOM_NAME).color(Colors.WHITE, true);
        UIElement sidebar = UI.column(5, 10, label, this.name, this.continuous, this.samples, this.clamp, this.useBase, this.save);

        sidebar.relative(this.content).x(1F).y(0).w(140).h(1F).anchorX(1F);
        this.keyframes.relative(this.content).x(0).y(0).w(1F, -140).h(1F);

        this.content.add(sidebar, this.keyframes);
        
        this.lastState = new MapType();
        this.lastState.put("channel", channel.toData());
        this.lastState.putBool("continuous", this.continuous.getValue());
    }

    public UICustomInterpolationPanel set(CustomInterpolation interpolation)
    {
        this.interpolation = interpolation;

        this.name.setText(interpolation.getKey());
        this.continuous.setValue(interpolation.continuous);

        KeyframeChannel<Double> channel = this.keyframes.getGraph().getSheets().get(0).channel;

        channel.fromData(interpolation.channel.toData());
        this.keyframes.resetView();

        return this;
    }

    public void saveInterpolation()
    {
        String name = this.name.getText();

        if (name.isEmpty())
        {
            return;
        }

        this.interpolation = new CustomInterpolation(name);
        this.interpolation.continuous = this.continuous.getValue();

        if (!this.keyframes.getGraph().getSheets().isEmpty())
        {
            UIKeyframeSheet sheet = this.keyframes.getGraph().getSheets().get(0);

            this.interpolation.channel.fromData(sheet.channel.toData());
        }

        CustomInterpolationManager.INSTANCE.save(this.interpolation);

        if (this.saveCallback != null)
        {
            this.saveCallback.accept(this.interpolation);
        }

        this.close();
    }

    public UICustomInterpolationPanel onSave(Consumer<CustomInterpolation> callback)
    {
        this.saveCallback = callback;

        return this;
    }

    private void toggleContinuous()
    {
        this.interpolation.continuous = this.continuous.getValue();
        this.markUndo();
    }

    private void pickBaseInterpolation()
    {
        Interpolation dummy = new Interpolation("dummy", Interpolations.MAP, Interpolations.LINEAR);
        UIInterpolationContextMenu menu = new UIInterpolationContextMenu(dummy);

        menu.callback(() ->
        {
            this.applyBaseInterpolation(dummy);
        });

        this.getContext().replaceContextMenu(menu);
    }

    private void applyBaseInterpolation(IInterp interp)
    {
        KeyframeChannel<Double> channel = this.keyframes.getGraph().getSheets().get(0).channel;
        
        channel.removeAll();
        channel.insert(0, 0D);
        channel.insert(1, 1D);

        int samples = (int) this.samples.getValue();
        double step = 1.0 / samples;
        boolean clamp = this.clamp.getValue();
        
        EasingArgs args = null;
        if (interp instanceof Interpolation)
        {
            args = ((Interpolation) interp).getArgs();
        }

        for (int i = 1; i < samples; i++)
        {
            double x = i * step;
            double y = this.sample(interp, x, args, clamp);

            channel.insert((float) x, y);
        }

        for (int i = 0; i <= samples; i++)
        {
            Keyframe<Double> k = channel.get(i);
            double x = k.getTick();
            double y = k.getValue();

            // Calculate slope (derivative)
            double slope = 0;
            double epsilon = 0.001;

            if (i == 0)
            {
                double nextY = this.sample(interp, x + epsilon, args, clamp);
                slope = (nextY - y) / epsilon;
            }
            else if (i == samples)
            {
                double prevY = this.sample(interp, x - epsilon, args, clamp);
                slope = (y - prevY) / epsilon;
            }
            else
            {
                double prevY = this.sample(interp, x - epsilon, args, clamp);
                double nextY = this.sample(interp, x + epsilon, args, clamp);
                slope = (nextY - prevY) / (2 * epsilon);
            }

            // Adjust handle length
            double handleX = step / 3.0; // Approximation for Bezier
            
            k.getInterpolation().setInterp(Interpolations.BEZIER);
            k.lx = (float) handleX;
            k.rx = (float) handleX;
            k.ly = (float) (-slope * handleX);
            k.ry = (float) (slope * handleX);
        }

        this.keyframes.resetView();
        this.markUndo();
    }
    
    private double sample(IInterp interp, double x, EasingArgs args, boolean clamp)
    {
        IInterp.context.set(0, 1, x);
        if (clamp) IInterp.context.setBoundary(true, true);
        if (args != null) IInterp.context.extra(args);
        
        return interp.interpolate(IInterp.context);
    }

    @Override
    public void resize()
    {
        super.resize();

        if (this.firstResize)
        {
            this.keyframes.resetView();
            this.firstResize = false;
        }
    }
    
    private void markUndo()
    {
        MapType state = new MapType();
        
        if (!this.keyframes.getGraph().getSheets().isEmpty())
        {
             state.put("channel", this.keyframes.getGraph().getSheets().get(0).channel.toData());
        }
        
        state.putBool("continuous", this.continuous.getValue());
        
        this.undoManager.pushUndo(new InterpolationUndo(this.lastState, state));
        this.lastState = state;
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_Z) && Window.isCtrlPressed())
        {
            if (Window.isShiftPressed())
            {
                this.undoManager.redo(this);
            }
            else
            {
                this.undoManager.undo(this);
            }
            
            return true;
        }
        else if (context.isPressed(GLFW.GLFW_KEY_Y) && Window.isCtrlPressed())
        {
            this.undoManager.redo(this);
            
            return true;
        }
        
        return super.subKeyPressed(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        boolean result = super.subMouseReleased(context);
        
        this.undoManager.markLastUndoNoMerging();
        
        return result;
    }
    
    public static class InterpolationUndo implements IUndo<UICustomInterpolationPanel>
    {
        public MapType before;
        public MapType after;
        private boolean mergable = true;

        public InterpolationUndo(MapType before, MapType after)
        {
            this.before = before;
            this.after = after;
        }

        @Override
        public IUndo<UICustomInterpolationPanel> noMerging()
        {
            this.mergable = false;
            return this;
        }

        @Override
        public void undo(UICustomInterpolationPanel context)
        {
            this.apply(context, this.before);
        }

        @Override
        public void redo(UICustomInterpolationPanel context)
        {
            this.apply(context, this.after);
        }

        private void apply(UICustomInterpolationPanel context, MapType state)
        {
            if (state.has("channel") && !context.keyframes.getGraph().getSheets().isEmpty())
            {
                context.keyframes.getGraph().getSheets().get(0).channel.fromData(state.getMap("channel"));
            }
            
            if (state.has("continuous"))
            {
                boolean continuous = state.getBool("continuous");
                
                context.continuous.setValue(continuous);
                context.interpolation.continuous = continuous;
            }
            
            context.keyframes.resetView();
            context.lastState = state;
        }

        @Override
        public void merge(IUndo<UICustomInterpolationPanel> undo)
        {
             if (undo instanceof InterpolationUndo)
             {
                 this.after = ((InterpolationUndo) undo).after;
             }
        }

        @Override
        public boolean isMergeable(IUndo<UICustomInterpolationPanel> undo)
        {
            return this.mergable && undo instanceof InterpolationUndo;
        }
    }
}
