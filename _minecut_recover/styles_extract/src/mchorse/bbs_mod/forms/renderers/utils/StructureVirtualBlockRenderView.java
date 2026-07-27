package mchorse.bbs_mod.forms.renderers.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1944;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;

public class StructureVirtualBlockRenderView extends VirtualBlockRenderView
{
    private final List<class_2338> emitters = new ArrayList<>();
    private final List<Integer> emitterLevels = new ArrayList<>();

    private boolean virtualMode = false;
    private int virtualAmbient = 15;
    private boolean ignoreWorldBlockLight = false;

    public StructureVirtualBlockRenderView(List<Entry> entries)
    {
        super(entries);

        for (Entry e : entries)
        {
            class_2680 state = e.state;

            if (state != null)
            {
                int lum = state.method_26213();

                if (lum > 0)
                {
                    this.emitters.add(e.pos);
                    this.emitterLevels.add(lum);
                }
            }
        }
    }

    public StructureVirtualBlockRenderView setVirtualMode(boolean enabled, int intensity)
    {
        this.virtualMode = enabled;
        this.virtualAmbient = Math.max(0, Math.min(15, intensity));

        return this;
    }

    public StructureVirtualBlockRenderView setIgnoreWorldBlockLight(boolean ignore)
    {
        this.ignoreWorldBlockLight = ignore;

        return this;
    }

    @Override
    public int method_8314(class_1944 type, class_2338 pos)
    {
        int base = super.method_8314(type, pos);

        if (type == class_1944.field_9282 && this.ignoreWorldBlockLight)
        {
            base = 0;
        }

        if (!this.virtualMode || type != class_1944.field_9282 || this.emitters.isEmpty())
        {
            return base;
        }

        int max = 0;

        for (int i = 0; i < this.emitters.size(); i++)
        {
            class_2338 sp = this.emitters.get(i);
            int L = this.emitterLevels.get(i);

            int dx = Math.abs(sp.method_10263() - pos.method_10263());
            int dy = Math.abs(sp.method_10264() - pos.method_10264());
            int dz = Math.abs(sp.method_10260() - pos.method_10260());
            int dist = dx + dy + dz;

            int contrib = Math.max(0, L - dist);

            if (contrib > max)
            {
                max = contrib;
            }
        }

        max = Math.min(max, this.virtualAmbient);

        return Math.max(base, max);
    }

    @Override
    public int method_22335(class_2338 pos, int ambientDarkness)
    {
        if (!this.ignoreWorldBlockLight)
        {
            return super.method_22335(pos, ambientDarkness);
        }

        if (class_310.method_1551().field_1687 == null)
        {
            return 15;
        }

        class_2338 worldPos = new class_2338(
            this.getWorldAnchor().method_10263() + this.getBaseDx() + pos.method_10263(),
            this.getWorldAnchor().method_10264() + this.getBaseDy() + pos.method_10264(),
            this.getWorldAnchor().method_10260() + this.getBaseDz() + pos.method_10260()
        );

        return class_310.method_1551().field_1687.method_8314(class_1944.field_9284, worldPos);
    }
}
