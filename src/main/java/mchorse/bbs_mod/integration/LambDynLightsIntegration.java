package mchorse.bbs_mod.integration;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;

import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;

public class LambDynLightsIntegration implements DynamicLightsInitializer
{
    @Override
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager)
    {
        DynamicLightHandlers.registerDynamicLightHandler(BBSMod.ACTOR_ENTITY, (ActorEntity entity) -> getLightLevelFromForm(entity.getForm()));
        DynamicLightHandlers.registerDynamicLightHandler(BBSMod.GUN_PROJECTILE_ENTITY, (GunProjectileEntity entity) -> getLightLevelFromForm(entity.getForm()));
        DynamicLightHandlers.registerDynamicLightHandler(EntityType.PLAYER, (PlayerEntity player) ->
        {
            Morph morph = Morph.getMorph(player);

            if (morph == null)
            {
                return 0;
            }

            return getLightLevelFromForm(morph.getForm());
        });
    }

    private static int getLightLevelFromForm(Form form)
    {
        if (!(form instanceof LightForm lightForm))
        {
            return 0;
        }

        if (!lightForm.enabled.get())
        {
            return 0;
        }

        int level = lightForm.level.get();

        if (level < 0)
        {
            level = 0;
        }
        else if (level > 15)
        {
            level = 15;
        }

        return level;
    }
}

