package mchorse.bbs_mod.ui.film.clips.renderer;

import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.HashMap;
import java.util.Map;

public class UIClipRenderers
{
    private static final Map<Class<? extends Clip>, IUIClipRenderer> EXTRA_RENDERERS = new HashMap<>();

    private UIClipRenderer defaultRenderer;

    private Map<Class, IUIClipRenderer> renderers = new HashMap<>();

    public static void registerExtra(Class<? extends Clip> key, IUIClipRenderer renderer)
    {
        if (key != null && renderer != null)
        {
            EXTRA_RENDERERS.put(key, renderer);
        }
    }

    public UIClipRenderers()
    {
        this.defaultRenderer = new UIClipRenderer();

        this.register(AudioClientClip.class, new UIAudioClipRenderer());
        this.register(VideoClip.class, new UIVideoClipRenderer());
        this.renderers.putAll(EXTRA_RENDERERS);
    }

    public void register(Class key, IUIClipRenderer renderer)
    {
        this.renderers.put(key, renderer);
    }

    public <T extends Clip> IUIClipRenderer<T> get(T clip)
    {
        IUIClipRenderer renderer = this.renderers.get(clip.getClass());

        return renderer == null ? this.defaultRenderer : renderer;
    }
}
