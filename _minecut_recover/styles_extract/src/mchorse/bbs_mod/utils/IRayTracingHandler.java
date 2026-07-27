package mchorse.bbs_mod.utils;

import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3965;

public interface IRayTracingHandler
{
    public class_3965 rayTrace(class_1937 world, class_243 pos, class_243 direction, double d);

    public class_239 rayTraceEntity(class_1297 entity, class_1937 world, class_243 pos, class_243 direction, double d);
}
