package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.items.StructurePickerAxis;
import mchorse.bbs_mod.items.StructurePickerBrushShape;
import mchorse.bbs_mod.items.StructurePickerExporter;
import mchorse.bbs_mod.items.StructurePickerMode;
import mchorse.bbs_mod.items.StructurePickerPlane;
import mchorse.bbs_mod.items.StructurePickerRegionMerger;
import mchorse.bbs_mod.items.StructurePickerSelection;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.items.UIStructurePickerPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class StructurePickerClient
{
    private static final int PLANE_LOCK_MOUSE_THRESHOLD_SQ = 16;

    private static StructurePickerMode mode = StructurePickerMode.CUBE;
    private static final List<Region> regions = new ArrayList<>();
    private static final Set<Integer> selectedRegionIndices = new HashSet<>();
    private static int activeRegionIndex = -1;
    private static int hoveredRegionIndex = -1;
    private static BlockPos firstCorner;
    private static BlockPos secondCorner;
    private static StructurePickerPlane selectionPlane;
    private static boolean depthAdjust;
    private static boolean subtractMode;
    private static StructurePickerAxis depthAxis;
    private static BlockPos slabMin;
    private static BlockPos slabMax;
    private static boolean rightMouseDown;
    private static boolean leftMouseDown;
    /** True only after LMB went down away from corner/scale handles. */
    private static boolean leftSelectArmed;
    private static double planeMouseX;
    private static double planeMouseY;
    private static StructurePickerAxis planeHorizontalAxis;
    private static boolean clickOnAir;
    private static BlockHitResult lastRaycastHit;
    private static BlockPos lastPaintedBlock;
    private static Direction triangleFacing;
    private static boolean undoKeyDown;
    private static boolean redoKeyDown;
    /** Snapshot of regions at the start of an RMB paint stroke (BLOCK / SAME / BRUSH). */
    private static List<Region> paintStrokeBefore;
    private static boolean paintStrokeDirty;
    /** Snapshot of regions at the start of a scale-gizmo drag. */
    private static List<Region> scaleStrokeBefore;
    private static int sameBlockLimit = 100;
    private static int brushRadius = 2;
    private static int brushDepth = 1;
    private static StructurePickerBrushShape brushShape = StructurePickerBrushShape.SPHERE;
    private static BlockPos brushPreviewHover;
    private static Direction brushPreviewFace;
    private static int brushPreviewRadius = Integer.MIN_VALUE;
    private static int brushPreviewDepth = Integer.MIN_VALUE;
    private static StructurePickerBrushShape brushPreviewShape;
    private static List<StructurePickerRegionMerger.MergedRegion> brushPreviewRegions = List.of();

    public static StructurePickerMode getMode()
    {
        return StructurePickerClient.mode;
    }

    public static void setMode(StructurePickerMode mode)
    {
        StructurePickerClient.mode = mode;
        StructurePickerScaleGizmo.clear();

        if (mode != StructurePickerMode.BRUSH)
        {
            StructurePickerClient.clearBrushPreviewCache();
        }
    }

    /**
     * Package helper for {@link StructurePickerScaleGizmo} region mutation.
     */
    static void replaceRegion(int index, Region region)
    {
        StructurePickerClient.regions.set(index, region);
    }

    public static boolean isSubtractMode()
    {
        return StructurePickerClient.subtractMode;
    }

    public static void setSubtractMode(boolean subtractMode)
    {
        StructurePickerClient.subtractMode = subtractMode;
    }

    public static boolean isClickOnAir()
    {
        return StructurePickerClient.clickOnAir;
    }

    public static void setClickOnAir(boolean clickOnAir)
    {
        StructurePickerClient.clickOnAir = clickOnAir;
    }

    public static int getSameBlockLimit()
    {
        return StructurePickerClient.sameBlockLimit;
    }

    public static void setSameBlockLimit(int limit)
    {
        StructurePickerClient.sameBlockLimit = MathUtils.clamp(limit, 1, 500);
    }

    public static int getBrushRadius()
    {
        return StructurePickerClient.brushRadius;
    }

    public static void setBrushRadius(int radius)
    {
        int clamped = MathUtils.clamp(radius, 0, 32);

        if (StructurePickerClient.brushRadius != clamped)
        {
            StructurePickerClient.brushRadius = clamped;
            StructurePickerClient.clearBrushPreviewCache();
        }
    }

    public static int getBrushDepth()
    {
        return StructurePickerClient.brushDepth;
    }

    public static void setBrushDepth(int depth)
    {
        int clamped = MathUtils.clamp(depth, 1, 32);

        if (StructurePickerClient.brushDepth != clamped)
        {
            StructurePickerClient.brushDepth = clamped;
            StructurePickerClient.clearBrushPreviewCache();
        }
    }

    public static StructurePickerBrushShape getBrushShape()
    {
        return StructurePickerClient.brushShape;
    }

    public static void setBrushShape(StructurePickerBrushShape shape)
    {
        StructurePickerBrushShape next = shape == null ? StructurePickerBrushShape.SPHERE : shape;

        if (StructurePickerClient.brushShape != next)
        {
            StructurePickerClient.brushShape = next;
            StructurePickerClient.clearBrushPreviewCache();
        }
    }

    public static List<StructurePickerRegionMerger.MergedRegion> getBrushPreviewRegions()
    {
        if (StructurePickerClient.mode != StructurePickerMode.BRUSH)
        {
            StructurePickerClient.clearBrushPreviewCache();

            return List.of();
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null)
        {
            StructurePickerClient.clearBrushPreviewCache();

            return List.of();
        }

        BlockPos hovered = StructurePickerClient.resolveTargetBlock(mc);

        if (hovered == null)
        {
            StructurePickerClient.clearBrushPreviewCache();

            return List.of();
        }

        Direction face = StructurePickerClient.getBrushFace();
        int radius = StructurePickerClient.getBrushRadius();
        int depth = StructurePickerClient.getBrushDepth();
        StructurePickerBrushShape shape = StructurePickerClient.getBrushShape();

        if (hovered.equals(StructurePickerClient.brushPreviewHover)
            && face == StructurePickerClient.brushPreviewFace
            && radius == StructurePickerClient.brushPreviewRadius
            && depth == StructurePickerClient.brushPreviewDepth
            && shape == StructurePickerClient.brushPreviewShape)
        {
            return StructurePickerClient.brushPreviewRegions;
        }

        List<BlockPos> blocks = StructurePickerSelection.collectBrushSurface(
            mc.world,
            hovered,
            shape,
            radius,
            depth,
            face
        );
        List<StructurePickerRegionMerger.MergedRegion> merged = StructurePickerRegionMerger.merge(blocks);

        StructurePickerClient.brushPreviewHover = hovered.toImmutable();
        StructurePickerClient.brushPreviewFace = face;
        StructurePickerClient.brushPreviewRadius = radius;
        StructurePickerClient.brushPreviewDepth = depth;
        StructurePickerClient.brushPreviewShape = shape;
        StructurePickerClient.brushPreviewRegions = merged;

        return merged;
    }

    private static void clearBrushPreviewCache()
    {
        StructurePickerClient.brushPreviewHover = null;
        StructurePickerClient.brushPreviewFace = null;
        StructurePickerClient.brushPreviewRadius = Integer.MIN_VALUE;
        StructurePickerClient.brushPreviewDepth = Integer.MIN_VALUE;
        StructurePickerClient.brushPreviewShape = null;
        StructurePickerClient.brushPreviewRegions = List.of();
    }

    private static Direction getBrushFace()
    {
        if (StructurePickerClient.lastRaycastHit != null)
        {
            return StructurePickerClient.lastRaycastHit.getSide();
        }

        return Direction.UP;
    }

    public static Direction getTriangleFacing()
    {
        return StructurePickerClient.triangleFacing;
    }

    public static List<Region> getRegions()
    {
        return StructurePickerClient.regions;
    }

    public static int getActiveRegionIndex()
    {
        return StructurePickerClient.activeRegionIndex;
    }

    public static int getHoveredRegionIndex()
    {
        return StructurePickerClient.hoveredRegionIndex;
    }

    public static boolean isRegionSelected(int index)
    {
        return StructurePickerClient.selectedRegionIndices.contains(index);
    }

    public static boolean hasRegionSelection()
    {
        return !StructurePickerClient.selectedRegionIndices.isEmpty();
    }

    /**
     * Marks a region selected and active (for gizmos). Does not clear other selections.
     */
    public static void activateAndSelectRegion(int index)
    {
        if (index < 0 || index >= StructurePickerClient.regions.size())
        {
            return;
        }

        StructurePickerClient.selectedRegionIndices.add(index);
        StructurePickerClient.activeRegionIndex = index;
    }

    public static BlockPos getFirstCorner()
    {
        return StructurePickerClient.firstCorner;
    }

    public static BlockPos getSecondCorner()
    {
        return StructurePickerClient.secondCorner;
    }

    public static boolean hasInProgress()
    {
        return StructurePickerClient.firstCorner != null && StructurePickerClient.secondCorner != null;
    }

    public static boolean hasAnySelection()
    {
        return !StructurePickerClient.regions.isEmpty() || StructurePickerClient.hasInProgress();
    }

    public static boolean hasBlockSelection()
    {
        return StructurePickerClient.mode.isPaintMode() && !StructurePickerClient.regions.isEmpty();
    }

    public static Set<BlockPos> getAllRegionBlocks()
    {
        Set<BlockPos> blocks = new LinkedHashSet<>();

        for (StructurePickerClient.Region region : StructurePickerClient.regions)
        {
            blocks.addAll(StructurePickerSelection.preview(null, region.first(), region.second(), region.mode(), region.triangleFacing()));
        }

        return blocks;
    }

    private static void setRegionsFromBlocks(Set<BlockPos> blocks)
    {
        StructurePickerClient.regions.clear();
        StructurePickerClient.selectedRegionIndices.clear();
        StructurePickerClient.activeRegionIndex = -1;

        for (StructurePickerRegionMerger.MergedRegion merged : StructurePickerRegionMerger.merge(blocks))
        {
            StructurePickerClient.regions.add(new Region(merged.min(), merged.max(), merged.mode()));
        }

        for (int i = 0; i < StructurePickerClient.regions.size(); i++)
        {
            StructurePickerClient.selectedRegionIndices.add(i);
        }

        if (!StructurePickerClient.regions.isEmpty())
        {
            StructurePickerClient.activeRegionIndex = StructurePickerClient.regions.size() - 1;
        }

        StructurePickerScaleGizmo.clear();
    }

    public static boolean isActive()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null)
        {
            return false;
        }

        ItemStack stack = mc.player.getMainHandStack();

        return stack.getItem() == BBSMod.STRUCTURE_PICKER_ITEM;
    }

    public static ActionResult onUseBlock(BlockHitResult hitResult, boolean sneaking)
    {
        if (!StructurePickerClient.isActive())
        {
            return ActionResult.PASS;
        }

        return ActionResult.SUCCESS;
    }

    public static void openPanel()
    {
        StructurePickerClient.endPaintStroke();
        StructurePickerClient.finalizeInProgress();
        UIStructurePickerPanel.open();
    }

    public static ActionResult onAttackBlock()
    {
        if (!StructurePickerClient.isActive())
        {
            return ActionResult.PASS;
        }

        /* Cancel vanilla break; region select / erase is handled in tick(). */
        return ActionResult.SUCCESS;
    }

    public static void tick(MinecraftClient mc)
    {
        StructurePickerClient.tickUndoRedoKeys();

        if (mc.world == null || mc.player == null)
        {
            StructurePickerClient.clearSelection();

            return;
        }

        boolean rightDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean released = !rightDown && StructurePickerClient.rightMouseDown;
        boolean leftDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean leftPressed = leftDown && !StructurePickerClient.leftMouseDown;
        boolean leftReleased = !leftDown && StructurePickerClient.leftMouseDown;

        StructurePickerClient.rightMouseDown = rightDown;
        StructurePickerClient.leftMouseDown = leftDown;

        if (UIStructurePickerPanel.isOpened() || mc.currentScreen != null || !StructurePickerClient.isActive())
        {
            StructurePickerClient.leftSelectArmed = false;
            StructurePickerClient.hoveredRegionIndex = -1;
            StructurePickerClient.endPaintStroke();

            return;
        }

        /* Shift (sneak) is used for multi-select — do not clear hover or bail out early. */
        StructurePickerClient.hoveredRegionIndex = StructurePickerClient.findClosestRegionHit(mc);

        if (StructurePickerScaleGizmo.hasScalableSelection())
        {
            StructurePickerScaleGizmo.ensure();
        }

        if (leftPressed && !rightDown)
        {
            if (StructurePickerScaleGizmo.isOverSelectionInteractable(mc))
            {
                /* Corner / scale gizmo owns LMB — never select-body or erase. */
                StructurePickerClient.leftSelectArmed = false;

                if (StructurePickerScaleGizmo.isOverSelectionCorner(mc))
                {
                    StructurePickerScaleGizmo.tryPickCubeCorner(mc);
                }
            }
            else
            {
                StructurePickerClient.leftSelectArmed = true;
            }
        }

        if (leftReleased)
        {
            boolean shouldHandleSelect = StructurePickerClient.leftSelectArmed
                && !rightDown
                && !StructurePickerScaleGizmo.isDragging()
                && !StructurePickerScaleGizmo.isOverSelectionInteractable(mc);

            StructurePickerClient.leftSelectArmed = false;
            StructurePickerScaleGizmo.tick(mc, leftPressed, leftReleased, leftDown);

            if (shouldHandleSelect)
            {
                StructurePickerClient.handleLeftClickSelect(mc);

                return;
            }
        }
        else if (leftDown)
        {
            StructurePickerScaleGizmo.tick(mc, leftPressed, leftReleased, leftDown);
        }

        if (StructurePickerClient.mode.isPaintMode())
        {
            if (rightDown)
            {
                StructurePickerClient.beginPaintStroke();
                StructurePickerClient.updateBlockPaint(mc);
            }
            else if (released)
            {
                StructurePickerClient.endPaintStroke();
                StructurePickerClient.lastPaintedBlock = null;
            }

            return;
        }

        if (StructurePickerClient.mode.isEraseMode())
        {
            if (rightDown)
            {
                StructurePickerClient.updateErasePaint(mc);
            }

            return;
        }

        if (StructurePickerClient.firstCorner != null && !StructurePickerClient.depthAdjust)
        {
            StructurePickerClient.updatePlaneSelection(mc);
        }

        if (StructurePickerClient.depthAdjust && StructurePickerClient.selectionPlane != null)
        {
            StructurePickerClient.updateDepthSelection(mc);
        }

        if (released)
        {
            /* Sneak+RMB opens the panel when not mid-draw (Shift multi-select stays usable). */
            if (mc.player.isSneaking() && !StructurePickerClient.hasInProgress())
            {
                StructurePickerClient.openPanel();
            }
            else
            {
                StructurePickerClient.handleClick(mc);
            }
        }
    }

    private static void updatePlaneSelection(MinecraftClient mc)
    {
        StructurePickerClient.tryLockPlane(mc);
        StructurePickerClient.ensureSelectionPlane(mc);

        Vec3d look = mc.player.getRotationVec(1.0F);
        BlockPos target = StructurePickerClient.resolvePlaneTarget(mc, look);

        if (target == null)
        {
            return;
        }

        if (StructurePickerClient.selectionPlane == null)
        {
            StructurePickerClient.secondCorner = target.toImmutable();

            return;
        }

        if (StructurePickerClient.selectionPlane == StructurePickerPlane.VERTICAL && StructurePickerClient.planeHorizontalAxis == null)
        {
            StructurePickerClient.planeHorizontalAxis = StructurePickerClient.resolveVerticalPlaneAxis(mc, look);
        }

        StructurePickerClient.secondCorner = StructurePickerClient.selectionPlane.clampSecond(
            StructurePickerClient.firstCorner,
            target,
            StructurePickerClient.planeHorizontalAxis
        );
    }

    private static BlockPos resolvePlaneTarget(MinecraftClient mc, Vec3d look)
    {
        BlockPos hovered = StructurePickerClient.resolveTargetBlock(mc);

        if (hovered != null)
        {
            return hovered;
        }

        if (StructurePickerClient.selectionPlane == null)
        {
            return null;
        }

        return switch (StructurePickerClient.selectionPlane)
        {
            case XZ -> StructurePickerClient.raycastHorizontalPlane(mc, StructurePickerClient.firstCorner.getY() + 0.5D);
            case VERTICAL -> StructurePickerClient.raycastVerticalPlane(mc, StructurePickerClient.firstCorner, StructurePickerClient.planeHorizontalAxis);
        };
    }

    private static BlockPos raycastHorizontalPlane(MinecraftClient mc, double planeY)
    {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);

        if (Math.abs(look.y) < 0.001D)
        {
            return null;
        }

        double distance = (planeY - eye.y) / look.y;
        double reach = StructurePickerClient.getPickerReach(mc);

        if (distance < 0D)
        {
            return null;
        }

        if (distance > reach)
        {
            distance = reach;
        }

        Vec3d hit = eye.add(look.multiply(distance));

        return BlockPos.ofFloored(hit);
    }

    private static BlockPos raycastVerticalPlane(MinecraftClient mc, BlockPos anchor, StructurePickerAxis lockedHorizontal)
    {
        Vec3d eye = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVec(1.0F);
        double reach = StructurePickerClient.getPickerReach(mc);

        if (lockedHorizontal == StructurePickerAxis.X)
        {
            if (Math.abs(dir.z) < 0.001D)
            {
                return null;
            }

            double distance = (anchor.getZ() + 0.5D - eye.z) / dir.z;

            if (distance < 0D)
            {
                return null;
            }

            if (distance > reach)
            {
                distance = reach;
            }

            return BlockPos.ofFloored(eye.add(dir.multiply(distance)));
        }

        if (Math.abs(dir.x) < 0.001D)
        {
            return null;
        }

        double distance = (anchor.getX() + 0.5D - eye.x) / dir.x;

        if (distance < 0D)
        {
            return null;
        }

        if (distance > reach)
        {
            distance = reach;
        }

        return BlockPos.ofFloored(eye.add(dir.multiply(distance)));
    }

    private static StructurePickerAxis resolveVerticalPlaneAxis(MinecraftClient mc, Vec3d look)
    {
        StructurePickerAxis along = StructurePickerClient.resolveLookHorizontalAxis(mc, look);

        return along == StructurePickerAxis.X ? StructurePickerAxis.Z : StructurePickerAxis.X;
    }

    private static StructurePickerAxis resolveLookHorizontalAxis(MinecraftClient mc, Vec3d look)
    {
        if (Math.abs(look.x) >= 0.1D || Math.abs(look.z) >= 0.1D)
        {
            return StructurePickerAxis.pickHorizontal(look);
        }

        float yaw = mc.player.getYaw() * ((float) Math.PI / 180F);
        double facingX = -Math.sin(yaw);
        double facingZ = Math.cos(yaw);

        return Math.abs(facingX) >= Math.abs(facingZ) ? StructurePickerAxis.X : StructurePickerAxis.Z;
    }

    private static void applyPlaneFromFace(Direction face)
    {
        switch (face)
        {
            case UP, DOWN -> StructurePickerClient.selectionPlane = StructurePickerPlane.XZ;
            case NORTH, SOUTH ->
            {
                StructurePickerClient.selectionPlane = StructurePickerPlane.VERTICAL;
                StructurePickerClient.planeHorizontalAxis = StructurePickerAxis.X;
            }
            case EAST, WEST ->
            {
                StructurePickerClient.selectionPlane = StructurePickerPlane.VERTICAL;
                StructurePickerClient.planeHorizontalAxis = StructurePickerAxis.Z;
            }
        }
    }

    private static void applyPlaneFromLook(MinecraftClient mc)
    {
        Vec3d look = mc.player.getRotationVec(1.0F);
        double absX = Math.abs(look.x);
        double absY = Math.abs(look.y);
        double absZ = Math.abs(look.z);

        if (absY > absX && absY > absZ)
        {
            StructurePickerClient.selectionPlane = StructurePickerPlane.XZ;
        }
        else
        {
            StructurePickerClient.selectionPlane = StructurePickerPlane.VERTICAL;
            StructurePickerClient.planeHorizontalAxis = StructurePickerClient.resolveVerticalPlaneAxis(mc, look);
        }
    }

    private static void ensureSelectionPlane(MinecraftClient mc)
    {
        if (StructurePickerClient.selectionPlane != null)
        {
            return;
        }

        StructurePickerClient.applyPlaneFromLook(mc);

        if (StructurePickerClient.selectionPlane == StructurePickerPlane.VERTICAL && StructurePickerClient.planeHorizontalAxis == null)
        {
            StructurePickerClient.planeHorizontalAxis = StructurePickerClient.resolveVerticalPlaneAxis(mc, mc.player.getRotationVec(1.0F));
        }
    }

    private static void tryLockPlane(MinecraftClient mc)
    {
        if (StructurePickerClient.selectionPlane != null)
        {
            return;
        }

        double[] cursorX = new double[1];
        double[] cursorY = new double[1];

        GLFW.glfwGetCursorPos(mc.getWindow().getHandle(), cursorX, cursorY);

        double dx = cursorX[0] - StructurePickerClient.planeMouseX;
        double dy = cursorY[0] - StructurePickerClient.planeMouseY;

        if (dx * dx + dy * dy < PLANE_LOCK_MOUSE_THRESHOLD_SQ)
        {
            return;
        }

        StructurePickerClient.selectionPlane = StructurePickerPlane.fromMouseDrag(dx, dy);

        if (StructurePickerClient.selectionPlane == StructurePickerPlane.VERTICAL)
        {
            StructurePickerClient.planeHorizontalAxis = StructurePickerClient.resolveVerticalPlaneAxis(mc, mc.player.getRotationVec(1.0F));
        }
    }

    private static void updateDepthSelection(MinecraftClient mc)
    {
        if (StructurePickerClient.slabMin == null || StructurePickerClient.slabMax == null || StructurePickerClient.selectionPlane == null || StructurePickerClient.depthAxis == null)
        {
            return;
        }

        int depth = StructurePickerClient.resolveDepthCoord(mc);
        BlockPos[] corners = new BlockPos[2];

        StructurePickerClient.selectionPlane.applyDepth(StructurePickerClient.slabMin, StructurePickerClient.slabMax, StructurePickerClient.depthAxis, depth, corners);
        StructurePickerClient.firstCorner = corners[0];
        StructurePickerClient.secondCorner = corners[1];
    }

    private static int resolveDepthCoord(MinecraftClient mc)
    {
        BlockPos hit = StructurePickerClient.resolveTargetBlock(mc);

        if (hit != null)
        {
            return StructurePickerClient.depthAxis.read(hit);
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        double reach = StructurePickerClient.getPickerReach(mc);
        Vec3d end = eye.add(look.multiply(reach));
        Box box = StructurePickerClient.getDepthRayBox(StructurePickerClient.depthAxis, StructurePickerClient.slabMin, StructurePickerClient.slabMax, reach);
        Optional<Vec3d> intersection = box.raycast(eye, end);

        if (intersection.isPresent())
        {
            return StructurePickerClient.depthAxis.read(BlockPos.ofFloored(intersection.get()));
        }

        return Math.min(
            StructurePickerClient.depthAxis.read(StructurePickerClient.slabMin),
            StructurePickerClient.depthAxis.read(StructurePickerClient.slabMax)
        );
    }

    private static Box getDepthRayBox(StructurePickerAxis axis, BlockPos slabMin, BlockPos slabMax, double margin)
    {
        BlockPos min = StructurePickerSelection.min(slabMin, slabMax);
        BlockPos max = StructurePickerSelection.max(slabMin, slabMax);

        return switch (axis)
        {
            case X -> new Box(min.getX() - margin, min.getY(), min.getZ(), max.getX() + margin + 1D, max.getY() + 1D, max.getZ() + 1D);
            case Y -> new Box(min.getX(), min.getY() - margin, min.getZ(), max.getX() + 1D, max.getY() + margin + 1D, max.getZ() + 1D);
            case Z -> new Box(min.getX(), min.getY(), min.getZ() - margin, max.getX() + 1D, max.getY() + 1D, max.getZ() + margin + 1D);
        };
    }

    private static void updateBlockPaint(MinecraftClient mc)
    {
        BlockPos hovered = StructurePickerClient.resolveTargetBlock(mc);

        if (hovered == null)
        {
            return;
        }

        if (StructurePickerClient.lastPaintedBlock != null)
        {
            if (hovered.equals(StructurePickerClient.lastPaintedBlock))
            {
                return;
            }

            for (BlockPos pos : StructurePickerClient.iterateBlockLine(StructurePickerClient.lastPaintedBlock, hovered))
            {
                StructurePickerClient.applyBlockPaint(pos);
            }

            StructurePickerClient.lastPaintedBlock = hovered.toImmutable();

            return;
        }

        StructurePickerClient.applyBlockPaint(hovered);
        StructurePickerClient.lastPaintedBlock = hovered.toImmutable();
    }

    /**
     * Erase mode: while RMB is held, delete each hovered volume region under the crosshair
     * (one region per tick) so sweeping across areas clears many selections comfortably.
     */
    private static void updateErasePaint(MinecraftClient mc)
    {
        int hitIndex = StructurePickerClient.hoveredRegionIndex;

        if (hitIndex < 0)
        {
            hitIndex = StructurePickerClient.findClosestRegionHit(mc);
        }

        if (hitIndex >= 0)
        {
            StructurePickerClient.deleteRegionAt(hitIndex);
        }
    }

    private static void applyBlockPaint(BlockPos pos)
    {
        if (StructurePickerClient.mode == StructurePickerMode.SAME)
        {
            StructurePickerClient.applySameBlockPaint(pos);

            return;
        }

        if (StructurePickerClient.mode == StructurePickerMode.BRUSH)
        {
            StructurePickerClient.applyBrushPaint(pos);

            return;
        }

        if (StructurePickerClient.subtractMode)
        {
            StructurePickerClient.applySubtract(pos, pos, StructurePickerMode.BLOCK);
        }
        else if (!StructurePickerClient.isBlockSelected(pos))
        {
            StructurePickerClient.addPaintBlocks(List.of(pos));
        }
    }

    private static void applyBrushPaint(BlockPos origin)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;

        if (world == null)
        {
            return;
        }

        List<BlockPos> stamped = StructurePickerSelection.collectBrushSurface(
            world,
            origin,
            StructurePickerClient.getBrushShape(),
            StructurePickerClient.getBrushRadius(),
            StructurePickerClient.getBrushDepth(),
            StructurePickerClient.getBrushFace()
        );

        if (stamped.isEmpty())
        {
            return;
        }

        if (StructurePickerClient.subtractMode)
        {
            StructurePickerClient.removePaintBlocks(stamped);

            return;
        }

        StructurePickerClient.addPaintBlocks(stamped);
    }

    private static void applySameBlockPaint(BlockPos origin)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;

        if (world == null)
        {
            return;
        }

        List<BlockPos> connected = StructurePickerSelection.collectConnectedSame(world, origin, StructurePickerClient.getSameBlockLimit());

        if (connected.isEmpty())
        {
            return;
        }

        if (StructurePickerClient.subtractMode)
        {
            StructurePickerClient.removePaintBlocks(connected);

            return;
        }

        StructurePickerClient.addPaintBlocks(connected);
    }

    private static void addPaintBlocks(Collection<BlockPos> stamped)
    {
        Set<BlockPos> blocks = StructurePickerClient.getAllRegionBlocks();
        boolean addedAny = false;

        for (BlockPos pos : stamped)
        {
            if (blocks.add(pos.toImmutable()))
            {
                addedAny = true;
            }
        }

        if (addedAny)
        {
            StructurePickerClient.setRegionsFromBlocks(blocks);
            StructurePickerClient.markPaintStrokeDirty();
        }
    }

    private static void removePaintBlocks(Collection<BlockPos> stamped)
    {
        Set<BlockPos> blocks = StructurePickerClient.getAllRegionBlocks();
        boolean removedAny = false;

        for (BlockPos pos : stamped)
        {
            if (blocks.remove(pos))
            {
                removedAny = true;
            }
        }

        if (removedAny)
        {
            StructurePickerClient.setRegionsFromBlocks(blocks);
            StructurePickerClient.markPaintStrokeDirty();
        }
    }

    private static boolean isBlockSelected(BlockPos pos)
    {
        return StructurePickerClient.getAllRegionBlocks().contains(pos);
    }

    private static List<BlockPos> iterateBlockLine(BlockPos from, BlockPos to)
    {
        List<BlockPos> line = new ArrayList<>();
        int x0 = from.getX();
        int y0 = from.getY();
        int z0 = from.getZ();
        int x1 = to.getX();
        int y1 = to.getY();
        int z1 = to.getZ();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int dm = Math.max(dx, Math.max(dy, dz));

        if (dm == 0)
        {
            line.add(to.toImmutable());

            return line;
        }

        for (int step = 1; step <= dm; step++)
        {
            int x = x0 + (dx == 0 ? 0 : (int) Math.round((double) dx * step / dm) * sx);
            int y = y0 + (dy == 0 ? 0 : (int) Math.round((double) dy * step / dm) * sy);
            int z = z0 + (dz == 0 ? 0 : (int) Math.round((double) dz * step / dm) * sz);

            line.add(new BlockPos(x, y, z));
        }

        return line;
    }

    private static void handleClick(MinecraftClient mc)
    {
        if (StructurePickerClient.depthAdjust)
        {
            StructurePickerClient.commitRegion();

            return;
        }

        BlockPos hovered = StructurePickerClient.resolveTargetBlock(mc);

        if (hovered == null)
        {
            return;
        }

        if (StructurePickerClient.mode.isSingleClick())
        {
            return;
        }

        if (StructurePickerClient.firstCorner == null)
        {
            StructurePickerClient.beginPlaneSelection(mc, hovered);

            return;
        }

        StructurePickerClient.tryLockPlane(mc);

        StructurePickerClient.ensureSelectionPlane(mc);

        if (StructurePickerClient.selectionPlane == StructurePickerPlane.VERTICAL && StructurePickerClient.planeHorizontalAxis == null)
        {
            StructurePickerClient.planeHorizontalAxis = StructurePickerClient.resolveVerticalPlaneAxis(mc, mc.player.getRotationVec(1.0F));
        }

        StructurePickerClient.secondCorner = StructurePickerClient.selectionPlane.clampSecond(
            StructurePickerClient.firstCorner,
            hovered,
            StructurePickerClient.planeHorizontalAxis
        );

        if (StructurePickerClient.mode.isFlat())
        {
            StructurePickerClient.commitRegion();
        }
        else
        {
            StructurePickerClient.beginDepthSelection(mc);
        }
    }

    private static void handleLeftClickSelect(MinecraftClient mc)
    {
        if (StructurePickerClient.hasInProgress())
        {
            return;
        }

        int hitIndex = StructurePickerClient.findClosestRegionHit(mc);

        if (hitIndex >= 0)
        {
            if (Window.isShiftPressed())
            {
                if (StructurePickerClient.selectedRegionIndices.contains(hitIndex))
                {
                    StructurePickerClient.selectedRegionIndices.remove(hitIndex);

                    if (StructurePickerClient.activeRegionIndex == hitIndex)
                    {
                        StructurePickerClient.activeRegionIndex = StructurePickerClient.pickFallbackActiveIndex();
                    }
                }
                else
                {
                    StructurePickerClient.selectedRegionIndices.add(hitIndex);
                    StructurePickerClient.activeRegionIndex = hitIndex;
                }
            }
            else
            {
                StructurePickerClient.selectedRegionIndices.clear();
                StructurePickerClient.selectedRegionIndices.add(hitIndex);
                StructurePickerClient.activeRegionIndex = hitIndex;
            }

            StructurePickerScaleGizmo.ensure();

            return;
        }

        /* LMB on air / miss */
        if (!Window.isShiftPressed() && StructurePickerClient.hasRegionSelection())
        {
            StructurePickerClient.selectedRegionIndices.clear();
            StructurePickerClient.activeRegionIndex = -1;
            StructurePickerScaleGizmo.clear();
        }
    }

    private static int pickFallbackActiveIndex()
    {
        int best = -1;

        for (Integer index : StructurePickerClient.selectedRegionIndices)
        {
            if (index != null && index > best)
            {
                best = index;
            }
        }

        return best;
    }

    private static void deleteRegionAt(int index)
    {
        if (index < 0 || index >= StructurePickerClient.regions.size())
        {
            return;
        }

        List<Region> before = StructurePickerClient.copyRegions();

        StructurePickerClient.regions.remove(index);

        Set<Integer> nextSelected = new HashSet<>();

        for (Integer selected : StructurePickerClient.selectedRegionIndices)
        {
            if (selected == null || selected == index)
            {
                continue;
            }

            nextSelected.add(selected > index ? selected - 1 : selected);
        }

        StructurePickerClient.selectedRegionIndices.clear();
        StructurePickerClient.selectedRegionIndices.addAll(nextSelected);

        if (StructurePickerClient.activeRegionIndex == index)
        {
            StructurePickerClient.activeRegionIndex = StructurePickerClient.pickFallbackActiveIndex();
        }
        else if (StructurePickerClient.activeRegionIndex > index)
        {
            StructurePickerClient.activeRegionIndex -= 1;
        }

        if (StructurePickerClient.hoveredRegionIndex == index)
        {
            StructurePickerClient.hoveredRegionIndex = -1;
        }
        else if (StructurePickerClient.hoveredRegionIndex > index)
        {
            StructurePickerClient.hoveredRegionIndex -= 1;
        }

        StructurePickerScaleGizmo.ensure();
        StructurePickerHistory.push(new RegionsChangeEntry(before, StructurePickerClient.copyRegions()));
    }

    private static int findClosestRegionHit(MinecraftClient mc)
    {
        if (mc.player == null || StructurePickerClient.regions.isEmpty())
        {
            return -1;
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        double lookLen = look.length();

        if (lookLen < 1.0E-6D)
        {
            return -1;
        }

        Vec3d dir = look.multiply(1.0D / lookLen);
        double reach = StructurePickerClient.getPickerReach(mc);
        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < StructurePickerClient.regions.size(); i++)
        {
            Region region = StructurePickerClient.regions.get(i);
            BlockPos adjusted = StructurePickerSelection.adjustSecond(region.first(), region.second(), region.mode());
            BlockPos min = StructurePickerSelection.min(region.first(), adjusted);
            BlockPos max = StructurePickerSelection.max(region.first(), adjusted);
            Double hitDist = StructurePickerClient.rayAabbDistance(eye, dir, reach, min, max);

            if (hitDist != null && hitDist < bestDist)
            {
                bestDist = hitDist;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static Double rayAabbDistance(Vec3d eye, Vec3d dir, double reach, BlockPos min, BlockPos max)
    {
        Box box = new Box(
            min.getX(),
            min.getY(),
            min.getZ(),
            max.getX() + 1D,
            max.getY() + 1D,
            max.getZ() + 1D
        );

        if (box.contains(eye))
        {
            return 0D;
        }

        Vec3d end = eye.add(dir.multiply(reach));
        Optional<Vec3d> hit = box.raycast(eye, end);

        if (hit.isEmpty())
        {
            return null;
        }

        return eye.distanceTo(hit.get());
    }

    private static void beginPlaneSelection(MinecraftClient mc, BlockPos hovered)
    {
        StructurePickerClient.firstCorner = hovered.toImmutable();
        StructurePickerClient.secondCorner = hovered.toImmutable();
        StructurePickerClient.selectionPlane = null;
        StructurePickerClient.planeHorizontalAxis = null;
        StructurePickerClient.depthAdjust = false;
        StructurePickerClient.slabMin = null;
        StructurePickerClient.slabMax = null;

        if (StructurePickerClient.mode == StructurePickerMode.TRIANGLE)
        {
            StructurePickerClient.triangleFacing = mc.player.getHorizontalFacing();
        }
        else
        {
            StructurePickerClient.triangleFacing = null;
        }

        if (!StructurePickerClient.mode.isSingleClick())
        {
            if (StructurePickerClient.lastRaycastHit != null && StructurePickerClient.lastRaycastHit.getType() == HitResult.Type.BLOCK)
            {
                StructurePickerClient.applyPlaneFromFace(StructurePickerClient.lastRaycastHit.getSide());
            }
            else
            {
                StructurePickerClient.applyPlaneFromLook(mc);
            }
        }

        double[] cursorX = new double[1];
        double[] cursorY = new double[1];

        GLFW.glfwGetCursorPos(mc.getWindow().getHandle(), cursorX, cursorY);
        StructurePickerClient.planeMouseX = cursorX[0];
        StructurePickerClient.planeMouseY = cursorY[0];
    }

    private static void beginDepthSelection(MinecraftClient mc)
    {
        Vec3d look = mc.player.getRotationVec(1.0F);

        StructurePickerClient.slabMin = StructurePickerSelection.min(StructurePickerClient.firstCorner, StructurePickerClient.secondCorner);
        StructurePickerClient.slabMax = StructurePickerSelection.max(StructurePickerClient.firstCorner, StructurePickerClient.secondCorner);
        StructurePickerClient.depthAdjust = true;
        StructurePickerClient.depthAxis = StructurePickerClient.selectionPlane == StructurePickerPlane.VERTICAL
            ? StructurePickerClient.verticalDepthAxis(StructurePickerClient.planeHorizontalAxis)
            : StructurePickerAxis.Y;

        if (StructurePickerClient.depthAxis == null)
        {
            StructurePickerClient.depthAxis = StructurePickerClient.selectionPlane.defaultDepthAxis(look);
        }

        StructurePickerClient.updateDepthSelection(mc);
    }

    private static StructurePickerAxis verticalDepthAxis(StructurePickerAxis planeLockedHorizontal)
    {
        if (planeLockedHorizontal == StructurePickerAxis.X)
        {
            return StructurePickerAxis.Z;
        }

        if (planeLockedHorizontal == StructurePickerAxis.Z)
        {
            return StructurePickerAxis.X;
        }

        return StructurePickerAxis.Y;
    }

    private static void commitRegion()
    {
        if (!StructurePickerClient.hasInProgress())
        {
            StructurePickerClient.clearInProgress();

            return;
        }

        List<Region> before = StructurePickerClient.copyRegions();

        if (StructurePickerClient.subtractMode)
        {
            StructurePickerClient.applySubtract(StructurePickerClient.firstCorner, StructurePickerClient.secondCorner, StructurePickerClient.mode);
        }
        else
        {
            StructurePickerClient.regions.add(new Region(
                StructurePickerClient.firstCorner,
                StructurePickerClient.secondCorner,
                StructurePickerClient.mode,
                StructurePickerClient.triangleFacing
            ));

            int index = StructurePickerClient.regions.size() - 1;

            StructurePickerClient.selectedRegionIndices.add(index);
            StructurePickerClient.activeRegionIndex = index;

            if (StructurePickerScaleGizmo.isScalableMode(StructurePickerClient.mode))
            {
                StructurePickerScaleGizmo.ensure();
            }
        }

        StructurePickerClient.clearInProgress();

        List<Region> after = StructurePickerClient.copyRegions();

        if (!before.equals(after))
        {
            StructurePickerHistory.push(new RegionsChangeEntry(before, after));
        }
    }

    private static void applySubtract(BlockPos first, BlockPos second, StructurePickerMode mode)
    {
        Set<BlockPos> removeBlocks = new LinkedHashSet<>(StructurePickerSelection.preview(null, first, second, mode, StructurePickerClient.triangleFacing));
        Set<BlockPos> remaining = new LinkedHashSet<>();
        boolean removedAny = false;

        for (Region region : StructurePickerClient.regions)
        {
            for (BlockPos pos : StructurePickerSelection.preview(null, region.first(), region.second(), region.mode(), region.triangleFacing()))
            {
                if (removeBlocks.contains(pos))
                {
                    removedAny = true;
                }
                else
                {
                    remaining.add(pos);
                }
            }
        }

        if (!removedAny)
        {
            return;
        }

        StructurePickerClient.regions.clear();

        StructurePickerClient.setRegionsFromBlocks(remaining);
        StructurePickerClient.markPaintStrokeDirty();
    }

    private static void finalizeInProgress()
    {
        if (StructurePickerClient.depthAdjust)
        {
            StructurePickerClient.commitRegion();
        }
        else
        {
            StructurePickerClient.clearInProgress();
        }
    }

    private static void clearInProgress()
    {
        StructurePickerClient.firstCorner = null;
        StructurePickerClient.secondCorner = null;
        StructurePickerClient.selectionPlane = null;
        StructurePickerClient.planeHorizontalAxis = null;
        StructurePickerClient.depthAdjust = false;
        StructurePickerClient.depthAxis = null;
        StructurePickerClient.slabMin = null;
        StructurePickerClient.slabMax = null;
        StructurePickerClient.triangleFacing = null;
    }

    public static void clearSelection()
    {
        StructurePickerClient.discardPaintStroke();
        StructurePickerClient.scaleStrokeBefore = null;
        StructurePickerClient.regions.clear();
        StructurePickerClient.selectedRegionIndices.clear();
        StructurePickerClient.activeRegionIndex = -1;
        StructurePickerClient.hoveredRegionIndex = -1;
        StructurePickerClient.lastPaintedBlock = null;
        StructurePickerClient.clearBrushPreviewCache();
        StructurePickerClient.clearInProgress();
        StructurePickerScaleGizmo.clear();
    }

    public static void removeSelection()
    {
        if (StructurePickerClient.regions.isEmpty() && !StructurePickerClient.hasInProgress())
        {
            return;
        }

        List<Region> previous = StructurePickerClient.copyRegions();

        StructurePickerClient.clearSelection();
        StructurePickerHistory.push(new RemoveSelectionEntry(previous));
    }

    public static List<Region> copyRegions()
    {
        List<Region> copy = new ArrayList<>(StructurePickerClient.regions.size());

        for (Region region : StructurePickerClient.regions)
        {
            copy.add(new Region(
                region.first().toImmutable(),
                region.second().toImmutable(),
                region.mode(),
                region.triangleFacing()
            ));
        }

        return copy;
    }

    private static void beginPaintStroke()
    {
        if (StructurePickerClient.paintStrokeBefore != null)
        {
            return;
        }

        StructurePickerClient.paintStrokeBefore = StructurePickerClient.copyRegions();
        StructurePickerClient.paintStrokeDirty = false;
    }

    private static void markPaintStrokeDirty()
    {
        if (StructurePickerClient.paintStrokeBefore != null)
        {
            StructurePickerClient.paintStrokeDirty = true;
        }
    }

    private static void endPaintStroke()
    {
        if (StructurePickerClient.paintStrokeBefore == null)
        {
            return;
        }

        if (StructurePickerClient.paintStrokeDirty)
        {
            List<Region> after = StructurePickerClient.copyRegions();

            if (!StructurePickerClient.paintStrokeBefore.equals(after))
            {
                StructurePickerHistory.push(new RegionsChangeEntry(StructurePickerClient.paintStrokeBefore, after));
            }
        }

        StructurePickerClient.discardPaintStroke();
    }

    private static void discardPaintStroke()
    {
        StructurePickerClient.paintStrokeBefore = null;
        StructurePickerClient.paintStrokeDirty = false;
    }

    /**
     * Called by {@link StructurePickerScaleGizmo} when an axis drag starts.
     */
    static void beginScaleStroke()
    {
        StructurePickerClient.scaleStrokeBefore = StructurePickerClient.copyRegions();
    }

    /**
     * Called by {@link StructurePickerScaleGizmo} when an axis drag ends.
     */
    static void endScaleStroke()
    {
        if (StructurePickerClient.scaleStrokeBefore == null)
        {
            return;
        }

        List<Region> after = StructurePickerClient.copyRegions();

        if (!StructurePickerClient.scaleStrokeBefore.equals(after))
        {
            StructurePickerHistory.push(new RegionsChangeEntry(StructurePickerClient.scaleStrokeBefore, after));
        }

        StructurePickerClient.scaleStrokeBefore = null;
    }

    public static void restoreRegions(List<Region> regions)
    {
        StructurePickerClient.clearSelection();

        if (regions == null || regions.isEmpty())
        {
            return;
        }

        StructurePickerClient.regions.addAll(regions);

        for (int i = 0; i < StructurePickerClient.regions.size(); i++)
        {
            StructurePickerClient.selectedRegionIndices.add(i);
        }

        StructurePickerClient.activeRegionIndex = StructurePickerClient.regions.size() - 1;
        StructurePickerScaleGizmo.ensure();
    }

    private static void tickUndoRedoKeys()
    {
        /* Panel owns Ctrl+Z/Y while open so text fields / overlays can take priority. */
        if (UIStructurePickerPanel.isOpened())
        {
            StructurePickerClient.undoKeyDown = Window.isCtrlPressed() && Window.isKeyPressed(GLFW.GLFW_KEY_Z);
            StructurePickerClient.redoKeyDown = Window.isCtrlPressed() && Window.isKeyPressed(GLFW.GLFW_KEY_Y);

            return;
        }

        if (!StructurePickerClient.isActive())
        {
            StructurePickerClient.undoKeyDown = false;
            StructurePickerClient.redoKeyDown = false;

            return;
        }

        boolean undoDown = Window.isCtrlPressed() && Window.isKeyPressed(GLFW.GLFW_KEY_Z) && !Window.isShiftPressed();
        boolean redoDown = Window.isCtrlPressed() && Window.isKeyPressed(GLFW.GLFW_KEY_Y);

        if (undoDown && !StructurePickerClient.undoKeyDown)
        {
            StructurePickerClient.undo();
        }

        if (redoDown && !StructurePickerClient.redoKeyDown)
        {
            StructurePickerClient.redo();
        }

        StructurePickerClient.undoKeyDown = undoDown;
        StructurePickerClient.redoKeyDown = redoDown;
    }

    public static boolean canUndo()
    {
        return StructurePickerHistory.canUndo();
    }

    public static boolean canRedo()
    {
        return StructurePickerHistory.canRedo();
    }

    public static boolean undo()
    {
        return StructurePickerHistory.undo();
    }

    public static boolean redo()
    {
        return StructurePickerHistory.redo();
    }

    /**
     * Places a structure file into the world and records undo. No panel UI yet;
     * kept so PlaceStructure history entries match the prior SIRSPY contract.
     */
    public static void placeStructure(String path, BlockPos origin)
    {
        if (path == null || path.isEmpty() || origin == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        List<Region> previousRegions = StructurePickerClient.copyRegions();

        StructurePickerClient.runOnServer(mc, (serverWorld) ->
        {
            StructurePickerExporter.PlaceResult result = StructurePickerExporter.placeStructure(serverWorld, path, origin);

            if (result == null)
            {
                return;
            }

            mc.execute(() ->
            {
                StructurePickerClient.mode = StructurePickerMode.CUBE;
                StructurePickerClient.restoreRegions(List.of(new Region(result.min(), result.max(), StructurePickerMode.CUBE)));
                StructurePickerHistory.push(new PlaceStructureEntry(path, origin.toImmutable(), previousRegions, result.previousBlocks(), result.min(), result.max()));
            });
        });
    }

    public static Set<BlockPos> getSelectedBlocks(World world)
    {
        Set<BlockPos> blocks = new LinkedHashSet<>();

        for (Region region : StructurePickerClient.regions)
        {
            blocks.addAll(StructurePickerSelection.collect(world, region.first(), region.second(), region.mode(), StructurePickerClient.clickOnAir, region.triangleFacing()));
        }

        return blocks;
    }

    public static Set<BlockPos> getPreviewBlocks()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        Set<BlockPos> blocks = new LinkedHashSet<>();

        if (world == null)
        {
            return blocks;
        }

        for (Region region : StructurePickerClient.regions)
        {
            StructurePickerClient.addPreviewBlocks(blocks, world, region.first(), region.second(), region.mode(), region.triangleFacing());
        }

        if (StructurePickerClient.hasInProgress() && !StructurePickerClient.subtractMode)
        {
            StructurePickerClient.addPreviewBlocks(blocks, world, StructurePickerClient.firstCorner, StructurePickerClient.secondCorner, StructurePickerClient.mode, StructurePickerClient.triangleFacing);
        }

        return blocks;
    }

    private static void addPreviewBlocks(Set<BlockPos> blocks, World world, BlockPos first, BlockPos second, StructurePickerMode mode, Direction triangleFacing)
    {
        for (BlockPos pos : StructurePickerSelection.preview(world, first, second, mode, triangleFacing))
        {
            if (StructurePickerClient.clickOnAir || !world.getBlockState(pos).isAir())
            {
                blocks.add(pos);
            }
        }
    }

    public static void importSelection(boolean toModelBlock)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;

        if (world == null)
        {
            return;
        }

        List<BlockPos> blocks = new ArrayList<>(StructurePickerClient.getSelectedBlocks(world));

        if (blocks.isEmpty())
        {
            return;
        }

        BlockPos min = blocks.getFirst();
        BlockPos max = blocks.getFirst();

        for (BlockPos pos : blocks)
        {
            min = StructurePickerSelection.min(min, pos);
            max = StructurePickerSelection.max(max, pos);
        }

        BlockPos placement = StructurePickerExporter.getPlacementPos(min, max);

        if (toModelBlock)
        {
            StructurePickerClient.runOnServer(mc, (serverWorld) ->
            {
                StructurePickerExporter.BlockSnapshot previous = StructurePickerExporter.captureBlock(serverWorld, placement);
                String path = StructurePickerExporter.export(serverWorld, blocks);

                if (path == null)
                {
                    return;
                }

                if (!StructurePickerExporter.placeModelBlock(serverWorld, placement, path))
                {
                    return;
                }

                mc.execute(() -> StructurePickerHistory.push(new ImportModelBlockEntry(placement.toImmutable(), previous, path)));
            });
        }
        else
        {
            StructurePickerClient.runOnServer(mc, (serverWorld) ->
            {
                String path = StructurePickerExporter.export(serverWorld, blocks);

                if (path != null)
                {
                    mc.execute(() ->
                    {
                        Replay replay = StructurePickerClient.importToFilm(path, placement);

                        if (replay != null)
                        {
                            StructurePickerHistory.push(new ImportFilmEntry(path, placement.toImmutable(), replay));
                        }
                    });
                }
            });
        }
    }

    public static void breakSelection()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;

        if (world == null)
        {
            return;
        }

        List<Region> previousRegions = StructurePickerClient.copyRegions();
        List<BlockPos> blocks = new ArrayList<>(StructurePickerClient.getSelectedBlocks(world));

        if (blocks.isEmpty())
        {
            StructurePickerClient.clearSelection();

            if (!previousRegions.isEmpty())
            {
                StructurePickerHistory.push(new RemoveSelectionEntry(previousRegions));
            }

            return;
        }

        StructurePickerClient.runOnServer(mc, (serverWorld) ->
        {
            List<StructurePickerExporter.BlockSnapshot> snapshots = StructurePickerExporter.captureBlocks(serverWorld, blocks);

            StructurePickerExporter.removeBlocks(serverWorld, blocks);
            mc.execute(() ->
            {
                StructurePickerClient.clearSelection();
                StructurePickerHistory.push(new BreakSelectionEntry(previousRegions, snapshots));
            });
        });
    }

    private static void runOnServer(MinecraftClient mc, Consumer<ServerWorld> task)
    {
        if (mc.getServer() == null || mc.player == null)
        {
            return;
        }

        RegistryKey<World> key = mc.player.getWorld().getRegistryKey();

        mc.getServer().execute(() ->
        {
            ServerWorld serverWorld = mc.getServer().getWorld(key);

            if (serverWorld != null)
            {
                task.accept(serverWorld);
            }
        });
    }

    private static final int HUD_HOTBAR_HEIGHT = 22;
    private static final int HUD_LINE_HEIGHT = 20;
    private static final int HUD_MARGIN_ABOVE_HOTBAR = 6;

    private static int hudLineY(int screenH, int lineIndex)
    {
        return screenH - HUD_HOTBAR_HEIGHT - HUD_MARGIN_ABOVE_HOTBAR - HUD_LINE_HEIGHT * (lineIndex + 1);
    }

    private static void renderHudLine(Batcher2D batcher, int screenW, int screenH, int lineIndex, String text)
    {
        int w = batcher.getFont().getWidth(text) + 12;
        int x = screenW - w - 8;
        int y = StructurePickerClient.hudLineY(screenH, lineIndex);

        batcher.box(x, y, x + w, y + 16, Colors.A50);
        batcher.textShadow(text, x + 6, y + 4);
    }

    public static void renderHud(Batcher2D batcher)
    {
        if (!StructurePickerClient.isActive())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int lineIndex = 0;

        if (StructurePickerClient.subtractMode && !StructurePickerClient.hasInProgress())
        {
            StructurePickerClient.renderHudLine(batcher, screenW, screenH, lineIndex, UIKeys.STRUCTURE_PICKER_SUBTRACTING.get());
            lineIndex += 1;
        }

        if (!StructurePickerClient.hasInProgress())
        {
            if (StructurePickerClient.mode.isPaintMode() && StructurePickerClient.hasBlockSelection())
            {
                Set<BlockPos> blocks = StructurePickerClient.getAllRegionBlocks();
                BlockPos blockMin = null;
                BlockPos blockMax = null;

                for (BlockPos pos : blocks)
                {
                    if (blockMin == null)
                    {
                        blockMin = pos;
                        blockMax = pos;
                    }
                    else
                    {
                        blockMin = StructurePickerSelection.min(blockMin, pos);
                        blockMax = StructurePickerSelection.max(blockMax, pos);
                    }
                }

                int width = StructurePickerSelection.spanX(blockMin, blockMax);
                int depth = StructurePickerSelection.spanZ(blockMin, blockMax);
                String modeLabel = UIKeys.STRUCTURE_PICKER_MODE_LABELS[StructurePickerClient.mode.index].get();
                String text = UIKeys.STRUCTURE_PICKER_INTERACTING.format(modeLabel, width, depth, blocks.size()).get();

                StructurePickerClient.renderHudLine(batcher, screenW, screenH, lineIndex, text);
            }

            return;
        }

        BlockPos adjusted = StructurePickerSelection.adjustSecond(StructurePickerClient.firstCorner, StructurePickerClient.secondCorner, StructurePickerClient.mode);
        BlockPos min = StructurePickerSelection.min(StructurePickerClient.firstCorner, adjusted);
        BlockPos max = StructurePickerSelection.max(StructurePickerClient.firstCorner, adjusted);
        int width = StructurePickerSelection.spanX(min, max);
        int depth = StructurePickerSelection.spanZ(min, max);
        int count = StructurePickerClient.getPreviewBlocks().size();
        String modeLabel = UIKeys.STRUCTURE_PICKER_MODE_LABELS[StructurePickerClient.mode.index].get();
        String text = UIKeys.STRUCTURE_PICKER_INTERACTING.format(modeLabel, width, depth, count).get();

        StructurePickerClient.renderHudLine(batcher, screenW, screenH, lineIndex, text);
    }

    private static double getPickerReach(MinecraftClient mc)
    {
        if (StructurePickerClient.clickOnAir)
        {
            return mc.player.getBlockInteractionRange();
        }

        if (Window.isCtrlPressed())
        {
            return BBSSettings.structurePickerReachExtended.get();
        }

        return BBSSettings.structurePickerReach.get();
    }

    private static double getAirClickReach(MinecraftClient mc)
    {
        return mc.player.getBlockInteractionRange();
    }

    private static BlockPos resolveTargetBlock(MinecraftClient mc)
    {
        BlockHitResult hit = StructurePickerClient.performRaycast(mc, StructurePickerClient.clickOnAir);

        if (hit == null)
        {
            return null;
        }

        if (hit.getType() == HitResult.Type.BLOCK)
        {
            return hit.getBlockPos();
        }

        if (StructurePickerClient.clickOnAir)
        {
            return BlockPos.ofFloored(hit.getPos());
        }

        return null;
    }

    private static BlockHitResult performRaycast(MinecraftClient mc, boolean allowAir)
    {
        StructurePickerClient.lastRaycastHit = null;

        if (mc.player == null || mc.world == null)
        {
            return null;
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        double reach = StructurePickerClient.getPickerReach(mc);
        Vec3d end = eye.add(look.multiply(reach));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
            eye,
            end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            mc.player
        ));

        if (hit.getType() == HitResult.Type.BLOCK)
        {
            StructurePickerClient.lastRaycastHit = hit;

            return hit;
        }

        if (allowAir)
        {
            double airReach = StructurePickerClient.getAirClickReach(mc);
            Vec3d airPoint = eye.add(look.multiply(airReach));
            BlockHitResult airHit = BlockHitResult.createMissed(airPoint, Direction.getFacing(look.x, look.y, look.z), BlockPos.ofFloored(airPoint));

            StructurePickerClient.lastRaycastHit = airHit;

            return airHit;
        }

        return null;
    }

    private static BlockPos raycastTarget(MinecraftClient mc, boolean allowAir)
    {
        BlockHitResult hit = StructurePickerClient.performRaycast(mc, allowAir);

        if (hit == null)
        {
            return null;
        }

        if (hit.getType() == HitResult.Type.BLOCK)
        {
            return hit.getBlockPos();
        }

        if (allowAir)
        {
            return BlockPos.ofFloored(hit.getPos());
        }

        return null;
    }

    private static BlockPos raycastBlock(MinecraftClient mc)
    {
        return StructurePickerClient.raycastTarget(mc, false);
    }

    private static Replay importToFilm(String structurePath, BlockPos placement)
    {
        UIFilmPanel panel = BBSModClient.getDashboard().getPanel(UIFilmPanel.class);

        if (panel == null || panel.getData() == null)
        {
            return null;
        }

        StructureForm form = new StructureForm();

        form.structureFile.set(structurePath);

        Film film = panel.getData();
        Replay replay = film.replays.addReplay();

        replay.form.set(FormUtils.copy(form));

        replay.keyframes.x.insert(0, placement.getX() + 0.5D);
        replay.keyframes.y.insert(0, (double) placement.getY());
        replay.keyframes.z.insert(0, placement.getZ() + 0.5D);

        panel.replayEditor.replays.replays.finishImport(replay);

        return replay;
    }

    private static void removeFilmReplay(Replay replay)
    {
        if (replay == null)
        {
            return;
        }

        UIFilmPanel panel = BBSModClient.getDashboard().getPanel(UIFilmPanel.class);

        if (panel == null || panel.getData() == null)
        {
            return;
        }

        panel.getData().replays.remove(replay);

        if (panel.replayEditor != null && panel.replayEditor.replays != null && panel.replayEditor.replays.replays != null)
        {
            panel.replayEditor.replays.replays.update();
        }
    }

    private static final class RegionsChangeEntry implements StructurePickerHistory.Entry
    {
        private final List<Region> before;
        private final List<Region> after;

        private RegionsChangeEntry(List<Region> before, List<Region> after)
        {
            this.before = before;
            this.after = after;
        }

        @Override
        public void undo()
        {
            StructurePickerClient.restoreRegions(this.before);
        }

        @Override
        public void redo()
        {
            StructurePickerClient.restoreRegions(this.after);
        }
    }

    private static final class RemoveSelectionEntry implements StructurePickerHistory.Entry
    {
        private final List<Region> regions;

        private RemoveSelectionEntry(List<Region> regions)
        {
            this.regions = regions;
        }

        @Override
        public void undo()
        {
            StructurePickerClient.restoreRegions(this.regions);
        }

        @Override
        public void redo()
        {
            StructurePickerClient.clearSelection();
        }
    }

    private static final class BreakSelectionEntry implements StructurePickerHistory.Entry
    {
        private final List<Region> regions;
        private final List<StructurePickerExporter.BlockSnapshot> snapshots;

        private BreakSelectionEntry(List<Region> regions, List<StructurePickerExporter.BlockSnapshot> snapshots)
        {
            this.regions = regions;
            this.snapshots = snapshots;
        }

        @Override
        public void undo()
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            StructurePickerClient.runOnServer(mc, (serverWorld) -> StructurePickerExporter.restoreBlocks(serverWorld, this.snapshots));
            StructurePickerClient.restoreRegions(this.regions);
        }

        @Override
        public void redo()
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            List<BlockPos> blocks = new ArrayList<>(this.snapshots.size());

            for (StructurePickerExporter.BlockSnapshot snapshot : this.snapshots)
            {
                blocks.add(snapshot.pos());
            }

            StructurePickerClient.runOnServer(mc, (serverWorld) -> StructurePickerExporter.removeBlocks(serverWorld, blocks));
            StructurePickerClient.clearSelection();
        }
    }

    private static final class PlaceStructureEntry implements StructurePickerHistory.Entry
    {
        private final String path;
        private final BlockPos origin;
        private final List<Region> previousRegions;
        private final List<StructurePickerExporter.BlockSnapshot> previousBlocks;
        private final BlockPos placedMin;
        private final BlockPos placedMax;

        private PlaceStructureEntry(String path, BlockPos origin, List<Region> previousRegions, List<StructurePickerExporter.BlockSnapshot> previousBlocks, BlockPos placedMin, BlockPos placedMax)
        {
            this.path = path;
            this.origin = origin;
            this.previousRegions = previousRegions;
            this.previousBlocks = previousBlocks;
            this.placedMin = placedMin;
            this.placedMax = placedMax;
        }

        @Override
        public void undo()
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            StructurePickerClient.runOnServer(mc, (serverWorld) -> StructurePickerExporter.restoreBlocks(serverWorld, this.previousBlocks));
            StructurePickerClient.restoreRegions(this.previousRegions);
        }

        @Override
        public void redo()
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            StructurePickerClient.runOnServer(mc, (serverWorld) ->
            {
                StructurePickerExporter.PlaceResult result = StructurePickerExporter.placeStructure(serverWorld, this.path, this.origin);

                if (result == null)
                {
                    return;
                }

                mc.execute(() ->
                {
                    StructurePickerClient.mode = StructurePickerMode.CUBE;
                    StructurePickerClient.restoreRegions(List.of(new Region(result.min(), result.max(), StructurePickerMode.CUBE)));
                });
            });
        }
    }

    private static final class ImportModelBlockEntry implements StructurePickerHistory.Entry
    {
        private final BlockPos placement;
        private final StructurePickerExporter.BlockSnapshot previous;
        private final String structurePath;

        private ImportModelBlockEntry(BlockPos placement, StructurePickerExporter.BlockSnapshot previous, String structurePath)
        {
            this.placement = placement;
            this.previous = previous;
            this.structurePath = structurePath;
        }

        @Override
        public void undo()
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            StructurePickerClient.runOnServer(mc, (serverWorld) -> StructurePickerExporter.restoreBlock(serverWorld, this.previous));
        }

        @Override
        public void redo()
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            StructurePickerClient.runOnServer(mc, (serverWorld) ->
                StructurePickerExporter.placeModelBlock(serverWorld, this.placement, this.structurePath));
        }
    }

    private static final class ImportFilmEntry implements StructurePickerHistory.Entry
    {
        private final String structurePath;
        private final BlockPos placement;
        private Replay replay;

        private ImportFilmEntry(String structurePath, BlockPos placement, Replay replay)
        {
            this.structurePath = structurePath;
            this.placement = placement;
            this.replay = replay;
        }

        @Override
        public void undo()
        {
            StructurePickerClient.removeFilmReplay(this.replay);
            this.replay = null;
        }

        @Override
        public void redo()
        {
            this.replay = StructurePickerClient.importToFilm(this.structurePath, this.placement);
        }
    }

    public record Region(BlockPos first, BlockPos second, StructurePickerMode mode, Direction triangleFacing)
    {
        public Region(BlockPos first, BlockPos second, StructurePickerMode mode)
        {
            this(first, second, mode, null);
        }
    }
}
