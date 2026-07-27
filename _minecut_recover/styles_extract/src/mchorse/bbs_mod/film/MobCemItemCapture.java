package mchorse.bbs_mod.film;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import net.minecraft.class_1268;

public class MobCemItemCapture
{
    private MobCemItemCapture()
    {}

    public static boolean isActive(Replay replay)
    {
        return MobCemPoseCapture.isMobPlaybackActive(replay);
    }

    public static void recordItemStats(Replay replay, MobForm mobForm, IEntity entity, int tick, float transition)
    {
        if (!isActive(replay))
        {
            return;
        }

        MobForm form = resolveMobForm(mobForm, entity);
        MobItemStats stats = sampleItemStats(form, entity, transition);

        recordItemStats(replay, (float) tick, stats);
    }

    public static MobItemStats sampleItemStats(MobForm mobForm, IEntity entity, float transition)
    {
        MobFormRenderer renderer = (MobFormRenderer) FormUtilsClient.getRenderer(mobForm);

        renderer.ensureRenderEntity();

        return renderer.sampleItemStats(entity, transition);
    }

    private static MobForm resolveMobForm(MobForm mobForm, IEntity entity)
    {
        if (entity != null && entity.getForm() instanceof MobForm formFromEntity)
        {
            return formFromEntity;
        }

        return mobForm;
    }

    private static void recordItemStats(Replay replay, float tick, MobItemStats stats)
    {
        BaseValue.edit(replay.keyframes, (keyframes) ->
        {
            keyframes.usingItem.insert(tick, stats.usingItem ? 1D : 0D);
            keyframes.itemUseTime.insert(tick, (double) stats.itemUseElapsed);
            keyframes.activeHand.insert(tick, stats.activeHand == class_1268.field_5810 ? 1D : 0D);
            keyframes.mainHand.insert(tick, stats.mainHand.method_7972());
            keyframes.offHand.insert(tick, stats.offHand.method_7972());
        });
    }
}
