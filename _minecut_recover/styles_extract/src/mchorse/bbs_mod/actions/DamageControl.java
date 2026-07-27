package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.BBSSettings;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_2487;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import java.util.ArrayList;
import java.util.List;

public class DamageControl
{
    private List<BlockCapture> blocks = new ArrayList<>();
    private List<class_1297> entities = new ArrayList<>();

    private class_3218 world;

    public int nested;
    public boolean enable;

    public DamageControl(class_3218 world)
    {
        this.world = world;
        this.enable = BBSSettings.damageControl.get();
    }

    public void addBlock(class_2338 pos, class_2680 state, class_2586 entity)
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

        this.blocks.add(new BlockCapture(new class_2338(pos), state, entity == null ? null : entity.method_38243(this.world.method_30349())));
    }

    public void addEntity(class_1297 entity)
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
        List<class_1297> entitiesCopy = new ArrayList<>(this.entities);

        this.blocks.clear();
        this.entities.clear();

        for (BlockCapture block : blocksCopy)
        {
            this.world.method_8652(block.pos, block.lastState, 2);

            if (block.blockEntity != null)
            {
                class_2586 blockEntity = class_2586.method_11005(block.pos, block.lastState, block.blockEntity, this.world.method_30349());

                this.world.method_8438(blockEntity);
            }
        }

        for (class_1297 entity : entitiesCopy)
        {
            if (!entity.method_31481())
            {
                entity.method_5650(class_1297.class_5529.field_26999);
            }
        }

        this.enable = prev;
    }

    private static class BlockCapture
    {
        public class_2338 pos;
        public class_2680 lastState;
        public class_2487 blockEntity;

        public BlockCapture(class_2338 pos, class_2680 lastState, class_2487 blockEntity)
        {
            this.pos = pos;
            this.lastState = lastState;
            this.blockEntity = blockEntity;
        }
    }
}
