package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.TrackpadRecorder;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import org.lwjgl.glfw.GLFW;

/**
 * Base class for numeric keyframe factories (Double, Float, Integer) with recording support.
 */
public abstract class UINumericKeyframeFactory<T extends Number> extends UIKeyframeFactory<T>
{
    protected UITrackpad value;
    protected UIBezierHandles handles;
    
    private TrackpadRecorder trackpadRecorder;
    private boolean recordingMode;
    private boolean recordingInitialized;
    private int lastMouseX;
    private double lastRecordedValue;
    private boolean editingMode;
    private double editingInitialValue;

    public UINumericKeyframeFactory(Keyframe<T> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad((v) -> this.setValue(v));
        this.value.setValue(this.getNumericValue(keyframe.getValue()));
        this.handles = new UIBezierHandles(keyframe);

        this.setupRecordingContextMenu();
        this.keys().register(Keys.TRANSFORMATIONS_TRANSLATE, this::startEditingMode).category(UIKeys.TRANSFORMS_KEYS_CATEGORY);
        this.scroll.add(this.value, this.handles.createColumn());
    }

    /**
     * Convert typed value to double for trackpad display.
     */
    protected abstract double getNumericValue(T value);

    /**
     * Convert double value back to typed value and update keyframe.
     */
    protected abstract void setKeyframeValue(double value);
    
    /**
     * Create a value converter for the recorder.
     */
    protected abstract TrackpadRecorder.ValueConverter createValueConverter();

    /**
     * Override parent's setValue to handle numeric conversion.
     */
    private void setValue(double value)
    {
        this.setKeyframeValue(value);
        this.editor.getGraph().setValue(this.keyframe.getValue(), true);
    }

    private void setupRecordingContextMenu()
    {
        this.value.context((menu) ->
        {
            KeyframeChannel<?> channel = this.getKeyframeChannel();

            if (channel != null)
            {
                menu.action(Icons.SPHERE, UIKeys.KEYFRAMES_RECORD_VALUE, () -> this.startRecording(channel));
            }
        });
    }

    private KeyframeChannel<?> getKeyframeChannel()
    {
        if (this.editor != null && this.editor.getGraph() != null)
        {
            for (var sheet : this.editor.getGraph().getSheets())
            {
                if (sheet.channel != null && sheet.channel.getKeyframes().contains(this.keyframe))
                {
                    return sheet.channel;
                }
            }
        }

        return null;
    }

    private void startRecording(KeyframeChannel<?> channel)
    {
        if (this.trackpadRecorder == null)
        {
            this.trackpadRecorder = new TrackpadRecorder(channel, this.editor, this.createValueConverter());
        }

        this.stopEditingMode(false);
        this.recordingMode = true;
        this.recordingInitialized = false;
        
        this.startPlaybackIfNeeded();
    }

    private void startEditingMode()
    {
        if (this.recordingMode)
        {
            return;
        }

        UIContext context = this.getContext();
        if (context == null)
        {
            return;
        }

        this.editingInitialValue = this.value.getValue();
        this.lastMouseX = context.mouseX;
        this.editingMode = true;
    }

    private void stopEditingMode(boolean accept)
    {
        if (!this.editingMode)
        {
            return;
        }

        this.editingMode = false;

        if (!accept)
        {
            this.value.setValue(this.editingInitialValue);
            this.setValue(this.editingInitialValue);
        }
    }
    
    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.editingMode)
        {
            if (context.mouseButton == 0)
            {
                this.stopEditingMode(true);

                return true;
            }
            else if (context.mouseButton == 1)
            {
                this.stopEditingMode(false);

                return true;
            }
        }

        if (this.recordingMode && context.mouseButton == 0)
        {
            return true;
        }
        
        return super.subMouseClicked(context);
    }
    
    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.recordingMode && context.mouseButton == 0)
        {
            this.recordingMode = false;
            return true;
        }
        
        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.editingMode)
        {
            if (context.isPressed(GLFW.GLFW_KEY_ENTER))
            {
                this.stopEditingMode(true);

                return true;
            }
            else if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.stopEditingMode(false);

                return true;
            }
        }

        return super.subKeyPressed(context);
    }
    
    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (this.editingMode)
        {
            int dx = context.mouseX - this.lastMouseX;

            if (dx != 0)
            {
                double modifier = this.value.getValueModifier();
                double newValue = MathUtils.clamp(this.value.getValue() + dx * modifier, this.value.min, this.value.max);

                if (this.value.integer)
                {
                    newValue = (int) newValue;
                }

                this.value.setValue(newValue);
                this.setValue(newValue);
                this.lastMouseX = context.mouseX;
            }
        }
        
        if (this.recordingMode && this.isPlaybackRunning())
        {
            if (!this.recordingInitialized)
            {
                this.lastMouseX = context.mouseX;
                this.lastRecordedValue = this.value.getValue();
                this.recordingInitialized = true;
            }
            
            int dx = context.mouseX - this.lastMouseX;
            
            if (dx != 0)
            {
                double valueModifier = this.value.getValueModifier();
                double newValue = this.lastRecordedValue + (dx * valueModifier);
                newValue = MathUtils.clamp(newValue, this.value.min, this.value.max);
                
                if (this.value.integer)
                {
                    newValue = (int) newValue;
                }
                
                this.value.setValue(newValue);
                
                this.lastMouseX = context.mouseX;
                this.lastRecordedValue = newValue;
            }
            
            this.trackpadRecorder.recordValue(this.lastRecordedValue);
        }
        else if (this.recordingMode)
        {
            this.recordingMode = false;
            this.recordingInitialized = false;
        }

        if (this.editingMode)
        {
            String label = UIKeys.TRANSFORMS_EDITING.get();
            FontRenderer font = context.batcher.getFont();
            int x = this.area.mx(font.getWidth(label));
            int y = this.area.my(font.getHeight());

            context.batcher.textCard(label, x, y, Colors.WHITE, Colors.A50);
        }
    }
    
    private UIFilmKeyframes getFilmKeyframes()
    {
        return this.editor instanceof UIFilmKeyframes ? (UIFilmKeyframes) this.editor : null;
    }
    
    private boolean isPlaybackRunning()
    {
        UIFilmKeyframes filmKeyframes = this.getFilmKeyframes();
        return filmKeyframes != null && filmKeyframes.editor != null && filmKeyframes.editor.isRunning();
    }
    
    private void startPlaybackIfNeeded()
    {
        UIFilmKeyframes filmKeyframes = this.getFilmKeyframes();
        
        if (filmKeyframes != null && filmKeyframes.editor != null && !filmKeyframes.editor.isRunning())
        {
            filmKeyframes.editor.togglePlayback();
        }
    }

    @Override
    public void update()
    {
        super.update();

        this.value.setValue(this.getNumericValue(this.keyframe.getValue()));
        this.handles.update();
    }
}
