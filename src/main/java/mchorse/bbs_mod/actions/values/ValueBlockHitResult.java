package mchorse.bbs_mod.actions.values;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.EnumUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ValueBlockHitResult extends ValueGroup
{
    public final ValueInt x = new ValueInt("x", 0);
    public final ValueInt y = new ValueInt("y", 0);
    public final ValueInt z = new ValueInt("z", 0);
    public final ValueDouble hitX = new ValueDouble("hitX", 0D);
    public final ValueDouble hitY = new ValueDouble("hitY", 0D);
    public final ValueDouble hitZ = new ValueDouble("hitZ", 0D);
    public final ValueInt direction = new ValueInt("direction", 0);
    public final ValueBoolean inside = new ValueBoolean("inside", false);

    public ValueBlockHitResult(String id)
    {
        super(id);

        this.add(this.x);
        this.add(this.y);
        this.add(this.z);
        this.add(this.hitX);
        this.add(this.hitY);
        this.add(this.hitZ);
        this.add(this.direction);
        this.add(this.inside);
    }

    public void setHitResult(BlockHitResult result)
    {
        this.x.set(result.getBlockPos().getX());
        this.y.set(result.getBlockPos().getY());
        this.z.set(result.getBlockPos().getZ());
        this.hitX.set(result.getLocation().x);
        this.hitY.set(result.getLocation().y);
        this.hitZ.set(result.getLocation().z);
        this.inside.set(result.isInside());
        this.direction.set(result.getDirection().ordinal());
    }

    public void setHitResult(UseOnContext context)
    {
        this.x.set(context.getClickedPos().getX());
        this.y.set(context.getClickedPos().getY());
        this.z.set(context.getClickedPos().getZ());
        this.hitX.set(context.getClickLocation().x);
        this.hitY.set(context.getClickLocation().y);
        this.hitZ.set(context.getClickLocation().z);
        this.inside.set(context.isInside());
        this.direction.set(context.getClickedFace().ordinal());
    }

    public void shift(double x, double y, double z)
    {
        this.x.set((int) (this.x.get() + x));
        this.y.set((int) (this.y.get() + y));
        this.z.set((int) (this.z.get() + z));
        this.hitX.set(this.hitX.get() + x);
        this.hitY.set(this.hitY.get() + y);
        this.hitZ.set(this.hitZ.get() + z);
    }

    public BlockHitResult getHitResult()
    {
        BlockPos pos = new BlockPos(this.x.get(), this.y.get(), this.z.get());
        Vec3 vec = new Vec3(this.hitX.get(), this.hitY.get(), this.hitZ.get());

        return new BlockHitResult(vec, EnumUtils.getValue(this.direction.get(), Direction.values(), Direction.UP), pos, this.inside.get());
    }
}