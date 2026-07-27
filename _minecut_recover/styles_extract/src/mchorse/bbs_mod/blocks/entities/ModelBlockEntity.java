package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.events.ModelBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2596;
import net.minecraft.class_2602;
import net.minecraft.class_2622;
import net.minecraft.class_2680;
import net.minecraft.class_7225.class_7874;
import org.jetbrains.annotations.Nullable;

public class ModelBlockEntity extends class_2586
{
    private ModelProperties properties = new ModelProperties();
    private IEntity entity = new StubEntity();

    private float lastYaw = Float.NaN;
    private float currentYaw = Float.NaN;
    private int lastLightLevel = -1;

    public ModelBlockEntity(class_2338 pos, class_2680 state)
    {
        super(BBSMod.MODEL_BLOCK_ENTITY, pos, state);
    }

    public String getName()
    {
        class_2338 pos = this.method_11016();
        Form form = this.getProperties().getForm();
        String s = "(" + pos.method_10263() + ", " + pos.method_10264() + ", " + pos.method_10260() + ")";
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

    public static void tick(class_1937 world, class_2338 pos, class_2680 state, ModelBlockEntity blockEntity)
    {
        ModelBlockEntityUpdateCallback.EVENT.invoker().update(blockEntity);
        /* Asegura que el StubEntity tenga posición y mundo correctos para cálculos de luz/bioma.
         * Sin esto, el entity se queda en (0,0,0) y los renders toman luz de esa zona,
         * provocando oscurecimiento en editor, miniatura y bloque de modelo. */
        blockEntity.entity.setWorld(world);

        double x = pos.method_10263() + 0.5D;
        double y = pos.method_10264();
        double z = pos.method_10260() + 0.5D;

        blockEntity.entity.setPosition(x, y, z);

        mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions.updateRegistration(blockEntity);

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
        if (!world.field_9236)
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
                    world.method_8652(pos, state.method_11657(ModelBlock.LIGHT_LEVEL, target), class_2248.field_31028);
                }
                catch (Exception e) {}
            }
        }
    }

    @Nullable
    @Override
    public class_2596<class_2602> method_38235()
    {
        return class_2622.method_38585(this);
    }

    @Override
    public class_2487 method_16887(class_7874 registryLookup)
    {
        return this.method_38243(registryLookup);
    }

    @Override
    protected void method_11007(class_2487 nbt, class_7874 registryLookup)
    {
        super.method_11007(nbt, registryLookup);

        MapType data = this.properties.toData();

        DataStorageUtils.writeToNbtCompound(nbt, "Properties", data);
    }

    @Override
    public void method_11014(class_2487 nbt, class_7874 registryLookup)
    {
        super.method_11014(nbt, registryLookup);

        BaseType baseType = DataStorageUtils.readFromNbtCompound(nbt, "Properties");

        if (baseType instanceof MapType mapType)
        {
            this.properties.fromData(mapType);
        }
        /* Ensure block state reflects stored light level when chunk/block is loaded */
        if (this.field_11863 != null && !this.field_11863.field_9236)
        {
            try
            {
                int level = this.properties.getLightLevel();
                class_2338 pos = this.method_11016();
                class_2680 state = this.field_11863.method_8320(pos);

                if (state.method_26204() instanceof class_2248)
                {
                    this.field_11863.method_8652(pos, state.method_11657(ModelBlock.LIGHT_LEVEL, level), class_2248.field_31028);
                }
            }
            catch (Exception e) {}
        }
    }

    public void updateForm(MapType data, class_1937 world)
    {
        this.properties.fromData(data);

        class_2338 pos = this.method_11016();
        class_2680 blockState = world.method_8320(pos);
        int level = this.properties.getLightLevel();
        class_2680 newState = blockState.method_11657(ModelBlock.LIGHT_LEVEL, level);

        world.method_8524(pos);
        mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions.updateRegistration(this);

        if (blockState != newState)
        {
            world.method_8652(pos, newState, class_2248.field_31028);
        }
        else
        {
            world.method_8413(pos, blockState, newState, class_2248.field_31028);
        }
    }

    @Override
    public void method_11012()
    {
        mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions.unregister(this);
        super.method_11012();
    }
}
