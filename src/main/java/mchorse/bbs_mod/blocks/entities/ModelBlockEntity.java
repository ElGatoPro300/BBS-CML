package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.events.ModelBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.LightForm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

public class ModelBlockEntity extends BlockEntity
{
    private ModelProperties properties = new ModelProperties();
    private IEntity entity = new StubEntity();

    private float lastYaw = Float.NaN;
    private float currentYaw = Float.NaN;
    private int lastLightLevel = -1;

    public ModelBlockEntity(BlockPos pos, BlockState state)
    {
        super(BBSMod.MODEL_BLOCK_ENTITY, pos, state);
    }

    public String getName()
    {
        BlockPos pos = this.getBlockPos();
        Form form = this.getProperties().getForm();
        String s = "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
        String customName = this.getProperties().getName();

        if (!customName.isEmpty())
        {
            return s + " " + customName;
        }

        if (form != null)
        {
            s += " " + form.getDisplayName();
        }

        return s;
    }

    public ModelProperties getProperties()
    {
        return this.properties;
    }

    public IEntity getEntity()
    {
        return this.entity;
    }

    public void setLookYaw(float yaw)
    {
        this.lastYaw = yaw;
        this.currentYaw = yaw;
    }

    public float updateLookYawContinuous(float yaw)
    {
        if (Float.isNaN(this.currentYaw))
        {
            this.setLookYaw(yaw);

            return this.currentYaw;
        }

        float diff = yaw - this.lastYaw;

        while (diff > Math.PI) diff -= (float) (Math.PI * 2);
        while (diff < -Math.PI) diff += (float) (Math.PI * 2);

        this.currentYaw += diff;
        this.lastYaw = yaw;

        return this.currentYaw;
    }

    public void resetLookYaw()
    {
        this.lastYaw = this.currentYaw = Float.NaN;
    }

    public void snapLookYawToBase(float lastYaw, float currentYaw)
    {
        this.lastYaw = lastYaw;
        this.currentYaw = currentYaw;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ModelBlockEntity blockEntity)
    {
        ModelBlockEntityUpdateCallback.EVENT.invoker().update(blockEntity);
        /* Asegura que el StubEntity tenga posición y mundo correctos para cálculos de luz/bioma.
         * Sin esto, el entity se queda en (0,0,0) y los renders toman luz de esa zona,
         * provocando oscurecimiento en editor, miniatura y bloque de modelo. */
        blockEntity.entity.setWorld(world);

        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;

        blockEntity.entity.setPosition(x, y, z);

        /* Initialize previous position/yaw on the very first tick to avoid
         * a huge movement delta (spike) when the block is placed. */
        try
        {
            if (blockEntity.entity.getAge() == 0)
            {
                blockEntity.entity.setPrevX(x);
                blockEntity.entity.setPrevY(y);
                blockEntity.entity.setPrevZ(z);

                blockEntity.entity.setPrevYaw(blockEntity.entity.getYaw());
                blockEntity.entity.setPrevHeadYaw(blockEntity.entity.getHeadYaw());
                blockEntity.entity.setPrevPitch(blockEntity.entity.getPitch());
                blockEntity.entity.setPrevBodyYaw(blockEntity.entity.getBodyYaw());
                blockEntity.entity.setPrevPrevBodyYaw(blockEntity.entity.getPrevBodyYaw());

                float[] extra = blockEntity.entity.getExtraVariables();
                float[] prevExtra = blockEntity.entity.getPrevExtraVariables();

                if (extra != null && prevExtra != null)
                {
                    for (int i = 0; i < Math.min(extra.length, prevExtra.length); i++)
                    {
                        prevExtra[i] = extra[i];
                    }
                }
            }
        }
        catch (Exception e) {}

        blockEntity.entity.update();
        blockEntity.properties.update(blockEntity.entity);
        if (!world.isClientSide())
        {
            int target = blockEntity.properties.getLightLevel();
            Form form = blockEntity.properties.getForm();

            if (form instanceof LightForm lightForm && lightForm.enabled.get())
            {
                int level = lightForm.level.get();

                if (level < 0)
                {
                    level = 0;
                }
                else if (level > 15)
                {
                    level = 15;
                }

                target = level;
            }

            if (target != blockEntity.lastLightLevel)
            {
                blockEntity.lastLightLevel = target;
                blockEntity.properties.setLightLevel(target);

                try
                {
                    world.setBlock(pos, state.setValue(ModelBlock.LIGHT_LEVEL, target), Block.UPDATE_CLIENTS);
                }
                catch (Exception e) {}
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(Provider registryLookup)
    {
        return this.saveWithoutMetadata(registryLookup);
    }

    @Override
    protected void saveAdditional(ValueOutput view)
    {
        super.saveAdditional(view);

        MapType data = this.properties.toData();
        CompoundTag nbt = new CompoundTag();

        DataStorageUtils.writeToNbtCompound(nbt, "Properties", data);

        view.store("Properties", CompoundTag.CODEC, nbt.getCompoundOrEmpty("Properties"));
    }

    @Override
    protected void loadAdditional(ValueInput view)
    {
        super.loadAdditional(view);

        CompoundTag nbt = new CompoundTag();

        view.read("Properties", CompoundTag.CODEC).ifPresent((compound) -> nbt.put("Properties", compound));

        BaseType baseType = DataStorageUtils.readFromNbtCompound(nbt, "Properties");

        if (baseType instanceof MapType mapType)
        {
            this.properties.fromData(mapType);
        }
        /* Ensure block state reflects stored light level when chunk/block is loaded */
        if (this.level != null && !this.level.isClientSide())
        {
            try
            {
                int level = this.properties.getLightLevel();
                BlockPos pos = this.getBlockPos();
                BlockState state = this.level.getBlockState(pos);

                if (state.getBlock() instanceof Block)
                {
                    this.level.setBlock(pos, state.setValue(ModelBlock.LIGHT_LEVEL, level), Block.UPDATE_CLIENTS);
                }
            }
            catch (Exception e) {}
        }
    }

    public void updateForm(MapType data, Level world)
    {
        this.properties.fromData(data);

        BlockPos pos = this.getBlockPos();
        BlockState blockState = world.getBlockState(pos);
        int level = this.properties.getLightLevel();
        BlockState newState = blockState.setValue(ModelBlock.LIGHT_LEVEL, level);

        world.blockEntityChanged(pos);

        if (blockState != newState)
        {
            world.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
        else
        {
            world.sendBlockUpdated(pos, blockState, newState, Block.UPDATE_CLIENTS);
        }
    }
}
