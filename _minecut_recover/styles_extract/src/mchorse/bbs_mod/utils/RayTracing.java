package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.entity.ActorEntity;
import net.minecraft.class_1297;
import net.minecraft.class_1675;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3726;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RayTracing
{
    public static final List<IRayTracingHandler> handlers = new ArrayList<>();

    public static class_243 fromVector3d(Vector3d vector)
    {
        return new class_243(vector.x, vector.y, vector.z);
    }

    public static class_243 fromVector3f(Vector3f vector)
    {
        return new class_243(vector.x, vector.y, vector.z);
    }

    public static class_3965 rayTrace(class_1937 world, Camera camera, double d)
    {
        return rayTrace(world, fromVector3d(camera.position), fromVector3f(camera.getLookDirection()), d);
    }

    public static class_3965 rayTrace(class_1937 world, class_243 pos, class_243 direction, double d)
    {
        for (IRayTracingHandler handler : handlers)
        {
            class_3965 result = handler.rayTrace(world, pos, direction, d);

            if (result != null)
            {
                return result;
            }
        }

        return world.method_17742(new class_3959(
            pos,
            pos.method_1019(direction.method_1029().method_1021(d)),
            class_3959.class_3960.field_17558,
            class_3959.class_242.field_1348,
            class_3726.method_16194()
        ));
    }

    public static class_239 rayTraceEntity(class_1937 world, Camera camera, double d)
    {
        Vector3f lookDirection = camera.getLookDirection();
        class_243 pos = new class_243(camera.position.x, camera.position.y, camera.position.z);
        class_243 look = new class_243(lookDirection.x, lookDirection.y, lookDirection.z);

        return rayTraceEntity(world, pos, look, d);
    }

    public static class_239 rayTraceEntity(class_1937 world, class_243 pos, class_243 direction, double d)
    {
        ActorEntity entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);

        entity.method_23327(pos.field_1352, pos.field_1351, pos.field_1350);

        return rayTraceEntity(entity, world, pos, direction, d);
    }

    public static class_239 rayTraceEntity(class_1297 entity, class_1937 world, class_243 pos, class_243 direction, double d)
    {
        for (IRayTracingHandler handler : handlers)
        {
            class_239 result = handler.rayTraceEntity(entity, world, pos, direction, d);

            if (result != null)
            {
                return result;
            }
        }

        class_3965 blockHit = rayTrace(world, pos, direction, d);

        double dist1 = blockHit != null ? blockHit.method_17784().method_1025(pos) : d * d;
        class_243 dir = direction.method_1029();
        class_243 posDir = pos.method_1031(dir.field_1352 * d, dir.field_1351 * d, dir.field_1350 * d);
        class_238 box = new class_238(pos.field_1352 - 0.5D, pos.field_1351 - 0.5D, pos.field_1350 - 0.5D, pos.field_1352 + 0.5D, pos.field_1351 + 0.5D, pos.field_1350 + 0.5D)
            .method_18804(dir.method_1021(d))
            .method_1009(1D, 1D, 1D);

        class_3966 entityHit = class_1675.method_18075(entity, pos, posDir, box, e -> !e.method_7325() && e.method_5863(), dist1);

        return entityHit == null || entityHit.method_17783() == class_239.class_240.field_1333 ? blockHit : entityHit;
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