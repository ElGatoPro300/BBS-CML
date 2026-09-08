package mchorse.bbs_mod.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface IRayTracingHandler
{
    public BlockHitResult rayTrace(World world, Vec3d pos, Vec3d direction, double d);

    public HitResult rayTraceEntity(Entity entity, World world, Vec3d pos, Vec3d direction, double d);
}
