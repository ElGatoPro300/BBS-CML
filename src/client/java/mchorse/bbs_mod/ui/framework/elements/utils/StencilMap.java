package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class StencilMap
{
    public static final List<Consumer<StencilMap>> extensions = new ArrayList<>();

    public int objectIndex;
    public Map<Integer, Pair<Form, String>> indexMap = new HashMap<>();
    public boolean increment = true;
    public Set<String> allowedBones;

    public void setIncrement(boolean increment)
    {
        this.increment = increment;
    }

    public void setup()
    {
        this.objectIndex = Gizmo.STENCIL_HANDLE_MAX + 1;

        /* Reset map and setup pairs for every Gizmo handle (solo translate/scale/rotate axes
         * and planes, the screen-move cube, and the Combined-mode-only scale/rotate/trackball
         * handles), so a).getPicked() != null for every handle id and b) picking never collides
         * with a bone/form's own picking id. */
        this.indexMap.clear();
        this.indexMap.put(Gizmo.STENCIL_X, new Pair<>(null, "x"));
        this.indexMap.put(Gizmo.STENCIL_Y, new Pair<>(null, "y"));
        this.indexMap.put(Gizmo.STENCIL_Z, new Pair<>(null, "z"));
        this.indexMap.put(Gizmo.STENCIL_XZ, new Pair<>(null, "xz"));
        this.indexMap.put(Gizmo.STENCIL_XY, new Pair<>(null, "xy"));
        this.indexMap.put(Gizmo.STENCIL_ZY, new Pair<>(null, "zy"));
        this.indexMap.put(Gizmo.STENCIL_FREE, new Pair<>(null, "free"));
        this.indexMap.put(Gizmo.STENCIL_SCALE_X, new Pair<>(null, "scale_x"));
        this.indexMap.put(Gizmo.STENCIL_SCALE_Y, new Pair<>(null, "scale_y"));
        this.indexMap.put(Gizmo.STENCIL_SCALE_Z, new Pair<>(null, "scale_z"));
        this.indexMap.put(Gizmo.STENCIL_ROTATE_X, new Pair<>(null, "rotate_x"));
        this.indexMap.put(Gizmo.STENCIL_ROTATE_Y, new Pair<>(null, "rotate_y"));
        this.indexMap.put(Gizmo.STENCIL_ROTATE_Z, new Pair<>(null, "rotate_z"));
        this.indexMap.put(Gizmo.STENCIL_TRACKBALL, new Pair<>(null, "trackball"));
        this.indexMap.put(Gizmo.STENCIL_SCREEN, new Pair<>(null, "screen"));
        this.indexMap.put(Gizmo.STENCIL_VIEW, new Pair<>(null, "view"));

        for (Consumer<StencilMap> consumer : extensions)
        {
            consumer.accept(this);
        }
    }

    public void addPicking(Form form)
    {
        this.addPicking(form, "");
    }

    public void addPicking(Form form, String bone)
    {
        if (this.increment)
        {
            this.indexMap.put(this.objectIndex, new Pair<>(form, bone));

            this.objectIndex += 1;
        }
        else
        {
            this.indexMap.put(this.objectIndex, new Pair<>(form, ""));
        }
    }

    public void addPicking(int index, Form form, String bone)
    {
        this.indexMap.put(index, new Pair<>(form, bone));

        if (this.increment)
        {
            this.objectIndex = Math.max(this.objectIndex, index + 1);
        }
    }

    public boolean isBoneAllowed(String bone)
    {
        return this.allowedBones == null || this.allowedBones.contains(bone);
    }

    public int findIndex(Form form, String bone)
    {
        if (form == null)
        {
            return 0;
        }

        String key = bone == null ? "" : bone;

        for (Map.Entry<Integer, Pair<Form, String>> entry : this.indexMap.entrySet())
        {
            if (entry.getValue() != null && entry.getValue().a == form && key.equals(entry.getValue().b))
            {
                return entry.getKey();
            }
        }

        return 0;
    }
}
