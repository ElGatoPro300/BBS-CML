package mchorse.bbs_mod.actions.values;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.EnumUtils;
import net.minecraft.class_1838;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_3965;

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

    public void setHitResult(class_3965 result)
    {
        this.x.set(result.method_17777().method_10263());
        this.y.set(result.method_17777().method_10264());
        this.z.set(result.method_17777().method_10260());
        this.hitX.set(result.method_17784().field_1352);
        this.hitY.set(result.method_17784().field_1351);
        this.hitZ.set(result.method_17784().field_1350);
        this.inside.set(result.method_17781());
        this.direction.set(result.method_17780().ordinal());
    }

    public void setHitResult(class_1838 context)
    {
        this.x.set(context.method_8037().method_10263());
        this.y.set(context.method_8037().method_10264());
        this.z.set(context.method_8037().method_10260());
        this.hitX.set(context.method_17698().field_1352);
        this.hitY.set(context.method_17698().field_1351);
        this.hitZ.set(context.method_17698().field_1350);
        this.inside.set(context.method_17699());
        this.direction.set(context.method_8038().ordinal());
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

    public class_3965 getHitResult()
    {
        class_2338 pos = new class_2338(this.x.get(), this.y.get(), this.z.get());
        class_243 vec = new class_243(this.hitX.get(), this.hitY.get(), this.hitZ.get());

        return new class_3965(vec, EnumUtils.getValue(this.direction.get(), class_2350.values(), class_2350.field_11036), pos, this.inside.get());
    }
}