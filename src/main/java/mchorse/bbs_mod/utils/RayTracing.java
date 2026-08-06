package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.entity.ActorEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class RayTracing
{
    public static Vec3d fromVector3d(Vector3d vector)
    {
        return new Vec3d(vector.x, vector.y, vector.z);
    }

    public static Vec3d fromVector3f(Vector3f vector)
    {
        return new Vec3d(vector.x, vector.y, vector.z);
    }

    public static BlockHitResult rayTrace(World world, Camera camera, double d)
    {
        return rayTrace(world, fromVector3d(camera.position), fromVector3f(camera.getLookDirection()), d);
    }

    public static BlockHitResult rayTrace(World world, Vec3d pos, Vec3d direction, double d)
    {
        return world.raycast(new RaycastContext(
            pos,
            pos.add(direction.normalize().multiply(d)),
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            ShapeContext.absent()
        ));
    }

    public static HitResult rayTraceEntity(World world, Camera camera, double d)
    {
        Vector3f lookDirection = camera.getLookDirection();
        Vec3d pos = new Vec3d(camera.position.x, camera.position.y, camera.position.z);
        Vec3d look = new Vec3d(lookDirection.x, lookDirection.y, lookDirection.z);

        return rayTraceEntity(world, pos, look, d);
    }

    public static HitResult rayTraceEntity(World world, Vec3d pos, Vec3d direction, double d)
    {
        ActorEntity entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);

        entity.setPos(pos.x, pos.y, pos.z);

        return rayTraceEntity(entity, world, pos, direction, d);
    }

    public static HitResult rayTraceEntity(Entity entity, World world, Vec3d pos, Vec3d direction, double d)
    {
        BlockHitResult blockHit = rayTrace(world, pos, direction, d);

        double dist1 = blockHit != null ? blockHit.getPos().squaredDistanceTo(pos) : d * d;
        Vec3d dir = direction.normalize();
        Vec3d posDir = pos.add(dir.x * d, dir.y * d, dir.z * d);
        Box box = new Box(pos.x - 0.5D, pos.y - 0.5D, pos.z - 0.5D, pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D)
            .stretch(dir.multiply(d))
            .expand(1D, 1D, 1D);

        EntityHitResult entityHit = ProjectileUtil.raycast(entity, pos, posDir, box, e -> !e.isSpectator() && e.canHit(), dist1);

        return entityHit == null || entityHit.getType() == HitResult.Type.MISS ? blockHit : entityHit;
    }
}