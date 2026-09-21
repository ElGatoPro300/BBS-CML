package mchorse.bbs_mod.camera.controller;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.camera.clips.screen.ColorEffect;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;

public abstract class CameraWorkCameraController implements ICameraController
{
    protected CameraClipContext context;
    protected Position position = new Position();

    public CameraWorkCameraController()
    {
        this.context = new CameraClipContext();
    }

    public CameraWorkCameraController setWork(Clips clips)
    {
        this.context.clips = clips;

        return this;
    }

    public CameraClipContext getContext()
    {
        return this.context;
    }

    public Position getPosition()
    {
        return this.position;
    }

    protected void apply(Camera camera, int ticks, float transition)
    {
        this.apply(camera, ticks, transition, true);
    }

    /**
     * @param applyTransform when false, clip position/rotation/FOV are evaluated for
     *        timeline context (audio, screen effects) but not written back to the camera.
     */
    protected void apply(Camera camera, int ticks, float transition, boolean applyTransform)
    {
        BBSRendering.setLensOverscanScale(1F);

        if (camera != null)
        {
            this.position.set(camera);
        }

        this.context.clipData.clear();
        this.context.setup(ticks, transition);

        for (Clip clip : this.context.clips.getClips(ticks))
        {
            this.context.apply(clip, this.position);
        }

        this.resetFisheyeFovOverscan();

        AudioClientClip.manageSounds(this.context);

        this.context.currentLayer = 0;

        if (camera != null && applyTransform)
        {
            this.position.apply(camera);
        }
    }

    /**
     * Keep fisheye as a single-render post-process. The shader works from a copy of
     * the native-FOV framebuffer so pixels outside a partial radius remain sharp.
     */
    private void resetFisheyeFovOverscan()
    {
        BBSRendering.setLensOverscanScale(1F);

        for (ColorEffect effect : ColorClip.getEffects(this.context))
        {
            if (effect.hasCinematic)
            {
                effect.lensOverscan = 1F;
            }
        }
    }

    @Override
    public int getPriority()
    {
        return 10;
    }
}
