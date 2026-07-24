package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.mc.ValueBlockState;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

public class BlockForm extends Form
{
    public final ValueBlockState blockState = new ValueBlockState("block_state");
    public final ValueString blockEntityNbt = new ValueString("block_entity_nbt", "");
    public final ValueColor color = new ValueColor("color", new Color(1F, 1F, 1F, 0F));
    public final ValueInt breaking = new ValueInt("breaking", 0, 0, 10);
    public final ValueInt repeatX = new ValueInt("repeat_x", 1, 1, 64);
    public final ValueInt repeatY = new ValueInt("repeat_y", 1, 1, 64);
    public final ValueInt repeatZ = new ValueInt("repeat_z", 1, 1, 64);
    public final ValueBoolean repeatCenterX = new ValueBoolean("repeat_center_x", false);
    public final ValueBoolean repeatCenterY = new ValueBoolean("repeat_center_y", false);
    public final ValueBoolean repeatCenterZ = new ValueBoolean("repeat_center_z", false);

    public static int repeatAxisStart(int count, boolean centered)
    {
        if (!centered || count <= 1)
        {
            return 0;
        }

        return -((count - 1) / 2);
    }

    public BlockForm()
    {
        this.add(this.blockState);
        this.add(this.blockEntityNbt);
        this.add(this.color);
        this.add(this.breaking);
        this.add(this.repeatX);
        this.add(this.repeatY);
        this.add(this.repeatZ);
        this.add(this.repeatCenterX);
        this.add(this.repeatCenterY);
        this.add(this.repeatCenterZ);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return Registries.BLOCK.getId(this.blockState.get().getBlock()).toString();
    }
}