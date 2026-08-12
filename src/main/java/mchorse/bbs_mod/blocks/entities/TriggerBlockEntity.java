package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class TriggerBlockEntity extends BlockEntity
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

    public TriggerBlockEntity(BlockPos pos, BlockState state)
    {
        super(BBSMod.TRIGGER_BLOCK_ENTITY, pos, state);
    }

    public void trigger(ServerPlayer player, boolean rightClick)
    {
        this.trigger(player, rightClick ? this.right.getList() : this.left.getList());
    }

    public void trigger(ServerPlayer player, List<Trigger> triggers)
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
                        player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), cmd);
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
                
                BlockPos pos = new BlockPos(x, y, z);
                
                if (this.level.hasChunkAt(pos))
                {
                    BlockEntity be = this.level.getBlockEntity(pos);
                    
                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.getProperties().setForm(FormUtils.copy(form));
                        modelBlock.setChanged();
                        this.level.sendBlockUpdated(pos, this.level.getBlockState(pos), this.level.getBlockState(pos), 3);
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
    
    public static void tick(Level world, BlockPos pos, BlockState state, TriggerBlockEntity blockEntity)
    {
        if (!world.isClientSide() && blockEntity.region.get())
        {
            blockEntity.tickRegion();
        }

        TriggerBlockEntityUpdateCallback.EVENT.invoker().update(blockEntity);
    }

    public AABB getRegionBox()
    {
        return this.getRegionBox(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
    }

    public AABB getRegionBoxRelative()
    {
        return this.getRegionBox(0, 0, 0);
    }

    public AABB getRegionBox(double x, double y, double z)
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

        return new AABB(
            x + minX, y + minY, z + minZ,
            x + maxX, y + maxY, z + maxZ
        );
    }

    private void tickRegion()
    {
        AABB box = this.getRegionBox();
        List<ServerPlayer> players = this.level.getEntitiesOfClass(ServerPlayer.class, box, (p) -> true);
        Set<UUID> currentPlayers = new HashSet<>();
        long time = this.level.getGameTime();

        for (ServerPlayer player : players)
        {
            UUID uuid = player.getUUID();
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
                ServerPlayer player = (ServerPlayer) this.level.getPlayerByUUID(uuid);

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
    protected void loadAdditional(ValueInput view)
    {
        super.loadAdditional(view);

        CompoundTag nbt = view.read("TriggerData", CompoundTag.CODEC).orElse(new CompoundTag());

        if (nbt.contains("Left")) this.left.fromData(DataStorageUtils.fromNbt(nbt.get("Left")));
        if (nbt.contains("Right")) this.right.fromData(DataStorageUtils.fromNbt(nbt.get("Right")));
        if (nbt.contains("Enter")) this.enter.fromData(DataStorageUtils.fromNbt(nbt.get("Enter")));
        if (nbt.contains("Exit")) this.exit.fromData(DataStorageUtils.fromNbt(nbt.get("Exit")));
        if (nbt.contains("WhileIn")) this.whileIn.fromData(DataStorageUtils.fromNbt(nbt.get("WhileIn")));
        if (nbt.contains("RegionDelay")) this.regionDelay.set(nbt.getInt("RegionDelay").orElse(15));
        if (nbt.contains("Collidable")) this.collidable.set(nbt.getBoolean("Collidable").orElse(false));
        if (nbt.contains("Region")) this.region.set(nbt.getBoolean("Region").orElse(false));
        if (nbt.contains("Pos1")) this.pos1.fromData(DataStorageUtils.fromNbt(nbt.get("Pos1")));
        if (nbt.contains("Pos2")) this.pos2.fromData(DataStorageUtils.fromNbt(nbt.get("Pos2")));
        if (nbt.contains("RegionOffset")) this.regionOffset.fromData(DataStorageUtils.fromNbt(nbt.get("RegionOffset")));
        if (nbt.contains("RegionSize")) this.regionSize.fromData(DataStorageUtils.fromNbt(nbt.get("RegionSize")));
    }

    @Override
    protected void saveAdditional(ValueOutput view)
    {
        super.saveAdditional(view);

        CompoundTag nbt = new CompoundTag();

        DataStorageUtils.writeToNbtCompound(nbt, "Left", this.left.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Right", this.right.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Enter", this.enter.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Exit", this.exit.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "WhileIn", this.whileIn.toData());
        nbt.putInt("RegionDelay", this.regionDelay.get());
        nbt.putBoolean("Collidable", this.collidable.get());
        nbt.putBoolean("Region", this.region.get());
        DataStorageUtils.writeToNbtCompound(nbt, "Pos1", this.pos1.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "Pos2", this.pos2.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "RegionOffset", this.regionOffset.toData());
        DataStorageUtils.writeToNbtCompound(nbt, "RegionSize", this.regionSize.toData());

        view.store("TriggerData", CompoundTag.CODEC, nbt);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup)
    {
        return this.saveWithoutMetadata(registryLookup);
    }
}
