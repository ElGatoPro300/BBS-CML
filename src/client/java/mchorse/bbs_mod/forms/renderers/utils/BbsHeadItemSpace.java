package mchorse.bbs_mod.forms.renderers.utils;

import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Adapts vanilla spyglass third-person placement
 * ({@code PlayerHeldItemFeatureRenderer.renderSpyglass} + {@code HeadFeatureRenderer.translate}
 * + {@link ModelTransformationMode#HEAD}) to BBS ModelForm head bone matrices.
 * <p>
 * {@code HeadFeatureRenderer.translate} cannot be called as-is: BBS {@code captureMatrices}
 * already bake {@code rotateY(PI)} and the procedural head uses the opposite pitch sign of
 * {@code ModelPart}, so vanilla's {@code T(0, -0.25)} drops the item to the chest.
 * This helper keeps the same stages (look-pitch clamp → hat/eye setup → arm bias → HEAD
 * display, whose {@code [0,0,-16]} translation parks the eyepiece at the eye).
 */
public final class BbsHeadItemSpace
{
    /** {@code PlayerHeldItemFeatureRenderer} HEAD_YAW. */
    public static final float SPYGLASS_LOOK_PITCH_MIN = -30F;
    /** {@code PlayerHeldItemFeatureRenderer} HEAD_ROLL. */
    public static final float SPYGLASS_LOOK_PITCH_MAX = 90F;

    /** Vanilla ±2.5/16 lateral bias by using arm. */
    private static final float ARM_BIAS = 2.5F / 16F;
    /** Neck pivot → eye line (3px − ¼px). */
    private static final float EYE_Y = 2.75F / 16F;
    /** {@code HeadFeatureRenderer.translate} uniform scale. */
    private static final float HAT_SCALE = 0.625F;
    /** Vanilla post-hat Y bias (−1/16). */
    private static final float HAT_Y = -1F / 16F;

    private BbsHeadItemSpace()
    {}

    public static float clampSpyglassLookPitch(float lookPitchDeg)
    {
        return MathHelper.clamp(lookPitchDeg, SPYGLASS_LOOK_PITCH_MIN, SPYGLASS_LOOK_PITCH_MAX);
    }

    /**
     * Call after {@code MatrixStackUtils.multiply(stack, headBoneMatrix)}.
     * Stack is then ready for {@link #spyglassTransformationMode()} with
     * {@link #spyglassLeftHanded()}.
     *
     * @param lookPitchDeg entity look pitch in degrees (positive = look down)
     * @param leftArm whether the active arm is the left (main-arm aware)
     */
    public static void applySpyglass(MatrixStack stack, float lookPitchDeg, boolean leftArm)
    {
        float clamped = clampSpyglassLookPitch(lookPitchDeg);

        /* captureMatrices attachment space: M_want = M * Rx(clamped − actual). */
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clamped - lookPitchDeg));

        /* Neck → eye. Do not use HeadFeatureRenderer's T(0,−0.25) here. */
        stack.translate(0F, EYE_Y, 0F);

        /* Remainder of HeadFeatureRenderer.translate. Y180 is omitted because
         * captureMatrices already baked rotateY(PI) onto the bone matrix — adding
         * another would face the spyglass the wrong way. */
        stack.scale(HAT_SCALE, -HAT_SCALE, -HAT_SCALE);
        /* Arm bias: vanilla uses −2.5/16 for left and +2.5/16 for right before the hat
         * scales; BBS captureMatrices Y180 + negative hat scale flip X, so signs swap. */
        stack.translate(leftArm ? ARM_BIAS : -ARM_BIAS, HAT_Y, 0F);
    }

    /**
     * Vanilla always uses {@link ModelTransformationMode#HEAD} for an active spyglass
     * (display: rotation 90°, translation [0,0,−16], scale 1.6).
     */
    public static ModelTransformationMode spyglassTransformationMode()
    {
        return ModelTransformationMode.HEAD;
    }

    /**
     * Vanilla {@code HeldItemRenderer.renderItem} always passes {@code false} for spyglass HEAD.
     */
    public static boolean spyglassLeftHanded()
    {
        return false;
    }
}
