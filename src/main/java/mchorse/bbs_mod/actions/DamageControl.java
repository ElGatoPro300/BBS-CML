package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.BBSSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class DamageControl
{
    private List<BlockCapture> blocks = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();

    private ServerLevel world;

    public int nested;
    public boolean enable;

    public DamageControl(ServerLevel world)
    {
        this.world = world;
        this.enable = BBSSettings.damageControl.get();
    }

    public void addBlock(BlockPos pos, BlockState state, BlockEntity entity)
    {
        if (!this.enable)
        {
            return;
        }

        for (int i = 0; i < this.blocks.size(); i++)
        {
            BlockCapture blockCapture = this.blocks.get(i);

            if (blockCapture.pos.equals(pos))
            {
                return;
            }
        }

        this.blocks.add(new BlockCapture(new BlockPos(pos), state, entity == null ? null : entity.saveWithFullMetadata(this.world.registryAccess())));
    }

    public void addEntity(Entity entity)
    {
        if (!this.enable)
        {
            return;
        }

        this.entities.add(entity);
    }

    public void restore()
    {
        boolean prev = this.enable;
        this.enable = false;

        List<BlockCapture> blocksCopy = new ArrayList<>(this.blocks);
        List<Entity> entitiesCopy = new ArrayList<>(this.entities);

        this.blocks.clear();
        this.entities.clear();

        for (BlockCapture block : blocksCopy)
        {
            this.world.setBlock(block.pos, block.lastState, 2);

            if (block.blockEntity != null)
            {
                BlockEntity blockEntity = BlockEntity.loadStatic(block.pos, block.lastState, block.blockEntity, this.world.registryAccess());

                this.world.setBlockEntity(blockEntity);
            }
        }

        for (Entity entity : entitiesCopy)
        {
            if (!entity.isRemoved())
            {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        this.enable = prev;
    }

    private static class BlockCapture
    {
        public BlockPos pos;
        public BlockState lastState;
        public CompoundTag blockEntity;

        public BlockCapture(BlockPos pos, BlockState lastState, CompoundTag blockEntity)
        {
            this.pos = pos;
            this.lastState = lastState;
            this.blockEntity = blockEntity;
        }
    }
}
