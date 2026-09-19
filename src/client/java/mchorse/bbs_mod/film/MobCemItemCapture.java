package mchorse.bbs_mod.film;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.WeakHashMap;

public class MobCemItemCapture
{
    private static final Map<Replay, MobItemStats> lastRecordedStats = new WeakHashMap<>();

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
        MobItemStats last = lastRecordedStats.get(replay);

        if (last != null && itemStatsEqual(last, stats))
        {
            return;
        }

        BaseValue.edit(replay.keyframes, (keyframes) ->
        {
            keyframes.usingItem.insertIfChanged(tick, stats.usingItem ? 1D : 0D);
            keyframes.itemUseTime.insertIfChanged(tick, (double) stats.itemUseElapsed);
            keyframes.activeHand.insertIfChanged(tick, stats.activeHand == Hand.OFF_HAND ? 1D : 0D);
            keyframes.mainHand.insertIfChanged(tick, stats.mainHand.copy());
            keyframes.offHand.insertIfChanged(tick, stats.offHand.copy());
        });

        lastRecordedStats.put(replay, copyStats(stats));
    }

    private static boolean itemStatsEqual(MobItemStats a, MobItemStats b)
    {
        return a.usingItem == b.usingItem
            && a.itemUseElapsed == b.itemUseElapsed
            && a.activeHand == b.activeHand
            && ItemStack.areEqual(a.mainHand, b.mainHand)
            && ItemStack.areEqual(a.offHand, b.offHand);
    }

    private static MobItemStats copyStats(MobItemStats stats)
    {
        MobItemStats copy = new MobItemStats();

        copy.usingItem = stats.usingItem;
        copy.itemUseElapsed = stats.itemUseElapsed;
        copy.activeHand = stats.activeHand;
        copy.mainHand = stats.mainHand.copy();
        copy.offHand = stats.offHand.copy();

        return copy;
    }
}
