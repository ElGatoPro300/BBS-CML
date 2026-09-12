package mchorse.bbs_mod.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface IRayTracingHandler
{
    public BlockHitResult rayTrace(Level world, Vec3 pos, Vec3 direction, double d);

    public HitResult rayTraceEntity(Entity entity, Level world, Vec3 pos, Vec3 direction, double d);
}
