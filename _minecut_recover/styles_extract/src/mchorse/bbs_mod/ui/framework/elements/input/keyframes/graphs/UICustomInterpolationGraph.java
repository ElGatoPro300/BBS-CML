package mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UICustomInterpolationGraph extends UIKeyframeGraph
{
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");

    public UICustomInterpolationGraph(UIKeyframes keyframes, UIKeyframeSheet sheet)
    {
        super(keyframes, sheet);
    }

    @Override
    public void resetView()
    {
        // Default to 0-1 view with minimal padding to maximize visibility
        this.keyframes.getXAxis().set(0, 1);
        this.keyframes.getXAxis().viewOffset(-0.05, 1.05, this.keyframes.area.w, 0);
        
        this.yAxis.set(0, 1);
        this.yAxis.viewOffset(-0.05, 1.05, this.keyframes.area.h, 0);
    }

    @Override
    public Keyframe addKeyframe(UIKeyframeSheet sheet, float tick, Object value)
    {
        Keyframe keyframe = super.addKeyframe(sheet, tick, value);
        
        keyframe.getInterpolation().setInterp(Interpolations.BEZIER);
        keyframe.lx = 0.15f;
        keyframe.rx = 0.15f;
        
        return keyframe;
    }

    @Override
    public void removeSelected()
    {
        for (UIKeyframeSheet sheet : this.getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();
            List<Keyframe> toRemove = new ArrayList<>();
            
            for (Keyframe kf : selected)
            {
                if (Math.abs(kf.getTick() - 0) > 0.0001 && Math.abs(kf.getTick() - 1) > 0.0001)
                {
                    toRemove.add(kf);
                }
            }
            
            for (Keyframe kf : toRemove)
            {
                sheet.remove(kf);
            }
            
            sheet.selection.clear();
        }
        
        this.pickKeyframe(null);
    }

    @Override
    public void dragKeyframes(UIContext context, Pair<Keyframe, KeyframeType> type, int originalX, int originalY, float originalT, Object originalV)
    {
        if (type == null)
        {
            return;
        }
        
        if (type.b == KeyframeType.REGULAR)
        {
             if (Math.abs(originalT - 0) < 0.0001 || Math.abs(originalT - 1) < 0.0001)
             {
                 return;
             }
        }

        IKeyframeFactory factory = this.sheet.channel.getFactory();
        Keyframe keyframe = type.a;

        if (type.b == KeyframeType.REGULAR)
        {
            float offsetX = (float) this.keyframes.fromGraphX(originalX) - originalT;
            double offsetY = this.fromGraphY(originalY) - factory.getY(originalV);

            float fx = (float) this.keyframes.fromGraphX(context.mouseX) - offsetX;
            Object fy = factory.yToValue(this.fromGraphY(context.mouseY) - offsetY);

            if (!Window.isShiftPressed())
            {
                fx = Math.round(fx * 100) / 100f;
            }

            this.setTick(fx, false);
            this.setValue(fy, false);
        }
        else if (type.b == KeyframeType.LEFT_HANDLE)
        {
            keyframe.lx = Math.max(0, -(float) ((this.keyframes.fromGraphX(context.mouseX)) - keyframe.getTick()));
            keyframe.ly = (float) (this.fromGraphY(context.mouseY) - factory.getY(originalV));

            if (!Window.isShiftPressed())
            {
                keyframe.rx = keyframe.lx;
                keyframe.ry = -keyframe.ly;
            }
        }
        else if (type.b == KeyframeType.RIGHT_HANDLE)
        {
            keyframe.rx = Math.max(0, (float) ((this.keyframes.fromGraphX(context.mouseX)) - keyframe.getTick()));
            keyframe.ry = (float) (this.fromGraphY(context.mouseY) - factory.getY(originalV));

            if (!Window.isShiftPressed())
            {
                keyframe.lx = keyframe.rx;
                keyframe.ly = -keyframe.ry;
            }
        }

        this.keyframes.triggerChange();
    }

    @Override
    protected void renderGrid(UIContext context)
    {
        Area area = this.keyframes.area;

        // Draw axes origin lines
        int x0 = this.keyframes.toGraphX(0);
        int y0 = this.toGraphY(0);
        int x1 = this.keyframes.toGraphX(1);
        int y1 = this.toGraphY(1);

        // Draw 0-1 box if visible (guide for interpolation)
        if (x0 < area.ex() && x1 > area.x && y0 > area.y && y1 < area.ey())
        {
             // Box (0,0) to (1,1)
             int bx = Math.max(x0, area.x);
             int by = Math.max(y1, area.y); // y1 is top (1.0)
             int bex = Math.min(x1, area.ex());
             int bey = Math.min(y0, area.ey()); // y0 is bottom (0.0)
             
             if (bx < bex && by < bey)
             {
                 context.batcher.box(bx, by, bex, bey, Colors.setA(Colors.WHITE, 0.05F));
             }
        }

        // Draw Grid
        drawAxisGrid(context, true);
        drawAxisGrid(context, false);
        
        // Draw Main Axes (stronger color)
        if (y0 >= area.y && y0 <= area.ey())
        {
            context.batcher.box(area.x, y0, area.ex(), y0 + 1, Colors.setA(Colors.WHITE, 0.5F));
        }
        
        if (x0 >= area.x && x0 <= area.ex())
        {
            context.batcher.box(x0, area.y, x0 + 1, area.ey(), Colors.setA(Colors.WHITE, 0.5F));
        }

        // Render preview keyframes (copied logic from UIKeyframeGraph but adapted if needed)
        // We reuse the logic by calling a helper or copying it. 
        // Since we exposed renderPreviewKeyframe, we can implement the logic here.
        
        if (!area.isInside(context))
        {
            return;
        }

        float currentTick = (float) this.keyframes.fromGraphX(context.mouseX);

        if (this.keyframes.isStacking())
        {
             // Stacking logic (omitted or copied if needed, assuming less critical for this specific editor)
             // But let's support it if possible.
             // Accessing keyframes.isStacking() is fine (public).
        }
        else if (Window.isCtrlPressed())
        {
            UIKeyframeSheet sheet = this.getSheet(context.mouseY);

            if (sheet != null)
            {
                float tick = currentTick;

                if (!Window.isShiftPressed())
                {
                    // Snap to grid?
                    // For interpolation, maybe snap to 0.1 or 0.05?
                    // Default logic was Math.round(tick).
                    // Let's snap to 0.05
                    tick = Math.round(tick * 20) / 20f;
                }

                this.renderPreviewKeyframe(context, sheet, tick, context.mouseY, Colors.WHITE);
            }
        }
    }

    private void drawAxisGrid(UIContext context, boolean horizontal)
    {
        Area area = this.keyframes.area;
        double min = horizontal ? this.keyframes.fromGraphX(area.x) : this.fromGraphY(area.ey());
        double max = horizontal ? this.keyframes.fromGraphX(area.ex()) : this.fromGraphY(area.y);
        
        if (min > max)
        {
            double temp = min;
            min = max;
            max = temp;
        }

        double range = max - min;
        double step = getStep(range);
        
        double start = Math.floor(min / step) * step;
        
        for (double val = start; val <= max + step; val += step)
        {
             if (Math.abs(val) < 0.0001) continue; // Skip 0 (drawn by main axis)

             if (horizontal)
             {
                 int x = this.keyframes.toGraphX(val);
                 if (x >= area.x && x <= area.ex())
                 {
                     context.batcher.box(x, area.y, x + 1, area.ey(), Colors.setA(Colors.WHITE, 0.15F));
                     context.batcher.text(FORMAT.format(val), x + 2, area.ey() - 10);
                 }
             }
             else
             {
                 int y = this.toGraphY(val);
                 if (y >= area.y && y <= area.ey())
                 {
                     context.batcher.box(area.x, y, area.ex(), y + 1, Colors.setA(Colors.WHITE, 0.15F));
                     context.batcher.text(FORMAT.format(val), area.x + 2, y + 2);
                 }
             }
        }
    }

    private double getStep(double range)
    {
        double step = 1.0;
        double[] steps = {1.0, 0.5, 0.2, 0.1, 0.05, 0.01, 0.005, 0.001};
        
        for (double s : steps)
        {
            if (range / s > 5) // Aim for at least 5 lines
            {
                step = s;
                break;
            }
        }
        
        // If range is very large or very small
        if (range > 10) step = 1.0; // fallback
        if (range > 50) step = 5.0;
        if (range > 100) step = 10.0;
        
        return step;
    }
    
    @Override
    public boolean addKeyframe(int mouseX, int mouseY)
    {
        // Override to support float snapping
        float tick = (float) this.keyframes.fromGraphX(mouseX);
        UIKeyframeSheet sheet = this.sheet;

        if (!Window.isShiftPressed())
        {
             tick = Math.round(tick * 20) / 20f;
        }

        if (sheet != null)
        {
            this.addKeyframe(sheet, tick, sheet.channel.getFactory().yToValue(this.fromGraphY(mouseY)));
        }

        return sheet != null;
    }
}
