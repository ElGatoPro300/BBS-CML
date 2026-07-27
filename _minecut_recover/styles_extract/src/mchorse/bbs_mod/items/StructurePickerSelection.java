package mchorse.bbs_mod.items;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.class_1937;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;

public class StructurePickerSelection
{
    public static class_2338 min(class_2338 a, class_2338 b)
    {
        return new class_2338(
            Math.min(a.method_10263(), b.method_10263()),
            Math.min(a.method_10264(), b.method_10264()),
            Math.min(a.method_10260(), b.method_10260())
        );
    }

    public static class_2338 max(class_2338 a, class_2338 b)
    {
        return new class_2338(
            Math.max(a.method_10263(), b.method_10263()),
            Math.max(a.method_10264(), b.method_10264()),
            Math.max(a.method_10260(), b.method_10260())
        );
    }

    public static int spanX(class_2338 min, class_2338 max)
    {
        return max.method_10263() - min.method_10263() + 1;
    }

    public static int spanY(class_2338 min, class_2338 max)
    {
        return max.method_10264() - min.method_10264() + 1;
    }

    public static int spanZ(class_2338 min, class_2338 max)
    {
        return max.method_10260() - min.method_10260() + 1;
    }

    public static StructurePickerPlane inferPlane(class_2338 first, class_2338 second, StructurePickerMode mode)
    {
        if (!mode.isFlat())
        {
            return StructurePickerPlane.XZ;
        }

        class_2338 min = StructurePickerSelection.min(first, second);
        class_2338 max = StructurePickerSelection.max(first, second);

        if (min.method_10264() == max.method_10264())
        {
            return StructurePickerPlane.XZ;
        }

        return StructurePickerPlane.VERTICAL;
    }

    public static StructurePickerAxis inferVerticalAxis(class_2338 first, class_2338 second)
    {
        class_2338 min = StructurePickerSelection.min(first, second);
        class_2338 max = StructurePickerSelection.max(first, second);

        if (min.method_10263() == max.method_10263())
        {
            return StructurePickerAxis.X;
        }

        if (min.method_10260() == max.method_10260())
        {
            return StructurePickerAxis.Z;
        }

        return StructurePickerSelection.spanX(min, max) <= StructurePickerSelection.spanZ(min, max)
            ? StructurePickerAxis.X
            : StructurePickerAxis.Z;
    }

    public static class_2338 adjustSecond(class_2338 first, class_2338 second, StructurePickerMode mode)
    {
        return second;
    }

    public static List<class_2338> collect(class_1937 world, class_2338 first, class_2338 second, StructurePickerMode mode)
    {
        return StructurePickerSelection.collect(world, first, second, mode, false);
    }

    public static List<class_2338> collect(class_1937 world, class_2338 first, class_2338 second, StructurePickerMode mode, boolean includeAir)
    {
        return StructurePickerSelection.collect(world, first, second, mode, includeAir, null);
    }

    public static List<class_2338> collect(class_1937 world, class_2338 first, class_2338 second, StructurePickerMode mode, boolean includeAir, class_2350 triangleFacing)
    {
        class_2338 adjusted = StructurePickerSelection.adjustSecond(first, second, mode);
        class_2338 min = StructurePickerSelection.min(first, adjusted);
        class_2338 max = StructurePickerSelection.max(first, adjusted);
        List<class_2338> blocks = new ArrayList<>();

        for (int x = min.method_10263(); x <= max.method_10263(); x++)
        {
            for (int y = min.method_10264(); y <= max.method_10264(); y++)
            {
                for (int z = min.method_10260(); z <= max.method_10260(); z++)
                {
                    class_2338 pos = new class_2338(x, y, z);

                    if (StructurePickerSelection.contains(mode, first, adjusted, min, max, x, y, z, triangleFacing) && (includeAir || !world.method_8320(pos).method_26215()))
                    {
                        blocks.add(pos);
                    }
                }
            }
        }

        return blocks;
    }

    public static List<class_2338> preview(class_1937 world, class_2338 first, class_2338 second, StructurePickerMode mode)
    {
        return StructurePickerSelection.preview(world, first, second, mode, null);
    }

    public static List<class_2338> preview(class_1937 world, class_2338 first, class_2338 second, StructurePickerMode mode, class_2350 triangleFacing)
    {
        if (first == null || second == null)
        {
            return List.of();
        }

        class_2338 adjusted = StructurePickerSelection.adjustSecond(first, second, mode);
        class_2338 min = StructurePickerSelection.min(first, adjusted);
        class_2338 max = StructurePickerSelection.max(first, adjusted);
        List<class_2338> blocks = new ArrayList<>();

        for (int x = min.method_10263(); x <= max.method_10263(); x++)
        {
            for (int y = min.method_10264(); y <= max.method_10264(); y++)
            {
                for (int z = min.method_10260(); z <= max.method_10260(); z++)
                {
                    if (StructurePickerSelection.contains(mode, first, adjusted, min, max, x, y, z, triangleFacing))
                    {
                        blocks.add(new class_2338(x, y, z));
                    }
                }
            }
        }

        return blocks;
    }

    private static boolean contains(StructurePickerMode mode, class_2338 first, class_2338 second, class_2338 min, class_2338 max, int x, int y, int z, class_2350 triangleFacing)
    {
        return switch (mode)
        {
            case CUBE -> true;
            case RECTANGLE -> StructurePickerSelection.onFlatPlane(first, second, min, max, x, y, z);
            case TRIANGLE -> StructurePickerSelection.onFlatPlane(first, second, min, max, x, y, z)
                && StructurePickerSelection.inFlatTriangle(first, second, min, max, x, y, z, triangleFacing);
            case CIRCLE -> StructurePickerSelection.onFlatPlane(first, second, min, max, x, y, z)
                && StructurePickerSelection.inFlatCircle(first, second, min, max, x, y, z);
            case CONE -> StructurePickerSelection.inCone(min, max, x, y, z);
            case SPHERE -> StructurePickerSelection.inSphere(min, max, x, y, z);
            case CYLINDER -> StructurePickerSelection.inCircle(min, max, x, z);
            case BLOCK, SAME, BRUSH -> x == min.method_10263() && y == min.method_10264() && z == min.method_10260();
        };
    }

    /**
     * Stamp a sphere/cube brush on the hit face plane. Radius spreads along the
     * surface; depth goes inward from that face. Only the painted face is selected
     * (e.g. a mountain wall), not the ground or interior volume. Plant covers
     * (grass, flowers, tall grass) also pull in the solid block beneath them.
     */
    public static List<class_2338> collectBrushSurface(class_1937 world, class_2338 center, StructurePickerBrushShape shape, int radius, int depth, class_2350 face)
    {
        LinkedHashSet<class_2338> blocks = new LinkedHashSet<>();

        if (world == null || center == null || radius < 0)
        {
            return new ArrayList<>();
        }

        class_2350 outward = face == null ? class_2350.field_11036 : face;
        class_2350 inward = outward.method_10153();
        class_2350[] tangents = StructurePickerSelection.tangentAxes(outward);
        int r = Math.max(0, radius);
        int layers = Math.max(1, depth);
        int scan = Math.max(1, r) + 2;

        for (int u = -r; u <= r; u++)
        {
            for (int v = -r; v <= r; v++)
            {
                if (shape == StructurePickerBrushShape.SPHERE && u * u + v * v > r * r)
                {
                    continue;
                }

                class_2338 column = center.method_10079(tangents[0], u).method_10079(tangents[1], v);
                class_2338 surface = StructurePickerSelection.findFaceSurface(world, column, outward, inward, scan);

                if (surface == null)
                {
                    continue;
                }

                for (int layer = 0; layer < layers; layer++)
                {
                    class_2338 pos = surface.method_10079(inward, layer);
                    class_2680 state = world.method_8320(pos);

                    if (state.method_26215())
                    {
                        break;
                    }

                    blocks.add(pos.method_10062());
                    StructurePickerSelection.addPlantSupport(world, pos, blocks);
                }
            }
        }

        return new ArrayList<>(blocks);
    }

    private static class_2350[] tangentAxes(class_2350 face)
    {
        return switch (face.method_10166())
        {
            case field_11052 -> new class_2350[] {class_2350.field_11034, class_2350.field_11035};
            case field_11048 -> new class_2350[] {class_2350.field_11036, class_2350.field_11035};
            case field_11051 -> new class_2350[] {class_2350.field_11036, class_2350.field_11034};
        };
    }

    /**
     * Walk from the air side of {@code column} inward until air meets solid —
     * that solid is the surface facing {@code outward}.
     */
    private static class_2338 findFaceSurface(class_1937 world, class_2338 column, class_2350 outward, class_2350 inward, int scan)
    {
        class_2338 cursor = column.method_10079(outward, scan);

        for (int i = 0; i < scan * 2 + 1; i++)
        {
            class_2338 next = cursor.method_10093(inward);
            boolean cursorEmpty = StructurePickerSelection.isBrushEmpty(world, cursor);
            boolean nextSolid = !StructurePickerSelection.isBrushEmpty(world, next);

            if (cursorEmpty && nextSolid)
            {
                return next.method_10062();
            }

            cursor = next;
        }

        return null;
    }

    /**
     * Air and non-colliding fluids count as empty for surface finding; plants count
     * as occupied so grass on dirt still forms a selectable surface.
     */
    private static boolean isBrushEmpty(class_1937 world, class_2338 pos)
    {
        return world.method_8320(pos).method_26215();
    }

    private static boolean isPlantCover(class_1937 world, class_2338 pos, class_2680 state)
    {
        if (state.method_26215() || state.method_26212(world, pos))
        {
            return false;
        }

        return !state.method_26227().method_15771();
    }

    private static void addPlantSupport(class_1937 world, class_2338 pos, Set<class_2338> out)
    {
        class_2680 state = world.method_8320(pos);

        if (!StructurePickerSelection.isPlantCover(world, pos, state))
        {
            return;
        }

        class_2338 below = pos.method_10074();

        for (int i = 0; i < 4; i++)
        {
            class_2680 belowState = world.method_8320(below);

            if (belowState.method_26215())
            {
                return;
            }

            if (!StructurePickerSelection.isPlantCover(world, below, belowState))
            {
                out.add(below.method_10062());

                return;
            }

            below = below.method_10074();
        }
    }

    public static boolean isSurfaceBlock(class_1937 world, class_2338 pos)
    {
        for (class_2350 direction : class_2350.values())
        {
            if (world.method_8320(pos.method_10093(direction)).method_26215())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Face-connected flood fill of the same block type (ignores blockstate properties
     * so e.g. rotated logs still connect).
     */
    public static List<class_2338> collectConnectedSame(class_1937 world, class_2338 origin, int maxBlocks)
    {
        List<class_2338> found = new ArrayList<>();

        if (world == null || origin == null || maxBlocks <= 0)
        {
            return found;
        }

        class_2680 originState = world.method_8320(origin);
        class_2248 match = originState.method_26204();

        if (originState.method_26215())
        {
            return found;
        }

        ArrayDeque<class_2338> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();

        queue.add(origin.method_10062());
        visited.add(origin.method_10063());

        while (!queue.isEmpty() && found.size() < maxBlocks)
        {
            class_2338 pos = queue.removeFirst();

            if (world.method_8320(pos).method_26204() != match)
            {
                continue;
            }

            found.add(pos);

            for (class_2350 direction : class_2350.values())
            {
                class_2338 next = pos.method_10093(direction);
                long key = next.method_10063();

                if (visited.add(key))
                {
                    queue.add(next.method_10062());
                }
            }
        }

        return found;
    }

    private static boolean onFlatPlane(class_2338 first, class_2338 second, class_2338 min, class_2338 max, int x, int y, int z)
    {
        StructurePickerPlane plane = StructurePickerSelection.inferPlane(first, second, StructurePickerMode.RECTANGLE);

        if (plane == StructurePickerPlane.XZ)
        {
            return y == min.method_10264();
        }

        StructurePickerAxis locked = StructurePickerSelection.inferVerticalAxis(first, second);

        if (locked == StructurePickerAxis.Z)
        {
            return z == min.method_10260();
        }

        return x == min.method_10263();
    }

    private static boolean inFlatCircle(class_2338 first, class_2338 second, class_2338 min, class_2338 max, int x, int y, int z)
    {
        int[] extents = StructurePickerSelection.flatExtents(first, second, min, max);

        return StructurePickerSelection.inCircle2D(
            extents[0],
            extents[1],
            extents[2],
            extents[3],
            StructurePickerSelection.flatCoordA(first, second, x, y, z) + 0.5D,
            StructurePickerSelection.flatCoordB(first, second, x, y, z) + 0.5D
        );
    }

    private static boolean inFlatTriangle(class_2338 first, class_2338 second, class_2338 min, class_2338 max, int x, int y, int z, class_2350 triangleFacing)
    {
        int[] extents = StructurePickerSelection.flatExtents(first, second, min, max);
        double[] forward = StructurePickerSelection.resolveTriangleForward(first, second, triangleFacing);

        return StructurePickerSelection.inEquilateralTriangleOriented(
            extents[0],
            extents[1],
            extents[2],
            extents[3],
            StructurePickerSelection.flatCoordA(first, second, x, y, z) + 0.5D,
            StructurePickerSelection.flatCoordB(first, second, x, y, z) + 0.5D,
            forward[0],
            forward[1]
        );
    }

    private static double[] resolveTriangleForward(class_2338 first, class_2338 second, class_2350 triangleFacing)
    {
        if (triangleFacing == null)
        {
            return new double[] {0D, 1D};
        }

        StructurePickerPlane plane = StructurePickerSelection.inferPlane(first, second, StructurePickerMode.TRIANGLE);
        double forwardA;
        double forwardB;

        if (plane == StructurePickerPlane.XZ)
        {
            forwardA = triangleFacing.method_10148();
            forwardB = triangleFacing.method_10165();
        }
        else
        {
            StructurePickerAxis locked = StructurePickerSelection.inferVerticalAxis(first, second);

            if (locked == StructurePickerAxis.Z)
            {
                forwardA = triangleFacing.method_10148();
                forwardB = triangleFacing.method_10164();
            }
            else
            {
                forwardA = triangleFacing.method_10165();
                forwardB = triangleFacing.method_10164();
            }
        }

        double length = Math.sqrt(forwardA * forwardA + forwardB * forwardB);

        if (length < 0.001D)
        {
            return new double[] {0D, 1D};
        }

        return new double[] {forwardA / length, forwardB / length};
    }

    private static boolean inEquilateralTriangleOriented(int minA, int maxA, int minB, int maxB, double a, double b, double forwardA, double forwardB)
    {
        double rightA = -forwardB;
        double rightB = forwardA;
        double minForward = Double.POSITIVE_INFINITY;
        double maxForward = Double.NEGATIVE_INFINITY;
        double minRight = Double.POSITIVE_INFINITY;
        double maxRight = Double.NEGATIVE_INFINITY;

        for (double cornerA : new double[] {minA, maxA + 1D})
        {
            for (double cornerB : new double[] {minB, maxB + 1D})
            {
                double forward = cornerA * forwardA + cornerB * forwardB;
                double right = cornerA * rightA + cornerB * rightB;

                minForward = Math.min(minForward, forward);
                maxForward = Math.max(maxForward, forward);
                minRight = Math.min(minRight, right);
                maxRight = Math.max(maxRight, right);
            }
        }

        double spanForward = maxForward - minForward;
        double spanRight = maxRight - minRight;

        if (spanForward <= 0D || spanRight <= 0D)
        {
            return false;
        }

        double sqrt3 = Math.sqrt(3D);
        double side = Math.min(spanRight, spanForward * 2D / sqrt3);

        if (side < 1D)
        {
            return false;
        }

        double height = side * sqrt3 / 2D;
        double centerRight = (minRight + maxRight) * 0.5D;
        double right0 = centerRight - side * 0.5D;
        double right1 = right0 + side;
        double baseForward = minForward + 0.5D;
        double apexForward = minForward + height - 0.5D;
        double pointForward = a * forwardA + b * forwardB;
        double pointRight = a * rightA + b * rightB;

        return StructurePickerSelection.pointInTriangle(pointForward, pointRight, baseForward, right0, baseForward, right1, apexForward, centerRight);
    }

    private static int[] flatExtents(class_2338 first, class_2338 second, class_2338 min, class_2338 max)
    {
        StructurePickerPlane plane = StructurePickerSelection.inferPlane(first, second, StructurePickerMode.CIRCLE);

        if (plane == StructurePickerPlane.XZ)
        {
            return new int[] {min.method_10263(), max.method_10263(), min.method_10260(), max.method_10260()};
        }

        StructurePickerAxis locked = StructurePickerSelection.inferVerticalAxis(first, second);

        if (locked == StructurePickerAxis.Z)
        {
            return new int[] {min.method_10263(), max.method_10263(), min.method_10264(), max.method_10264()};
        }

        return new int[] {min.method_10260(), max.method_10260(), min.method_10264(), max.method_10264()};
    }

    private static double flatCoordA(class_2338 first, class_2338 second, int x, int y, int z)
    {
        StructurePickerPlane plane = StructurePickerSelection.inferPlane(first, second, StructurePickerMode.CIRCLE);

        if (plane == StructurePickerPlane.XZ)
        {
            return x;
        }

        StructurePickerAxis locked = StructurePickerSelection.inferVerticalAxis(first, second);

        if (locked == StructurePickerAxis.Z)
        {
            return x;
        }

        return z;
    }

    private static double flatCoordB(class_2338 first, class_2338 second, int x, int y, int z)
    {
        StructurePickerPlane plane = StructurePickerSelection.inferPlane(first, second, StructurePickerMode.CIRCLE);

        if (plane == StructurePickerPlane.XZ)
        {
            return z;
        }

        return y;
    }

    private static boolean inCircle2D(int minA, int maxA, int minB, int maxB, double a, double b)
    {
        double ca = (minA + maxA + 1D) * 0.5D;
        double cb = (minB + maxB + 1D) * 0.5D;
        double ra = (maxA - minA + 1D) * 0.5D;
        double rb = (maxB - minB + 1D) * 0.5D;

        if (ra <= 0D || rb <= 0D)
        {
            return false;
        }

        double da = (a - ca) / ra;
        double db = (b - cb) / rb;

        return da * da + db * db <= 1D;
    }

    private static boolean inEquilateralTriangle2D(int minA, int maxA, int minB, int maxB, double a, double b)
    {
        return StructurePickerSelection.inEquilateralTriangleOriented(minA, maxA, minB, maxB, a, b, 0D, 1D);
    }

    private static boolean pointInTriangle(double px, double py, double x0, double y0, double x1, double y1, double x2, double y2)
    {
        double d1 = StructurePickerSelection.sign(px, py, x0, y0, x1, y1);
        double d2 = StructurePickerSelection.sign(px, py, x1, y1, x2, y2);
        double d3 = StructurePickerSelection.sign(px, py, x2, y2, x0, y0);
        boolean hasNeg = d1 < 0D || d2 < 0D || d3 < 0D;
        boolean hasPos = d1 > 0D || d2 > 0D || d3 > 0D;

        return !(hasNeg && hasPos);
    }

    private static double sign(double px, double py, double x0, double y0, double x1, double y1)
    {
        return (px - x1) * (y0 - y1) - (x0 - x1) * (py - y1);
    }

    private static boolean inCircle(class_2338 min, class_2338 max, int x, int z)
    {
        return StructurePickerSelection.inCircle2D(min.method_10263(), max.method_10263(), min.method_10260(), max.method_10260(), x + 0.5D, z + 0.5D);
    }

    private static boolean inSphere(class_2338 min, class_2338 max, int x, int y, int z)
    {
        double cx = (min.method_10263() + max.method_10263() + 1D) * 0.5D;
        double cy = (min.method_10264() + max.method_10264() + 1D) * 0.5D;
        double cz = (min.method_10260() + max.method_10260() + 1D) * 0.5D;
        double rx = StructurePickerSelection.spanX(min, max) * 0.5D;
        double ry = (max.method_10264() - min.method_10264() + 1D) * 0.5D;
        double rz = StructurePickerSelection.spanZ(min, max) * 0.5D;
        double dx = (x + 0.5D - cx) / rx;
        double dy = (y + 0.5D - cy) / ry;
        double dz = (z + 0.5D - cz) / rz;

        return dx * dx + dy * dy + dz * dz <= 1D;
    }

    private static boolean inCone(class_2338 min, class_2338 max, int x, int y, int z)
    {
        int height = max.method_10264() - min.method_10264() + 1;

        if (height <= 0)
        {
            return false;
        }

        double cx = (min.method_10263() + max.method_10263() + 1D) * 0.5D;
        double cz = (min.method_10260() + max.method_10260() + 1D) * 0.5D;
        double baseRadius = Math.min(StructurePickerSelection.spanX(min, max), StructurePickerSelection.spanZ(min, max)) * 0.5D;
        double t = (y + 0.5D - min.method_10264()) / height;
        double radius = baseRadius * (1D - t);
        double dx = x + 0.5D - cx;
        double dz = z + 0.5D - cz;

        return t >= 0D && t <= 1D && dx * dx + dz * dz <= radius * radius;
    }
}
