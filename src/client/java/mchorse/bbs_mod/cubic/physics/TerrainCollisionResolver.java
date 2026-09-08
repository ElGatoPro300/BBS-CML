package mchorse.bbs_mod.cubic.physics;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves spring-chain collisions against solid world blocks. Each particle is
 * tested against nearby blocks (gather per particle, not whole-chain then
 * iterate). Contacts use closest-point depenetration with Coulomb friction in
 * Verlet velocity space — same math as the reference gather-all path.
 */
public final class TerrainCollisionResolver
{
    private static final float EPS = 1.0e-5f;
    private static final int RELAXATIONS = 2;
    private static final int DEPENETRATION_STEPS = 4;
    private static final float SEGMENT_SAMPLE_STEP = 0.5F;

    private TerrainCollisionResolver()
    {
    }

    public static void resolve(World world, Vector3f[] pos, Vector3f[] prev, int from, int to, float radius, float friction)
    {
        if (world == null || pos == null || prev == null || from < 0 || to > pos.length || to > prev.length || from >= to || radius <= 0F)
        {
            return;
        }

        float f = MathHelper.clamp(friction, 0F, 1F);
        Vector3f normal = new Vector3f();
        Vector3f sample = new Vector3f();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        List<float[]> boxes = new ArrayList<>();

        for (int relax = 0; relax < RELAXATIONS; relax++)
        {
            /* Friction lands once per solve (last relaxation), not every pass. */
            boolean applyFriction = relax == RELAXATIONS - 1;

            for (int i = from; i < to; i++)
            {
                gatherNearbyBoxes(world, mutable, boxes, pos[i], prev[i], radius);
                collideSphere(pos[i], prev[i], radius, f, boxes, normal, applyFriction);
            }

            for (int i = from; i < to - 1; i++)
            {
                gatherSegmentBoxes(world, mutable, boxes, pos[i], pos[i + 1], radius);
                collideSegment(pos[i], pos[i + 1], radius, boxes, normal, sample);
            }
        }
    }

    private static void gatherNearbyBoxes(World world, BlockPos.Mutable mutable, List<float[]> boxes, Vector3f pos, Vector3f prev, float radius)
    {
        boxes.clear();

        float minX = Math.min(pos.x, prev.x);
        float minY = Math.min(pos.y, prev.y);
        float minZ = Math.min(pos.z, prev.z);
        float maxX = Math.max(pos.x, prev.x);
        float maxY = Math.max(pos.y, prev.y);
        float maxZ = Math.max(pos.z, prev.z);

        collectBoxesInAabb(world, mutable, boxes,
            MathHelper.floor(minX - radius), MathHelper.floor(minY - radius), MathHelper.floor(minZ - radius),
            MathHelper.floor(maxX + radius), MathHelper.floor(maxY + radius), MathHelper.floor(maxZ + radius));
    }

    private static void gatherSegmentBoxes(World world, BlockPos.Mutable mutable, List<float[]> boxes, Vector3f a, Vector3f b, float radius)
    {
        boxes.clear();

        collectBoxesInAabb(world, mutable, boxes,
            MathHelper.floor(Math.min(a.x, b.x) - radius), MathHelper.floor(Math.min(a.y, b.y) - radius), MathHelper.floor(Math.min(a.z, b.z) - radius),
            MathHelper.floor(Math.max(a.x, b.x) + radius), MathHelper.floor(Math.max(a.y, b.y) + radius), MathHelper.floor(Math.max(a.z, b.z) + radius));
    }

    private static void collectBoxesInAabb(World world, BlockPos.Mutable mutable, List<float[]> boxes, int bx1, int by1, int bz1, int bx2, int by2, int bz2)
    {
        for (int x = bx1; x <= bx2; x++)
        {
            for (int y = by1; y <= by2; y++)
            {
                for (int z = bz1; z <= bz2; z++)
                {
                    mutable.set(x, y, z);

                    if (!world.isChunkLoaded(mutable))
                    {
                        continue;
                    }

                    BlockState state = world.getBlockState(mutable);

                    if (state == null)
                    {
                        continue;
                    }

                    if (state.isFullCube(world, mutable))
                    {
                        boxes.add(new float[] {x, y, z, x + 1F, y + 1F, z + 1F});
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(world, mutable, ShapeContext.absent());

                    if (shape.isEmpty())
                    {
                        continue;
                    }

                    for (Box box : shape.getBoundingBoxes())
                    {
                        boxes.add(new float[] {
                            (float) (x + box.minX), (float) (y + box.minY), (float) (z + box.minZ),
                            (float) (x + box.maxX), (float) (y + box.maxY), (float) (z + box.maxZ)
                        });
                    }
                }
            }
        }
    }

    private static void collideSphere(Vector3f p, Vector3f prev, float radius, float friction, List<float[]> boxes, Vector3f normal, boolean applyFriction)
    {
        float pushX = 0F;
        float pushY = 0F;
        float pushZ = 0F;

        for (int step = 0; step < DEPENETRATION_STEPS; step++)
        {
            float pen = deepestPenetration(p.x, p.y, p.z, radius, boxes, normal);

            if (pen <= EPS)
            {
                break;
            }

            p.x += normal.x * pen;
            p.y += normal.y * pen;
            p.z += normal.z * pen;

            pushX += normal.x * pen;
            pushY += normal.y * pen;
            pushZ += normal.z * pen;
        }

        if (!applyFriction)
        {
            return;
        }

        float pushLenSq = pushX * pushX + pushY * pushY + pushZ * pushZ;

        if (pushLenSq <= EPS * EPS)
        {
            return;
        }

        float inv = 1F / (float) Math.sqrt(pushLenSq);
        applyFriction(p, prev, pushX * inv, pushY * inv, pushZ * inv, friction);
    }

    private static void collideSegment(Vector3f a, Vector3f b, float radius, List<float[]> boxes, Vector3f normal, Vector3f sample)
    {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float dz = b.z - a.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        int samples = (int) Math.ceil(len / (radius * SEGMENT_SAMPLE_STEP));

        if (samples <= 1)
        {
            return;
        }

        for (int s = 1; s < samples; s++)
        {
            float t = s / (float) samples;

            sample.set(a.x + dx * t, a.y + dy * t, a.z + dz * t);

            float pen = deepestPenetration(sample.x, sample.y, sample.z, radius, boxes, normal);

            if (pen <= EPS)
            {
                continue;
            }

            float wa = 1F - t;
            float wb = t;
            float distribute = pen / (wa * wa + wb * wb);

            a.x += normal.x * wa * distribute;
            a.y += normal.y * wa * distribute;
            a.z += normal.z * wa * distribute;

            b.x += normal.x * wb * distribute;
            b.y += normal.y * wb * distribute;
            b.z += normal.z * wb * distribute;
        }
    }

    private static void applyFriction(Vector3f p, Vector3f prev, float nx, float ny, float nz, float friction)
    {
        float vx = p.x - prev.x;
        float vy = p.y - prev.y;
        float vz = p.z - prev.z;

        float vn = vx * nx + vy * ny + vz * nz;

        float tangentX = vx - nx * vn;
        float tangentY = vy - ny * vn;
        float tangentZ = vz - nz * vn;

        /* Keep outward separation but kill motion driving into the surface. */
        float normalScale = vn > 0F ? vn : 0F;
        float tangentScale = 1F - friction;

        prev.x = p.x - (nx * normalScale + tangentX * tangentScale);
        prev.y = p.y - (ny * normalScale + tangentY * tangentScale);
        prev.z = p.z - (nz * normalScale + tangentZ * tangentScale);
    }

    private static float deepestPenetration(float px, float py, float pz, float radius, List<float[]> boxes, Vector3f outNormal)
    {
        float best = 0F;

        for (int i = 0, n = boxes.size(); i < n; i++)
        {
            float[] box = boxes.get(i);

            float cx = clamp(px, box[0], box[3]);
            float cy = clamp(py, box[1], box[4]);
            float cz = clamp(pz, box[2], box[5]);

            float dx = px - cx;
            float dy = py - cy;
            float dz = pz - cz;
            float d2 = dx * dx + dy * dy + dz * dz;

            if (d2 > radius * radius)
            {
                continue;
            }

            float pen;
            float nx;
            float ny;
            float nz;

            if (d2 > EPS * EPS)
            {
                float d = (float) Math.sqrt(d2);
                float inv = 1F / d;
                nx = dx * inv;
                ny = dy * inv;
                nz = dz * inv;
                pen = radius - d;
            }
            else
            {
                /* Centre inside the box: push out through the nearest face. */
                float dMinX = px - box[0];
                float dMaxX = box[3] - px;
                float dMinY = py - box[1];
                float dMaxY = box[4] - py;
                float dMinZ = pz - box[2];
                float dMaxZ = box[5] - pz;

                float face = dMinX;
                nx = -1F;
                ny = 0F;
                nz = 0F;

                if (dMaxX < face)
                {
                    face = dMaxX;
                    nx = 1F;
                    ny = 0F;
                    nz = 0F;
                }

                if (dMinY < face)
                {
                    face = dMinY;
                    nx = 0F;
                    ny = -1F;
                    nz = 0F;
                }

                if (dMaxY < face)
                {
                    face = dMaxY;
                    nx = 0F;
                    ny = 1F;
                    nz = 0F;
                }

                if (dMinZ < face)
                {
                    face = dMinZ;
                    nx = 0F;
                    ny = 0F;
                    nz = -1F;
                }

                if (dMaxZ < face)
                {
                    face = dMaxZ;
                    nx = 0F;
                    ny = 0F;
                    nz = 1F;
                }

                pen = radius + face;
            }

            if (pen > best)
            {
                best = pen;
                outNormal.set(nx, ny, nz);
            }
        }

        return best;
    }

    public static boolean hasFullCubeInAabb(World world, BlockPos.Mutable mutable, int minBX, int minBY, int minBZ, int maxBX, int maxBY, int maxBZ)
    {
        if (world == null)
        {
            return false;
        }

        for (int x = minBX; x <= maxBX; x++)
        {
            for (int y = minBY; y <= maxBY; y++)
            {
                for (int z = minBZ; z <= maxBZ; z++)
                {
                    mutable.set(x, y, z);

                    if (!world.isChunkLoaded(mutable))
                    {
                        continue;
                    }

                    BlockState block = world.getBlockState(mutable);

                    if (block == null)
                    {
                        continue;
                    }

                    if (block.isFullCube(world, mutable))
                    {
                        return true;
                    }

                    if (!block.getCollisionShape(world, mutable, ShapeContext.absent()).isEmpty())
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static float clamp(float value, float min, float max)
    {
        return value < min ? min : (value > max ? max : value);
    }
}
