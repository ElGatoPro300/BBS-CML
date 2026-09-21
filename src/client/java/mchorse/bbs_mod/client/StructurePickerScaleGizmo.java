package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.items.StructurePickerAxis;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.items.StructurePickerSelection;
import mchorse.bbs_mod.ui.items.UIStructurePickerPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Matrices;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;

/**
 * CUBE selection scale handles: click a corner to choose the free pivot,
 * then drag the colored XYZ axes to resize the single active cube region.
 */
public final class StructurePickerScaleGizmo
{
    /** Must match StructurePickerRenderer.CORNER_HANDLE — world size before distance scale. */
    private static final float CORNER_HANDLE = 0.28F;
    private static final double GIZMO_AXIS_LENGTH = 2.15D;
    /** Must match StructurePickerRenderer knob / axis half thickness. */
    private static final float GIZMO_KNOB = 0.20F;
    private static final float GIZMO_AXIS_HALF = 0.028F;
    /** Extra pick padding so handles are easy to grab without feeling larger than the preview. */
    private static final double GIZMO_PICK_PAD = 1.15D;
    /** Corner cubes: larger than the preview so they stay clickable from any distance. */
    private static final double CORNER_PICK_PAD = 2.35D;
    private static final double CORNER_PICK_MIN_PIXELS = 52D;
    private static final double GIZMO_STEM_PICK_PIXELS = 48D;
    private static final double GIZMO_TIP_PICK_PIXELS = 56D;
    /** Reference distance where visual handle scale == 1. */
    private static final double HANDLE_REF_DISTANCE = 8D;

    private static boolean resizeGizmoActive;
    private static int resizeRegionIndex = -1;
    private static BlockPos resizeFreeCorner;
    private static BlockPos resizeFixedCorner;
    private static boolean resizeAnchorIsMax;
    private static StructurePickerAxis resizeDragAxis;
    private static boolean resizeDragging;
    private static int resizeDragOriginCoord;

    private StructurePickerScaleGizmo()
    {
    }

    public static boolean isActive()
    {
        return StructurePickerScaleGizmo.resizeGizmoActive && StructurePickerScaleGizmo.resizeFreeCorner != null;
    }

    public static boolean isDragging()
    {
        return StructurePickerScaleGizmo.resizeDragging;
    }

    public static BlockPos getFreeCorner()
    {
        return StructurePickerScaleGizmo.resizeFreeCorner;
    }

    public static boolean isUsingMaxCorner()
    {
        return StructurePickerScaleGizmo.resizeAnchorIsMax;
    }

    /** Outward axis direction for the active scale corner (max = +, min = -). */
    public static boolean isScalePositive()
    {
        return StructurePickerScaleGizmo.resizeAnchorIsMax;
    }

    public static StructurePickerAxis getResizeDragAxis()
    {
        if (StructurePickerScaleGizmo.resizeDragAxis != null)
        {
            return StructurePickerScaleGizmo.resizeDragAxis;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || !StructurePickerScaleGizmo.resizeGizmoActive)
        {
            return null;
        }

        return StructurePickerScaleGizmo.pickAxisGizmo(mc);
    }

    public static double getAxisLength()
    {
        return StructurePickerScaleGizmo.GIZMO_AXIS_LENGTH;
    }

    /**
     * Keeps corner/gizmo visuals readable from far away (roughly constant on-screen size).
     */
    public static float getHandleVisualScale(double x, double y, double z)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eye = StructurePickerScaleGizmo.getViewEye(mc);
        double dist = eye.distanceTo(new Vec3d(x, y, z));
        float scale = (float) (dist / StructurePickerScaleGizmo.HANDLE_REF_DISTANCE);

        return MathUtils.clamp(scale, 0.7F, 7.5F);
    }

    public static boolean hasActiveCubeSelection()
    {
        if (StructurePickerClient.getMode() != StructurePickerMode.CUBE)
        {
            return false;
        }

        List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();
        int cubeRegions = 0;

        for (StructurePickerClient.Region region : regions)
        {
            if (region.mode() == StructurePickerMode.CUBE)
            {
                cubeRegions++;
            }
        }

        /* Brush paint stores many tiny CUBE AABBs — only a real cube-tool pick is scalable. */
        return cubeRegions == 1 && regions.size() == 1;
    }

    /**
     * Keep a scale gizmo on the latest cube region so handles are always available.
     */
    public static void ensure()
    {
        if (StructurePickerClient.getMode() != StructurePickerMode.CUBE || StructurePickerClient.getRegions().isEmpty())
        {
            StructurePickerScaleGizmo.clear();

            return;
        }

        List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();

        if (StructurePickerScaleGizmo.resizeGizmoActive
            && StructurePickerScaleGizmo.resizeRegionIndex >= 0
            && StructurePickerScaleGizmo.resizeRegionIndex < regions.size()
            && regions.get(StructurePickerScaleGizmo.resizeRegionIndex).mode() == StructurePickerMode.CUBE)
        {
            StructurePickerScaleGizmo.syncScaleCornersFromRegion();

            return;
        }

        for (int i = regions.size() - 1; i >= 0; i--)
        {
            if (regions.get(i).mode() == StructurePickerMode.CUBE)
            {
                StructurePickerScaleGizmo.activateRegionScale(i, true);

                return;
            }
        }

        StructurePickerScaleGizmo.clear();
    }

    public static void clear()
    {
        StructurePickerScaleGizmo.resizeGizmoActive = false;
        StructurePickerScaleGizmo.resizeRegionIndex = -1;
        StructurePickerScaleGizmo.resizeFreeCorner = null;
        StructurePickerScaleGizmo.resizeFixedCorner = null;
        StructurePickerScaleGizmo.resizeAnchorIsMax = false;
        StructurePickerScaleGizmo.resizeDragAxis = null;
        StructurePickerScaleGizmo.resizeDragging = false;
    }

    /**
     * True when the look ray hits a corner cube or scale-axis gizmo of the active selection.
     */
    public static boolean isOverSelectionInteractable(MinecraftClient mc)
    {
        if (StructurePickerClient.getMode() != StructurePickerMode.CUBE || !StructurePickerScaleGizmo.hasActiveCubeSelection())
        {
            return false;
        }

        StructurePickerScaleGizmo.ensure();

        return StructurePickerScaleGizmo.findCornerHit(mc, true) != null
            || (StructurePickerScaleGizmo.isActive() && StructurePickerScaleGizmo.pickAxisGizmo(mc) != null);
    }

    public static boolean isOverSelectionCorner(MinecraftClient mc)
    {
        return StructurePickerScaleGizmo.findCornerHit(mc, true) != null;
    }

    public static boolean tryPickCubeCorner(MinecraftClient mc)
    {
        CornerHit hit = StructurePickerScaleGizmo.findCornerHit(mc, true);

        if (hit == null)
        {
            return false;
        }

        StructurePickerScaleGizmo.resizeGizmoActive = true;
        StructurePickerScaleGizmo.resizeRegionIndex = hit.regionIndex();
        StructurePickerScaleGizmo.resizeFreeCorner = hit.isMax() ? hit.max().toImmutable() : hit.min().toImmutable();
        StructurePickerScaleGizmo.resizeFixedCorner = hit.isMax() ? hit.min().toImmutable() : hit.max().toImmutable();
        StructurePickerScaleGizmo.resizeAnchorIsMax = hit.isMax();
        StructurePickerScaleGizmo.resizeDragAxis = null;
        StructurePickerScaleGizmo.resizeDragging = false;

        return true;
    }

    public static void tick(MinecraftClient mc, boolean leftPressed, boolean leftReleased, boolean leftDown)
    {
        if (StructurePickerClient.getMode() != StructurePickerMode.CUBE)
        {
            return;
        }

        StructurePickerScaleGizmo.ensure();

        if (!StructurePickerScaleGizmo.resizeGizmoActive)
        {
            return;
        }

        if (leftReleased)
        {
            StructurePickerScaleGizmo.resizeDragging = false;
            StructurePickerScaleGizmo.resizeDragAxis = null;

            return;
        }

        if (!leftDown)
        {
            return;
        }

        if (!StructurePickerScaleGizmo.resizeDragging)
        {
            /* Only arm scale on a fresh LMB press — not while holding after a pivot-corner click. */
            if (!leftPressed)
            {
                return;
            }

            StructurePickerAxis axis = StructurePickerScaleGizmo.pickAxisGizmo(mc);

            if (axis == null)
            {
                return;
            }

            StructurePickerScaleGizmo.beginSelectionScaleDrag(mc, axis);

            return;
        }

        StructurePickerScaleGizmo.updateSelectionScaleDrag(mc);
    }

    private static void activateRegionScale(int regionIndex, boolean useMaxCorner)
    {
        StructurePickerClient.Region region = StructurePickerClient.getRegions().get(regionIndex);
        BlockPos min = StructurePickerSelection.min(region.first(), region.second());
        BlockPos max = StructurePickerSelection.max(region.first(), region.second());

        StructurePickerScaleGizmo.resizeGizmoActive = true;
        StructurePickerScaleGizmo.resizeRegionIndex = regionIndex;
        StructurePickerScaleGizmo.resizeFreeCorner = useMaxCorner ? max.toImmutable() : min.toImmutable();
        StructurePickerScaleGizmo.resizeFixedCorner = useMaxCorner ? min.toImmutable() : max.toImmutable();
        StructurePickerScaleGizmo.resizeAnchorIsMax = useMaxCorner;
        StructurePickerScaleGizmo.resizeDragAxis = null;
        StructurePickerScaleGizmo.resizeDragging = false;
    }

    private static void syncScaleCornersFromRegion()
    {
        List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();

        if (StructurePickerScaleGizmo.resizeRegionIndex < 0
            || StructurePickerScaleGizmo.resizeRegionIndex >= regions.size())
        {
            return;
        }

        StructurePickerClient.Region region = regions.get(StructurePickerScaleGizmo.resizeRegionIndex);
        BlockPos min = StructurePickerSelection.min(region.first(), region.second());
        BlockPos max = StructurePickerSelection.max(region.first(), region.second());

        if (StructurePickerScaleGizmo.resizeAnchorIsMax)
        {
            StructurePickerScaleGizmo.resizeFreeCorner = max.toImmutable();
            StructurePickerScaleGizmo.resizeFixedCorner = min.toImmutable();
        }
        else
        {
            StructurePickerScaleGizmo.resizeFreeCorner = min.toImmutable();
            StructurePickerScaleGizmo.resizeFixedCorner = max.toImmutable();
        }
    }

    private record CornerHit(int regionIndex, BlockPos min, BlockPos max, boolean isMax)
    {
    }

    private static CornerHit findCornerHit(MinecraftClient mc, boolean enlarged)
    {
        if (StructurePickerClient.getMode() != StructurePickerMode.CUBE || StructurePickerClient.getRegions().isEmpty())
        {
            return null;
        }

        Vec3d eye = StructurePickerScaleGizmo.getViewEye(mc);
        Vec3d look = StructurePickerScaleGizmo.getViewLook(mc);
        double bestDist = Double.MAX_VALUE;
        CornerHit best = null;
        List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();

        for (int regionIndex = 0; regionIndex < regions.size(); regionIndex++)
        {
            StructurePickerClient.Region region = regions.get(regionIndex);

            if (region.mode() != StructurePickerMode.CUBE)
            {
                continue;
            }

            BlockPos min = StructurePickerSelection.min(region.first(), region.second());
            BlockPos max = StructurePickerSelection.max(region.first(), region.second());
            Vec3d minCorner = new Vec3d(min.getX(), min.getY(), min.getZ());
            Vec3d maxCorner = new Vec3d(max.getX() + 1, max.getY() + 1, max.getZ() + 1);
            float scaleMin = StructurePickerScaleGizmo.getHandleVisualScale(minCorner.x, minCorner.y, minCorner.z);
            float scaleMax = StructurePickerScaleGizmo.getHandleVisualScale(maxCorner.x, maxCorner.y, maxCorner.z);
            double radiusMin;
            double radiusMax;

            if (enlarged)
            {
                radiusMin = Math.max(
                    StructurePickerScaleGizmo.visualPickRadius(StructurePickerScaleGizmo.CORNER_HANDLE * scaleMin * 1.18F, StructurePickerScaleGizmo.CORNER_PICK_PAD),
                    StructurePickerScaleGizmo.screenSpacePickRadius(mc, eye, minCorner, StructurePickerScaleGizmo.CORNER_PICK_MIN_PIXELS)
                );
                radiusMax = Math.max(
                    StructurePickerScaleGizmo.visualPickRadius(StructurePickerScaleGizmo.CORNER_HANDLE * scaleMax * 1.18F, StructurePickerScaleGizmo.CORNER_PICK_PAD),
                    StructurePickerScaleGizmo.screenSpacePickRadius(mc, eye, maxCorner, StructurePickerScaleGizmo.CORNER_PICK_MIN_PIXELS)
                );
            }
            else
            {
                radiusMin = StructurePickerScaleGizmo.visualPickRadius(StructurePickerScaleGizmo.CORNER_HANDLE * scaleMin * 1.18F, StructurePickerScaleGizmo.GIZMO_PICK_PAD);
                radiusMax = StructurePickerScaleGizmo.visualPickRadius(StructurePickerScaleGizmo.CORNER_HANDLE * scaleMax * 1.18F, StructurePickerScaleGizmo.GIZMO_PICK_PAD);
            }

            double distMin = StructurePickerScaleGizmo.distanceRayToPoint(eye, look, minCorner);
            double distMax = StructurePickerScaleGizmo.distanceRayToPoint(eye, look, maxCorner);

            if (distMin < radiusMin && distMin < bestDist)
            {
                bestDist = distMin;
                best = new CornerHit(regionIndex, min, max, false);
            }

            if (distMax < radiusMax && distMax < bestDist)
            {
                bestDist = distMax;
                best = new CornerHit(regionIndex, min, max, true);
            }
        }

        return best;
    }

    private static Vec3d getSelectionGizmoPoint()
    {
        if (StructurePickerScaleGizmo.resizeFreeCorner == null)
        {
            return null;
        }

        if (StructurePickerScaleGizmo.resizeAnchorIsMax)
        {
            return new Vec3d(
                StructurePickerScaleGizmo.resizeFreeCorner.getX() + 1,
                StructurePickerScaleGizmo.resizeFreeCorner.getY() + 1,
                StructurePickerScaleGizmo.resizeFreeCorner.getZ() + 1
            );
        }

        return new Vec3d(
            StructurePickerScaleGizmo.resizeFreeCorner.getX(),
            StructurePickerScaleGizmo.resizeFreeCorner.getY(),
            StructurePickerScaleGizmo.resizeFreeCorner.getZ()
        );
    }

    private static int readSelectionGizmoCoord(StructurePickerAxis axis)
    {
        Vec3d point = StructurePickerScaleGizmo.getSelectionGizmoPoint();

        if (point == null)
        {
            return 0;
        }

        return (int) Math.floor(axis == StructurePickerAxis.X ? point.x : (axis == StructurePickerAxis.Y ? point.y : point.z));
    }

    private static void beginSelectionScaleDrag(MinecraftClient mc, StructurePickerAxis axis)
    {
        StructurePickerScaleGizmo.resizeDragAxis = axis;
        StructurePickerScaleGizmo.resizeDragging = true;

        Vec3d gizmo = StructurePickerScaleGizmo.getSelectionGizmoPoint();
        Vec3d eye = StructurePickerScaleGizmo.getViewEye(mc);
        Vec3d look = StructurePickerScaleGizmo.getViewLook(mc);
        Double hit = gizmo == null ? null : StructurePickerScaleGizmo.projectLookOntoAxis(eye, look, gizmo, axis);

        if (hit != null)
        {
            StructurePickerScaleGizmo.resizeDragOriginCoord = (int) Math.floor(hit);
        }
        else
        {
            StructurePickerScaleGizmo.resizeDragOriginCoord = StructurePickerScaleGizmo.readSelectionGizmoCoord(axis);
        }
    }

    private static StructurePickerAxis pickAxisGizmo(MinecraftClient mc)
    {
        Vec3d gizmo = StructurePickerScaleGizmo.getSelectionGizmoPoint();

        if (gizmo == null)
        {
            return null;
        }

        Vec3d eye = StructurePickerScaleGizmo.getViewEye(mc);
        Vec3d look = StructurePickerScaleGizmo.normalizeLook(StructurePickerScaleGizmo.getViewLook(mc));

        if (look == null)
        {
            return null;
        }

        boolean positive = StructurePickerScaleGizmo.isScalePositive();
        float visualScale = StructurePickerScaleGizmo.getHandleVisualScale(gizmo.x, gizmo.y, gizmo.z);
        double axisLength = StructurePickerScaleGizmo.GIZMO_AXIS_LENGTH * visualScale;
        double tipRadius = StructurePickerScaleGizmo.screenSpacePickRadius(mc, eye, gizmo, StructurePickerScaleGizmo.GIZMO_TIP_PICK_PIXELS);
        double axisRadius = StructurePickerScaleGizmo.screenSpacePickRadius(mc, eye, gizmo, StructurePickerScaleGizmo.GIZMO_STEM_PICK_PIXELS);
        StructurePickerAxis best = null;
        double bestDist = Double.MAX_VALUE;

        for (StructurePickerAxis axis : StructurePickerAxis.values())
        {
            Vec3d shaftEnd = StructurePickerScaleGizmo.axisEnd(gizmo, axis, axisLength, positive);
            double tipPick = StructurePickerScaleGizmo.screenSpacePickRadius(mc, eye, shaftEnd, StructurePickerScaleGizmo.GIZMO_TIP_PICK_PIXELS);
            double stemPick = Math.max(axisRadius, StructurePickerScaleGizmo.screenSpacePickRadius(mc, eye, shaftEnd, StructurePickerScaleGizmo.GIZMO_STEM_PICK_PIXELS));
            double distTip = StructurePickerScaleGizmo.distanceRayToPoint(eye, look, shaftEnd);
            double distSeg = StructurePickerScaleGizmo.distanceRayToSegment(eye, look, gizmo, shaftEnd);
            double dist = Double.MAX_VALUE;

            if (distTip < Math.max(tipRadius, tipPick))
            {
                dist = Math.min(dist, distTip * 0.85D);
            }

            if (distSeg < stemPick)
            {
                dist = Math.min(dist, distSeg);
            }

            if (dist < bestDist)
            {
                bestDist = dist;
                best = axis;
            }
        }

        return best;
    }

    private static void updateSelectionScaleDrag(MinecraftClient mc)
    {
        if (!StructurePickerScaleGizmo.resizeDragging
            || StructurePickerScaleGizmo.resizeDragAxis == null
            || StructurePickerScaleGizmo.resizeFreeCorner == null)
        {
            return;
        }

        List<StructurePickerClient.Region> regions = StructurePickerClient.getRegions();

        if (StructurePickerScaleGizmo.resizeFixedCorner == null
            || StructurePickerScaleGizmo.resizeRegionIndex < 0
            || StructurePickerScaleGizmo.resizeRegionIndex >= regions.size())
        {
            return;
        }

        StructurePickerAxis axis = StructurePickerScaleGizmo.resizeDragAxis;
        Vec3d eye = StructurePickerScaleGizmo.getViewEye(mc);
        Vec3d look = StructurePickerScaleGizmo.getViewLook(mc);
        Vec3d gizmo = StructurePickerScaleGizmo.getSelectionGizmoPoint();

        if (gizmo == null)
        {
            return;
        }

        Double hit = StructurePickerScaleGizmo.projectLookOntoAxis(eye, look, gizmo, axis);

        if (hit == null)
        {
            return;
        }

        int newCoord = (int) Math.floor(hit);
        int delta = newCoord - StructurePickerScaleGizmo.resizeDragOriginCoord;

        if (delta == 0)
        {
            return;
        }

        BlockPos fixed = StructurePickerScaleGizmo.resizeFixedCorner;
        int fixedCoord = axis.read(fixed);
        int nextFree = axis.read(StructurePickerScaleGizmo.resizeFreeCorner) + delta;
        boolean flipped = false;

        /* Past size 1: flip which corner is free so the dragged handle continues
         * through the opposite face (upper ↔ lower / max ↔ min). */
        if (StructurePickerScaleGizmo.resizeAnchorIsMax)
        {
            if (nextFree < fixedCoord)
            {
                StructurePickerScaleGizmo.resizeAnchorIsMax = false;
                flipped = true;
            }
        }
        else if (nextFree > fixedCoord)
        {
            StructurePickerScaleGizmo.resizeAnchorIsMax = true;
            flipped = true;
        }

        BlockPos free = axis.write(StructurePickerScaleGizmo.resizeFreeCorner, nextFree);

        StructurePickerScaleGizmo.resizeDragOriginCoord = newCoord;
        StructurePickerScaleGizmo.resizeFreeCorner = free.toImmutable();

        /* Keep fixed/free aligned with min/max after a flip so gizmo polarity matches. */
        BlockPos min = StructurePickerSelection.min(free, StructurePickerScaleGizmo.resizeFixedCorner);
        BlockPos max = StructurePickerSelection.max(free, StructurePickerScaleGizmo.resizeFixedCorner);

        if (StructurePickerScaleGizmo.resizeAnchorIsMax)
        {
            StructurePickerScaleGizmo.resizeFreeCorner = max.toImmutable();
            StructurePickerScaleGizmo.resizeFixedCorner = min.toImmutable();
        }
        else
        {
            StructurePickerScaleGizmo.resizeFreeCorner = min.toImmutable();
            StructurePickerScaleGizmo.resizeFixedCorner = max.toImmutable();
        }

        if (flipped)
        {
            /* Re-seed from the new gizmo so the next frame does not jump after teleport. */
            Vec3d gizmoAfter = StructurePickerScaleGizmo.getSelectionGizmoPoint();
            Double hitAfter = gizmoAfter == null ? null : StructurePickerScaleGizmo.projectLookOntoAxis(eye, look, gizmoAfter, axis);

            if (hitAfter != null)
            {
                StructurePickerScaleGizmo.resizeDragOriginCoord = (int) Math.floor(hitAfter);
            }
            else
            {
                StructurePickerScaleGizmo.resizeDragOriginCoord = StructurePickerScaleGizmo.readSelectionGizmoCoord(axis);
            }
        }

        StructurePickerClient.Region previous = regions.get(StructurePickerScaleGizmo.resizeRegionIndex);

        StructurePickerClient.replaceRegion(StructurePickerScaleGizmo.resizeRegionIndex, new StructurePickerClient.Region(
            StructurePickerScaleGizmo.resizeFreeCorner,
            StructurePickerScaleGizmo.resizeFixedCorner,
            StructurePickerMode.CUBE,
            previous.triangleFacing()
        ));
    }

    private static Vec3d axisEnd(Vec3d origin, StructurePickerAxis axis, double length, boolean positive)
    {
        double signed = positive ? length : -length;

        return switch (axis)
        {
            case X -> origin.add(signed, 0D, 0D);
            case Y -> origin.add(0D, signed, 0D);
            case Z -> origin.add(0D, 0D, signed);
        };
    }

    private static double distanceRayToPoint(Vec3d eye, Vec3d look, Vec3d point)
    {
        Vec3d toPoint = point.subtract(eye);
        double along = toPoint.dotProduct(look);

        if (along < 0D)
        {
            return Double.MAX_VALUE;
        }

        Vec3d closest = eye.add(look.multiply(along));

        return closest.distanceTo(point);
    }

    private static double distanceRayToSegment(Vec3d eye, Vec3d look, Vec3d a, Vec3d b)
    {
        Vec3d ab = b.subtract(a);
        double abLenSq = ab.lengthSquared();

        if (abLenSq < 1.0E-6D)
        {
            return StructurePickerScaleGizmo.distanceRayToPoint(eye, look, a);
        }

        /* Closest approach between ray (eye + t*look) and segment (a + u*ab). */
        Vec3d ao = a.subtract(eye);
        double lookDotAb = look.dotProduct(ab);
        double lookDotAo = look.dotProduct(ao);
        double abDotAo = ab.dotProduct(ao);
        double denom = 1D - lookDotAb * lookDotAb / abLenSq;

        if (Math.abs(denom) < 1.0E-6D)
        {
            return StructurePickerScaleGizmo.distanceRayToPoint(eye, look, a);
        }

        double t = (lookDotAo - lookDotAb * abDotAo / abLenSq) / denom;
        double u = (abDotAo + t * lookDotAb) / abLenSq;

        t = Math.max(0D, t);
        u = Math.max(0D, Math.min(1D, u));

        Vec3d onRay = eye.add(look.multiply(t));
        Vec3d onSeg = a.add(ab.multiply(u));

        return onRay.distanceTo(onSeg);
    }

    private static Double projectLookOntoAxis(Vec3d eye, Vec3d look, Vec3d origin, StructurePickerAxis axis)
    {
        double axisLook = axis.readLook(look);

        if (Math.abs(axisLook) < 0.02D)
        {
            return null;
        }

        Vec3d planeNormal;

        if (axis == StructurePickerAxis.Y)
        {
            planeNormal = new Vec3d(look.x, 0D, look.z);

            if (planeNormal.lengthSquared() < 1.0E-6D)
            {
                planeNormal = new Vec3d(1D, 0D, 0D);
            }
            else
            {
                planeNormal = planeNormal.normalize();
            }
        }
        else
        {
            planeNormal = new Vec3d(0D, 1D, 0D);
        }

        double denom = look.dotProduct(planeNormal);

        if (Math.abs(denom) < 1.0E-6D)
        {
            double t = (axis.read(BlockPos.ofFloored(origin)) + 0.5D - StructurePickerScaleGizmo.readVec(eye, axis)) / axisLook;

            if (t < 0D)
            {
                return null;
            }

            return StructurePickerScaleGizmo.readVec(eye.add(look.multiply(t)), axis);
        }

        double t = origin.subtract(eye).dotProduct(planeNormal) / denom;

        if (t < 0D)
        {
            return null;
        }

        return StructurePickerScaleGizmo.readVec(eye.add(look.multiply(t)), axis);
    }

    private static double readVec(Vec3d v, StructurePickerAxis axis)
    {
        return switch (axis)
        {
            case X -> v.x;
            case Y -> v.y;
            case Z -> v.z;
        };
    }

    private static Vec3d normalizeLook(Vec3d look)
    {
        double len = look.length();

        if (len < 1.0E-6D)
        {
            return null;
        }

        return look.multiply(1.0D / len);
    }

    private static double screenSpacePickRadius(MinecraftClient mc, Vec3d eye, Vec3d point, double pixels)
    {
        double dist = Math.max(0.35D, eye.distanceTo(point));
        double screenH = Math.max(1, mc.getWindow().getFramebufferHeight());
        double fovDeg = mc.options.getFov().getValue().doubleValue();
        double halfFov = Math.toRadians(fovDeg) * 0.5D;
        double worldPerPixel = (2.0D * dist * Math.tan(halfFov)) / screenH;

        return Math.max(0.22D, worldPerPixel * pixels);
    }

    private static double visualPickRadius(float visualSize, double pad)
    {
        return Math.max(0.12D, visualSize * 0.5D * pad);
    }

    private static Vec3d getViewEye(MinecraftClient mc)
    {
        if (UIStructurePickerPanel.isOpened())
        {
            Vector3d pos = BBSModClient.getCameraController().getPosition();

            return new Vec3d(pos.x, pos.y, pos.z);
        }

        if (mc.player != null)
        {
            return mc.player.getEyePos();
        }

        return Vec3d.ZERO;
    }

    private static Vec3d getViewLook(MinecraftClient mc)
    {
        if (UIStructurePickerPanel.isOpened())
        {
            mchorse.bbs_mod.camera.Camera camera = BBSModClient.getCameraController().camera;
            Vector3f look = Matrices.rotation(camera.rotation.x, MathUtils.PI - camera.rotation.y);

            return new Vec3d(look.x, look.y, look.z);
        }

        if (mc.player != null)
        {
            return mc.player.getRotationVec(1.0F);
        }

        return new Vec3d(0.0D, 0.0D, 1.0D);
    }
}
