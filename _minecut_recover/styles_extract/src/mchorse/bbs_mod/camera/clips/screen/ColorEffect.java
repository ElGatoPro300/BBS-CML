package mchorse.bbs_mod.camera.clips.screen;

public class ColorEffect
{
    public boolean hasOverlay;
    public int overlayColor;

    public boolean hasVignette;
    public int vignetteColor;
    public float vignetteStrength;
    public float vignetteSmoothness;

    public boolean hasDistort;
    public float distortX;
    public float distortY;

    public boolean hasGrade;
    public float brightness;
    public float contrast;
    public float saturation;
    public float hue;
    public float liftR, liftG, liftB;
    public float gammaR, gammaG, gammaB;
    public float gainR, gainG, gainB;

    public boolean hasCinematic;
    public float aberration;
    public float vhs;
    public float lensDistortion;
    /** >1 when positive fisheye widened the render FOV to match the UV warp. */
    public float lensOverscan;
    public float vintage;
    public float radialBlur;
    public float rain;
    public float dust;
    public float lightLeak;
    public float heatStrength;
    public float heatSpeed;
    public float heatScale;
    public float time;

    public void reset()
    {
        this.hasOverlay = false;
        this.hasVignette = false;
        this.hasGrade = false;
        this.hasDistort = false;
        this.hasCinematic = false;

        this.aberration = 0F;
        this.vhs = 0F;
        this.lensDistortion = 0F;
        this.lensOverscan = 1F;
        this.vintage = 0F;
        this.radialBlur = 0F;
        this.rain = 0F;
        this.dust = 0F;
        this.lightLeak = 0F;
        this.heatStrength = 0F;
        this.heatSpeed = 0F;
        this.heatScale = 0F;
        this.time = 0F;
    }
}
