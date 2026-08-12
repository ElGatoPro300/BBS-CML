package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.utils.AABB;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RayTracing
{
    public static final List<IRayTracingHandler> handlers = new ArrayList<>();

    public static Vec3 fromVector3d(Vector3d vector)
    {
        return new Vec3(vector.x, vector.y, vector.z);
    }

    public static Vec3 fromVector3f(Vector3f vector)
    {
        return new Vec3(vector.x, vector.y, vector.z);
    }

    public static BlockHitResult rayTrace(Level world, Camera camera, double d)
    {
        return rayTrace(world, fromVector3d(camera.position), fromVector3f(camera.getLookDirection()), d);
    }

    public static BlockHitResult rayTrace(Level world, Vec3 pos, Vec3 direction, double d)
    {
        for (IRayTracingHandler handler : handlers)
        {
            BlockHitResult result = handler.rayTrace(world, pos, direction, d);

            if (result != null)
            {
                return result;
            }
        }

        return world.clip(new ClipContext(
            pos,
            pos.add(direction.normalize().scale(d)),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            CollisionContext.empty()
        ));
    }

    public static HitResult rayTraceEntity(Level world, Camera camera, double d)
    {
        Vector3f lookDirection = camera.getLookDirection();
        Vec3 pos = new Vec3(camera.position.x, camera.position.y, camera.position.z);
        Vec3 look = new Vec3(lookDirection.x, lookDirection.y, lookDirection.z);

        return rayTraceEntity(world, pos, look, d);
    }

    public static HitResult rayTraceEntity(Level world, Vec3 pos, Vec3 direction, double d)
    {
        ActorEntity entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);

        entity.setPosRaw(pos.x, pos.y, pos.z);

        return rayTraceEntity(entity, world, pos, direction, d);
    }

    public static HitResult rayTraceEntity(Entity entity, Level world, Vec3 pos, Vec3 direction, double d)
    {
        for (IRayTracingHandler handler : handlers)
        {
            HitResult result = handler.rayTraceEntity(entity, world, pos, direction, d);

            if (result != null)
            {
                return result;
            }
        }

        BlockHitResult blockHit = rayTrace(world, pos, direction, d);

        double dist1 = blockHit != null ? blockHit.getLocation().distanceToSqr(pos) : d * d;
        Vec3 dir = direction.normalize();
        Vec3 posDir = pos.add(dir.x * d, dir.y * d, dir.z * d);
        AABB box = new AABB(pos.x - 0.5D, pos.y - 0.5D, pos.z - 0.5D, pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D)
            .expandTowards(dir.scale(d))
            .inflate(1D, 1D, 1D);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(entity, pos, posDir, box, e -> !e.isSpectator() && e.isPickable(), dist1);

        return entityHit == null || entityHit.getType() == HitResult.Type.MISS ? blockHit : entityHit;
    }

    public static double intersect(Vector3d pos, Vector3f dir, AABB aabb)
    {
        Vector2d result = new Vector2d();

        if (aabb.intersectsRay(pos, dir, result))
        {
            return result.x;
        }

        return Double.POSITIVE_INFINITY;
    }
}