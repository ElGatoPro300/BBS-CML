package mchorse.bbs_mod.cubic.animation.gecko.validation;

import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoLimbAnimationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeckoAnimationValidator
{
    private static final Set<String> AXES = Set.of("x", "y", "z");

    public GeckoAnimationsConfig sanitize(GeckoAnimationsConfig input)
    {
        GeckoAnimationsConfig config = new GeckoAnimationsConfig();

        if (input == null)
        {
            return config;
        }

        config.copy(input);
        config.transitionSpeed = this.normalizeTransitionSpeed(config.transitionSpeed);
        config.previewWheelSpeed = this.normalizePreviewWheelSpeed(config.previewWheelSpeed);
        config.limbs.entrySet().removeIf((entry) -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isEmpty());

        for (Map.Entry<String, GeckoLimbAnimationConfig> entry : config.limbs.entrySet())
        {
            GeckoLimbAnimationConfig limb = entry.getValue();
            limb.wheelAxis = this.normalizeAxis(limb.wheelAxis);
            limb.wheelSpeed = this.normalizeWheelSpeed(limb.wheelSpeed);
        }

        return config;
    }

    public List<String> validate(GeckoAnimationsConfig config, Set<String> bones, Set<String> animations)
    {
        List<String> errors = new ArrayList<>();

        if (config == null)
        {
            errors.add("Gecko animations config is null");
            return errors;
        }

        Set<String> safeBones = bones == null ? Set.of() : bones;

        if (Float.isNaN(config.transitionSpeed) || Float.isInfinite(config.transitionSpeed) || config.transitionSpeed < 0F || config.transitionSpeed > 1F)
        {
            errors.add("Gecko transition_speed must be between 0 and 1");
        }

        if (Float.isNaN(config.previewWheelSpeed) || Float.isInfinite(config.previewWheelSpeed) || config.previewWheelSpeed < 0F || config.previewWheelSpeed > 8F)
        {
            errors.add("Gecko preview_wheel_speed must be between 0 and 8");
        }

        for (Map.Entry<String, GeckoLimbAnimationConfig> entry : config.limbs.entrySet())
        {
            String limbId = entry.getKey();
            GeckoLimbAnimationConfig limb = entry.getValue();

            if (limbId == null || limbId.isBlank())
            {
                errors.add("Limb id is blank");
                continue;
            }

            if (limb == null)
            {
                errors.add("Limb config is null for " + limbId);
                continue;
            }

            if (!safeBones.isEmpty() && !safeBones.contains(limbId))
            {
                errors.add("Limb id is not present in model: " + limbId);
            }

            if (!AXES.contains(this.rawAxis(limb.wheelAxis)))
            {
                errors.add("Wheel axis is invalid for " + limbId + ": " + limb.wheelAxis);
            }

            if (Float.isNaN(limb.wheelSpeed) || Float.isInfinite(limb.wheelSpeed) || limb.wheelSpeed < 0F || limb.wheelSpeed > 100F)
            {
                errors.add("Wheel speed is invalid for " + limbId + ": " + limb.wheelSpeed);
            }

        }

        return errors;
    }

    private float normalizeWheelSpeed(float wheelSpeed)
    {
        if (Float.isNaN(wheelSpeed) || Float.isInfinite(wheelSpeed))
        {
            return 1F;
        }

        if (wheelSpeed < 0F)
        {
            return 0F;
        }

        return Math.min(100F, wheelSpeed);
    }

    private float normalizeTransitionSpeed(float transitionSpeed)
    {
        if (Float.isNaN(transitionSpeed) || Float.isInfinite(transitionSpeed))
        {
            return 0.35F;
        }

        if (transitionSpeed < 0F)
        {
            return 0F;
        }

        return Math.min(1F, transitionSpeed);
    }

    private float normalizePreviewWheelSpeed(float previewWheelSpeed)
    {
        if (Float.isNaN(previewWheelSpeed) || Float.isInfinite(previewWheelSpeed))
        {
            return 1F;
        }

        if (previewWheelSpeed < 0F)
        {
            return 0F;
        }

        return Math.min(8F, previewWheelSpeed);
    }

    private String normalizeAxis(String axis)
    {
        if (axis == null)
        {
            return "x";
        }

        String normalized = axis.trim().toLowerCase();

        if (!AXES.contains(normalized))
        {
            return "x";
        }

        return normalized;
    }

    private String rawAxis(String axis)
    {
        if (axis == null)
        {
            return "";
        }

        return axis.trim().toLowerCase();
    }
}
