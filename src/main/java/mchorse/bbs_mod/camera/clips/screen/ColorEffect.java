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
    public float aberrationAngle;
    public float aberrationDirectional;
    public float aberrationRadius;
    public float aberrationHardness;
    public float aberrationBalance;
    public float aberrationCenterX;
    public float aberrationCenterY;
    public float aberrationGreen;
    public float aberrationSpectrum;
    public float vhs;
    public float lensDistortion;
    /** FOV match scale: {@code >1} widen (positive fisheye), {@code <1} narrow (negative). */
    public float lensOverscan;
    /** Fisheye effect radius on UV X ({@code 1} = legacy coverage to the corners). */
    public float lensRadiusX;
    /** Fisheye effect radius on UV Y ({@code 1} = legacy coverage to the corners). */
    public float lensRadiusY;
    /** Fisheye mask hardness (1 = hard/legacy, 0 = soft extended falloff). */
    public float lensHardness;
    /** Center unsharp amount for positive fisheye (1 = default full sharpen). */
    public float lensSharpen;
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
        this.aberrationAngle = 0F;
        this.aberrationDirectional = 0F;
        this.aberrationRadius = 1F;
        this.aberrationHardness = 1F;
        this.aberrationBalance = 0F;
        this.aberrationCenterX = 0.5F;
        this.aberrationCenterY = 0.5F;
        this.aberrationGreen = 0F;
        this.aberrationSpectrum = 0F;
        this.vhs = 0F;
        this.lensDistortion = 0F;
        this.lensOverscan = 1F;
        this.lensRadiusX = 1F;
        this.lensRadiusY = 1F;
        this.lensHardness = 1F;
        this.lensSharpen = 0F;
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
