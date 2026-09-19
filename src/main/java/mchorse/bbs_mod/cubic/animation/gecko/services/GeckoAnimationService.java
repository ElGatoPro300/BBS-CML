package mchorse.bbs_mod.cubic.animation.gecko.services;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationContext;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationState;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoStateBlend;
import mchorse.bbs_mod.cubic.animation.gecko.routes.GeckoAnimationRouteRegistry;
import mchorse.bbs_mod.cubic.animation.gecko.routes.GeckoLimbRole;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.entities.IEntity;

import net.minecraft.util.math.MathHelper;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class GeckoAnimationService
{
    private final GeckoAnimationRouteRegistry routes;
    private final GeckoModelLimbService modelService;
    private final GeckoBOBJLimbService bobjService;
    private final GeckoAnimationBlendService blendService;
    private final GeckoAnimationEventBus eventBus;
    private final WeakHashMap<IEntity, GeckoStateBlend> blends = new WeakHashMap<>();
    private final WeakHashMap<IEntity, EnumMap<GeckoAnimationState, Boolean>> activeStates = new WeakHashMap<>();

    public GeckoAnimationService(GeckoAnimationRouteRegistry routes, GeckoModelLimbService modelService, GeckoBOBJLimbService bobjService, GeckoAnimationBlendService blendService, GeckoAnimationEventBus eventBus)
    {
        this.routes = routes;
        this.modelService = modelService;
        this.bobjService = bobjService;
        this.blendService = blendService;
        this.eventBus = eventBus;
    }

    public void apply(IEntity entity, IModel model, GeckoAnimationsConfig config, GeckoAnimationContext context)
    {
        GeckoStateBlend blend = this.blends.computeIfAbsent(entity, key -> new GeckoStateBlend());
        Map<GeckoAnimationState, Float> targets = this.blendService.resolveTargets(context);
        float factor = MathHelper.clamp(config.transitionSpeed, 0F, 1F);
        blend.blendTo(targets, factor);
        Map<GeckoAnimationState, Float> weights = blend.snapshot();

        this.emitEvents(entity, config, weights);

        for (ModelGroup group : model.getAllGroups())
        {
            GeckoLimbAnimationConfig limb = config.limbs.get(group.id);

            if (limb == null)
            {
                continue;
            }

            GeckoLimbRole role = this.routes.resolve(group.id);

            this.modelService.apply(group, role, limb, context, weights);
        }

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            GeckoLimbAnimationConfig limb = config.limbs.get(bone.name);

            if (limb == null)
            {
                continue;
            }

            GeckoLimbRole role = this.routes.resolve(bone.name);

            this.bobjService.apply(bone, role, limb, context, weights);
        }
    }

    public Set<String> knownBones(IModel model)
    {
        HashSet<String> set = new HashSet<>();

        for (ModelGroup group : model.getAllGroups())
        {
            set.add(group.id);
        }

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            set.add(bone.name);
        }

        return set;
    }

    private void emitEvents(IEntity entity, GeckoAnimationsConfig config, Map<GeckoAnimationState, Float> weights)
    {
        if (!config.emitEvents)
        {
            return;
        }

        EnumMap<GeckoAnimationState, Boolean> active = this.activeStates.computeIfAbsent(entity, key -> new EnumMap<>(GeckoAnimationState.class));

        for (GeckoAnimationState state : GeckoAnimationState.values())
        {
            boolean now = weights.getOrDefault(state, 0F) > 0.5F;
            boolean before = active.getOrDefault(state, false);

            if (now != before)
            {
                String animation = config.stateAnimations.getOrDefault(state.id, state.id);
                this.eventBus.emit(entity, state, animation, now);
                active.put(state, now);
            }
        }
    }
}
