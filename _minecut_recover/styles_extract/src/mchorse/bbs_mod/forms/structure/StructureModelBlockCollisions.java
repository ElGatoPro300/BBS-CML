package mchorse.bbs_mod.forms.structure;

import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_265;
import java.util.List;

/**
 * @deprecated use {@link ModelBlockSolidCollisions} — kept as a thin delegate for existing call sites.
 */
@Deprecated
public final class StructureModelBlockCollisions
{
    private StructureModelBlockCollisions()
    {}

    public static void updateRegistration(ModelBlockEntity entity)
    {
        ModelBlockSolidCollisions.updateRegistration(entity);
    }

    public static void unregister(ModelBlockEntity entity)
    {
        ModelBlockSolidCollisions.unregister(entity);
    }

    public static boolean hasSolidStructureHitbox(ModelBlockEntity entity)
    {
        return ModelBlockSolidCollisions.hasSolidFormHitbox(entity);
    }

    public static void appendShapes(class_1297 entity, class_238 swept, class_1937 world, List<class_265> collisions)
    {
        ModelBlockSolidCollisions.appendShapes(entity, swept, world, collisions);
    }

    public static List<class_265> wrapMutable(List<class_265> collisions)
    {
        return ModelBlockSolidCollisions.wrapMutable(collisions);
    }

    public static class_238 sweptBox(class_1297 entity, class_243 movement)
    {
        return ModelBlockSolidCollisions.sweptBox(entity, movement);
    }
}
