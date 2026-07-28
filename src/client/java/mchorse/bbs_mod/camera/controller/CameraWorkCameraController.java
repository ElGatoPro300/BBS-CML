package mchorse.bbs_mod.camera.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.camera.clips.screen.ColorEffect;
import mchorse.bbs_mod.camera.clips.screen.LensDistortionOverscan;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.MathUtils;
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
     * @param applyTransform when false, clip position/rotation are evaluated but only
     *        FOV is written back (free-camera preview still needs fisheye FOV overscan).
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

        this.applyFisheyeFovOverscan();

        AudioClientClip.manageSounds(this.context);

        this.context.currentLayer = 0;

        if (camera != null)
        {
            if (applyTransform)
            {
                this.position.apply(camera);
            }
            else
            {
                camera.fov = MathUtils.toRad(this.position.angle.fov);
            }
        }
    }

    /**
     * Match render FOV to fisheye strength: widen for positive k, narrow for negative k,
     * so the post-process UV warp can map screen edges to rendered image edges.
     */
    private void applyFisheyeFovOverscan()
    {
        if (BBSSettings.editorFisheyeWidenFov == null || !BBSSettings.editorFisheyeWidenFov.get())
        {
            BBSRendering.setLensOverscanScale(1F);

            return;
        }

        float lens = 0F;

        for (ColorEffect effect : ColorClip.getEffects(this.context))
        {
            if (effect.hasCinematic)
            {
                lens += effect.lensDistortion;
            }
        }

        if (Math.abs(lens) <= 1.0e-6F)
        {
            BBSRendering.setLensOverscanScale(1F);

            return;
        }

        float fovBefore = this.position.angle.fov;
        float fovAfter = LensDistortionOverscan.adjustFovDegrees(fovBefore, lens);
        float scale = LensDistortionOverscan.scaleBetweenFovDegrees(fovBefore, fovAfter);

        this.position.angle.fov = fovAfter;
        BBSRendering.setLensOverscanScale(scale);

        for (ColorEffect effect : ColorClip.getEffects(this.context))
        {
            if (effect.hasCinematic)
            {
                effect.lensOverscan = scale;
            }
        }
    }

    @Override
    public int getPriority()
    {
        return 10;
    }
}
