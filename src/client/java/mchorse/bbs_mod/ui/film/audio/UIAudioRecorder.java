package mchorse.bbs_mod.ui.film.audio;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.audio.wav.WaveWriter;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import org.lwjgl.glfw.GLFW;

import java.io.File;

public class UIAudioRecorder extends UIElement
{
    private static String lastInput = "";

    private final OpenALRecorder recorder;
    private float volume;

    public UIAudioRecorder(OpenALRecorder recorder)
    {
        this.recorder = recorder;

        this.eventPropagataion(EventPropagation.BLOCK);
    }

    public static void addOption(UIFilmPanel filmPanel, ContextMenuManager menu)
    {
        UIContext context = filmPanel.getContext();
        String timestampFilename = StringUtils.createTimestampFilename();
        String value = lastInput.isEmpty() ? timestampFilename : lastInput;

        menu.action(Icons.SOUND, UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE, () ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE_TITLE,
                UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE_DESCRIPTION,
                (t) ->
                {
                    String newT = t.isEmpty() ? timestampFilename : t;

                    UIElement overlay = context.menu.overlay;
                    OpenALRecorder recorder = new OpenALRecorder((wave) ->
                    {
                        try
                        {
                            File file = new File(BBSMod.getAudioFolder(), newT + ".wav");
                            AudioClientClip clip = new AudioClientClip();
                            Clips clips = filmPanel.cameraEditor.clips.getClips();

                            file.getParentFile().mkdirs();
                            WaveWriter.write(file, wave);
                            clip.audio.set(Link.assets("audio/" + newT + ".wav"));
                            clip.duration.set((int) (wave.getDuration() * 20));
                            clip.layer.set(clips.getTopLayer() + 1);

                            clips.addClip(clip);
                            filmPanel.cameraEditor.clips.clearSelection();
                            filmPanel.cameraEditor.clips.pickClip(clip);

                            lastInput = newT.equals(value) ? "" : newT;
                        }
                        catch (Exception e)
                        {}
                    });
                    UIAudioRecorder audioRecorder = new UIAudioRecorder(recorder);

                    audioRecorder.full(overlay);
                    audioRecorder.resize();
                    overlay.add(audioRecorder);

                    Thread thread = new Thread(recorder, "Супер классный, я записываю твой микрофон хихихи :3");

                    thread.start();
                }
            );

            panel.text.setText(value);
            panel.text.path();

            UIOverlay.addOverlay(context, panel);
        });
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.recorder.stop();
            context.render.postRunnable(this::removeFromParent);

            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.volume = Lerps.lerp(this.volume, Interpolations.CUBIC_OUT.interpolate(0F, 1F, this.recorder.getVolume()), 0.5F);

        String label = UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE_LABEL
            .format(this.recorder.getTime() / 1000F)
            .get();
        int x = this.area.mx();
        int y = this.area.my();
        int w = context.batcher.getFont().getWidth(label);
        double volume = Interpolations.EXP_OUT.interpolate(0F, 1F, this.volume);

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50);
        context.batcher.icon(Icons.SPHERE, Colors.RED | Colors.A100, x - w / 2 - 12, y + context.batcher.getFont().getHeight() / 2, 0.5F, 0.5F);
        context.batcher.textShadow(label, x - w / 2, y);

        label = UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE_SUBLABEL.get();
        w = context.batcher.getFont().getWidth(label);

        context.batcher.textShadow(label, x - w / 2, this.area.y(0.75F));

        x -= w / 2;

        context.batcher.box(x, y + 16, x + w, y + 20, Colors.A100);
        context.batcher.box(x, y + 16, x + (int) (w * volume), y + 20, Colors.WHITE);

        super.render(context);
    }
}