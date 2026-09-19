package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/**
 * Handles recording trackpad value changes into keyframe channels.
 * Records values only when film playback is active.
 */
public class TrackpadRecorder
{
    private KeyframeChannel<?> channel;
    private UIKeyframes editor;
    private ValueConverter converter;
    
    public TrackpadRecorder(KeyframeChannel<?> channel, UIKeyframes editor, ValueConverter converter)
    {
        this.channel = channel;
        this.editor = editor;
        this.converter = converter;
    }
    
    /**
     * Records the current value as a keyframe at the current tick.
     */
    public void recordValue(double value)
    {
        int tick = this.getCurrentTick();
        KeyframeChannel<Object> typedChannel = (KeyframeChannel<Object>) this.channel;
        Object typedValue = this.converter.convert(value);
        
        typedChannel.insert(tick, typedValue);
    }
    
    /**
     * Gets the current tick from the film panel or recorder.
     */
    private int getCurrentTick()
    {
        Recorder recorder = BBSModClient.getFilms().getRecorder();
        
        if (recorder != null && !recorder.hasNotStarted())
        {
            return recorder.getTick();
        }
        
        if (this.editor instanceof UIFilmKeyframes)
        {
            UIFilmKeyframes filmKeyframes = (UIFilmKeyframes) this.editor;
            
            if (filmKeyframes.editor != null)
            {
                return filmKeyframes.editor.getCursor();
            }
        }
        
        return (int) this.editor.getTick();
    }
    
    /**
     * Functional interface for converting double values to the appropriate type.
     */
    @FunctionalInterface
    public interface ValueConverter
    {
        Object convert(double value);
    }
}
