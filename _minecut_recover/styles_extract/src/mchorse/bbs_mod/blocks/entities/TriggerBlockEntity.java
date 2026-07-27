package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.events.TriggerBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.misc.ValueVector3f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.triggers.Trigger;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2596;
import net.minecraft.class_2602;
import net.minecraft.class_2622;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import net.minecraft.class_7225;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class TriggerBlockEntity extends class_2586
{
    public final ValueList<Trigger> left = new ValueList<Trigger>("left")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> right = new ValueList<Trigger>("right")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> enter = new ValueList<Trigger>("enter")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> exit = new ValueList<Trigger>("exit")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> whileIn = new ValueList<Trigger>("whileIn")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueBoolean collidable = new ValueBoolean("collidable", false);
    public final ValueBoolean region = new ValueBoolean("region", false);
    public final ValueInt regionDelay = new ValueInt("regionDelay", 15);
    public final ValueVector3f pos1 = new ValueVector3f("pos1", new Vector3f(0, 0, 0));
    public final ValueVector3f pos2 = new ValueVector3f("pos2", new Vector3f(1, 1, 1));
    public final ValueVector3f regionOffset = new ValueVector3f("regionOffset", new Vector3f(0, 0, 0));
    public final ValueVector3f regionSize = new ValueVector3f("regionSize", new Vector3f(1, 1, 1));

    private Set<UUID> playersInRegion = new HashSet<>();
    private Map<UUID, Long> regionNextTriggerTick = new HashMap<>();

    public TriggerBlockEntity(class_2338 pos, class_2680 state)
    {
        super(BBSMod.TRIGGER_BLOCK_ENTITY, pos, state);
    }

    public void trigger(class_3222 player, boolean rightClick)
    {
        this.trigger(player, rightClick ? this.right.getList() : this.left.getList());
    }

    public void trigger(class_3222 player, List<Trigger> triggers)
    {
        for (Trigger trigger : triggers)
        {
            String type = trigger.type.get();
            
            if (type.equals("command"))
            {
                String cmd = trigger.command.get();
                
                if (!cmd.isEmpty())
                {
                    try
                    {
                        player.method_5682().method_3734().method_44252(player.method_5671().method_9206(2), cmd);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            else if (type.equals("form"))
            {
                Form form = trigger.form.get();

                ServerNetwork.sendMorphToTracked(player, form);
                Morph.getMorph(player).setForm(FormUtils.copy(form));
            }
            else if (type.equals("block"))
            {
                int x = trigger.x.get();
                int y = trigger.y.get();
                int z = trigger.z.get();
                Form form = trigger.blockForm.get();
                
                class_2338 pos = new class_2338(x, y, z);
                
                if (this.field_11863.method_22340(pos))
                {
                    class_2586 be = this.field_11863.method_8321(pos);
                    
                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.getProperties().setForm(FormUtils.copy(form));
                        modelBlock.method_5431();
                        this.field_11863.method_8413(pos, this.field_11863.method_8320(pos), this.field_11863.method_8320(pos), 3);
                    }
                }
            }
            else if (type.equals("film"))
            {
                String filmName = trigger.film.get();
                boolean playCamera = trigger.playCamera.get();
                
                if (!filmName.isEmpty())
                {
                    ServerNetwork.sendPlayFilm(player, filmName, playCamera);
                }
            }
        }
    }
    
    public static void tick(class_1937 world, class_2338 pos, class_2680 state, TriggerBlockEntity blockEntity)
    {
        if (!world.field_9236 && blockEntity.region.get())
        {
            blockEntity.tickRegion();
        }

        TriggerBlockEntityUpdateCallback.EVENT.invoker().update(blockEntity);
    }

    public class_238 getRegionBox()
    {
        return this.getRegionBox(this.field_11867.method_10263(), this.field_11867.method_10264(), this.field_11867.method_10260());
    }

    public class_238 getRegionBoxRelative()
    {
        return this.getRegionBox(0, 0, 0);
    }

    public class_238 getRegionBox(double x, double y, double z)
    {
        Vector3f offset = this.regionOffset.get();
        Vector3f size = this.regionSize.get();

        /* Slightly expand the region box so it's bigger than the 1x1 hitbox by default */
        double expansion = 1.0;
        double minX = offset.x + 0.5 - size.x / 2.0 - expansion;
        double minY = offset.y + 0.5 - size.y / 2.0 - expansion;
        double minZ = offset.z + 0.5 - size.z / 2.0 - expansion;
        double maxX = offset.x + 0.5 + size.x / 2.0 + expansion;
        double maxY = offset.y + 0.5 + size.y / 2.0 + expansion;
        double maxZ = offset.z + 0.5 + size.z / 2.0 + expansion;

        return new class_238(
            x + minX, y + minY, z + minZ,
            x + maxX, y + maxY, z + maxZ
        );
    }

    private void tickRegion()
    {
        class_238 box = this.getRegionBox();
        List<class_3222> players = this.field_11863.method_8390(class_3222.class, box, (p) -> true);
        Set<UUID> currentPlayers = new HashSet<>();
        long time = this.field_11863.method_8510();

        for (class_3222 player : players)
        {
            UUID uuid = player.method_5667();
            currentPlayers.add(uuid);

            boolean isNew = !this.playersInRegion.contains(uuid);
            long nextTick = this.regionNextTriggerTick.getOrDefault(uuid, 0L);

            if (isNew)
            {
                this.trigger(player, this.enter.getList());
                this.regionNextTriggerTick.put(uuid, time + this.regionDelay.get());
            }
            else if (time >= nextTick)
            {
                this.trigger(player, this.whileIn.getList());
                this.regionNextTriggerTick.put(uuid, time + this.regionDelay.get());
            }
        }

        for (UUID uuid : this.playersInRegion)
        {
            if (!currentPlayers.contains(uuid))
            {
                class_3222 player = (class_3222) this.field_11863.method_18470(uuid);

                if (player != null)
                {
                    this.trigger(player, this.exit.getList());
                }
                
                this.regionNextTriggerTick.remove(uuid);
            }
        }

        this.playersInRegion = currentPlayers;
    }

    @Override
    public void method_11014(class_2487 nbt, class_7225.class_7874 registryLookup)
    {
        super.method_11014(nbt, registryLookup);
        
        if (nbt.method_10545("Left")) this.left.fromData(DataStorageUtils.fromNbt(nbt.method_10580("Left")));
        if (nbt.method_10545("Right")) this.right.fromData(DataStorageUtils.fromNbt(nbt.method_10580("Right")));
        if (nbt.method_10545("Enter")) this.enter.fromData(DataStorageUtils.fromNbt(nbt.method_10580("Enter")));
        if (nbt.method_10545("Exit")) this.exit.fromData(DataStorageUtils.fromNbt(nbt.method_10580("Exit")));
        if (nbt.method_10545("WhileIn")) this.whileIn.fromData(DataStorageUtils.fromNbt(nbt.method_10580("WhileIn")));
        if (nbt.method_10545("RegionDelay")) this.regionDelay.set(nbt.method_10550("RegionDelay"));
        if (nbt.method_10545("Collidable")) this.collidable.set(nbt.method_10577("Collidable"));
        if (nbt.method_10545("Region")) this.region.set(nbt.method_10577("Region"));
        if (nbt.method_10545("Pos1")) this.pos1.fromData(DataStorageUtils.fromNbt(nbt.method_10580("Pos1")));
        if (nbt.method_10545("Pos2")) this.pos2.fromData(DataStorageUtils.fromNbt(nbt.method_10580("Pos2")));
        if (nbt.method_10545("RegionOffset")) this.regionOffset.fromData(DataStorageUtils.fromNbt(nbt.method_10580("RegionOffset")));
        if (nbt.method_10545("RegionSize")) this.regionSize.fromData(DataStorageUtils.fromNbt(nbt.method_10580("RegionSize")));
    }

    @Override
    public void method_11007(class_2487 nbt, class_7225.class_7874 registryLookup)
    {
        super.method_11007(nbt, registryLookup);

        /* Route every value through the null-safe helper: ValueList.toData() returns
         * null when the list is empty (the default state of a freshly placed block),
         * and NbtCompound.put with a null element corrupts the chunk save. */
        DataStorageUtils.writeToNbtCompound(nbt, "Left", this.left.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Right", this.right.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Enter", this.enter.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Exit", this.exit.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "WhileIn", this.whileIn.toData());
        nbt.method_10569("RegionDelay", this.regionDelay.get());
        nbt.method_10556("Collidable", this.collidable.get());
        nbt.method_10556("Region", this.region.get());
        DataStorageUtils.writeToNbtCompound(nbt, "Pos1", this.pos1.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Pos2", this.pos2.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "RegionOffset", this.regionOffset.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "RegionSize", this.regionSize.toData());
    }

    @Nullable
    @Override
    public class_2596<class_2602> method_38235()
    {
        return class_2622.method_38585(this);
    }

    @Override
    public class_2487 method_16887(class_7225.class_7874 registryLookup)
    {
        return this.method_38243(registryLookup);
    }
}
