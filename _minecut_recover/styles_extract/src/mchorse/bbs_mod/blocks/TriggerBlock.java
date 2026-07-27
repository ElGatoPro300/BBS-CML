package mchorse.bbs_mod.blocks;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.class_1269;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1922;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2343;
import net.minecraft.class_2464;
import net.minecraft.class_2586;
import net.minecraft.class_259;
import net.minecraft.class_2591;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import net.minecraft.class_3726;
import net.minecraft.class_3965;
import net.minecraft.class_4538;
import net.minecraft.class_5558;
import net.minecraft.class_9279;
import net.minecraft.class_9334;
import org.joml.Vector3f;

import org.jetbrains.annotations.Nullable;

public class TriggerBlock extends class_2248 implements class_2343
{
    public TriggerBlock(class_2251 settings)
    {
        super(settings);
    }

    @Override
    public class_1799 method_9574(class_4538 world, class_2338 pos, class_2680 state)
    {
        class_2586 entity = world.method_8321(pos);

        if (entity instanceof TriggerBlockEntity triggerBlock)
        {
            class_1799 stack = new class_1799(this);
            stack.method_57379(class_9334.field_49611, class_9279.method_57456(triggerBlock.method_38243(world.method_30349())));

            return stack;
        }

        return super.method_9574(world, pos, state);
    }

    @Override
    public class_2464 method_9604(class_2680 state)
    {
        return class_2464.field_11455;
    }

    @Override
    public float method_9575(class_2680 state, class_1922 world, class_2338 pos)
    {
        return 1.0F;
    }

    @Override
    public boolean method_9579(class_2680 state, class_1922 world, class_2338 pos)
    {
        return true;
    }

    @Nullable
    @Override
    public class_2586 method_10123(class_2338 pos, class_2680 state)
    {
        return new TriggerBlockEntity(pos, state);
    }

    @Override
    public void method_9606(class_2680 state, class_1937 world, class_2338 pos, class_1657 player)
    {
        if (!world.field_9236 && player instanceof class_3222 serverPlayer && !player.method_7337())
        {
            class_2586 be = world.method_8321(pos);

            if (be instanceof TriggerBlockEntity triggerBlock)
            {
                triggerBlock.trigger(serverPlayer, false);
            }
        }

        super.method_9606(state, world, pos, player);
    }

    @Override
    public class_1269 method_55766(class_2680 state, class_1937 world, class_2338 pos, class_1657 player, class_3965 hit)
    {
        if (player.method_6047().method_7960())
        {
            if (!world.field_9236 && player instanceof class_3222 serverPlayer)
            {
                if (!player.method_7337() || (player.method_7337() && player.method_5715()))
                {
                    class_2586 be = world.method_8321(pos);

                    if (be instanceof TriggerBlockEntity triggerBlock)
                    {
                        triggerBlock.trigger(serverPlayer, true);

                        return class_1269.field_5812;
                    }
                }
                else
                {
                    ServerNetwork.sendClickedTriggerBlock(serverPlayer, pos);

                    return class_1269.field_5812;
                }
            }

            return class_1269.field_5812;
        }

        return super.method_55766(state, world, pos, player, hit);
    }

    @Override
    public class_265 method_9530(class_2680 state, class_1922 world, class_2338 pos, class_3726 context)
    {
        return this.getShape(world, pos);
    }

    @Override
    public class_265 method_9549(class_2680 state, class_1922 world, class_2338 pos, class_3726 context)
    {
        try
        {
            class_2586 be = world.method_8321(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                if (!trigger.collidable.get())
                {
                    return class_259.method_1073();
                }

                return this.getShape(world, pos);
            }
        }
        catch (Exception e)
        {
        }

        /* Never fall back to a solid full cube for a non-solid block: an exception or
         * a missing block entity here must not eject the player through the world. */
        return class_259.method_1073();
    }

    private class_265 getShape(class_1922 world, class_2338 pos)
    {
        try
        {
            class_2586 be = world.method_8321(pos);

            if (be instanceof TriggerBlockEntity trigger)
            {
                Vector3f min = trigger.pos1.get();
                Vector3f max = trigger.pos2.get();

                if (min == null || max == null)
                {
                    return class_259.method_1073();
                }

                double minX = Math.max(0D, Math.min(min.x, max.x));
                double minY = Math.max(0D, Math.min(min.y, max.y));
                double minZ = Math.max(0D, Math.min(min.z, max.z));
                double maxX = Math.min(1D, Math.max(min.x, max.x));
                double maxY = Math.min(1D, Math.max(min.y, max.y));
                double maxZ = Math.min(1D, Math.max(min.z, max.z));

                if (minX < maxX && minY < maxY && minZ < maxZ)
                {
                    return class_259.method_1081(minX, minY, minZ, maxX, maxY, maxZ);
                }
            }
        }
        catch (Exception e)
        {
        }

        return class_259.method_1073();
    }

    @Nullable
    @Override
    public <T extends class_2586> class_5558<T> method_31645(class_1937 world, class_2680 state, class_2591<T> type)
    {
        return type == BBSMod.TRIGGER_BLOCK_ENTITY ? (class_5558<T>) (class_5558<TriggerBlockEntity>) (w, p, s, e) -> TriggerBlockEntity.tick(w, p, s, e) : null;
    }
}
