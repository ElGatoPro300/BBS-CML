package mchorse.bbs_mod.cubic.animation.gecko.controllers;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationContext;
import mchorse.bbs_mod.cubic.animation.gecko.services.GeckoAnimationService;
import mchorse.bbs_mod.cubic.animation.gecko.validation.GeckoAnimationValidator;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.forms.entities.IEntity;

import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;

public class GeckoAnimationController
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final GeckoAnimationValidator validator;
    private final GeckoAnimationService service;
    private String lastDecision;

    public GeckoAnimationController(GeckoAnimationService service)
    {
        this(new GeckoAnimationValidator(), service);
    }

    public GeckoAnimationController(GeckoAnimationValidator validator, GeckoAnimationService service)
    {
        this.validator = validator;
        this.service = service;
    }

    public boolean apply(IEntity entity, IModel model, Animations animations, GeckoAnimationsConfig sourceConfig, GeckoAnimationContext context)
    {
        GeckoAnimationsConfig config = this.validator.sanitize(sourceConfig);

        if (entity == null || model == null || config == null)
        {
            this.logDecision("Gecko animation skipped: missing entity/model/config");
            return false;
        }

        if (!config.enabled)
        {
            this.logDecision("Gecko animation skipped: module disabled");
            return false;
        }

        if (config.limbs.isEmpty())
        {
            this.logDecision("Gecko animation skipped: no limb configurations");
            return false;
        }

        Set<String> bones = this.service.knownBones(model);
        Set<String> animationIds = animations == null ? Set.of() : animations.animations.keySet();
        List<String> errors = this.validator.validate(config, bones, animationIds);

        if (!errors.isEmpty())
        {
            this.logDecision("Gecko animation skipped: " + String.join("; ", errors));
            return false;
        }

        this.logDecision("Gecko animation applied: configuredLimbs=" + config.limbs.size());
        this.service.apply(entity, model, config, context);

        return true;
    }

    private void logDecision(String decision)
    {
        if (!Objects.equals(this.lastDecision, decision))
        {
            this.lastDecision = decision;
            LOGGER.debug(decision);
        }
    }
}
