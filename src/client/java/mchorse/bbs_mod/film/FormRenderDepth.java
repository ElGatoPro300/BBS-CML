package mchorse.bbs_mod.film;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ShapeForm;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.netty.util.collection.IntObjectMap;

/**
 * Per-form render depth layering for film rendering, including nested body part forms.
 */
public class FormRenderDepth
{
    public static final class Occluder
    {
        public final Form form;
        public final double depth;
        public final double distanceSq;

        public Occluder(Form form, double depth, double distanceSq)
        {
            this.form = form;
            this.depth = depth;
            this.distanceSq = distanceSq;
        }
    }

    public static final class Frame
    {
        public final List<Occluder> occluders;
        public final Form sourceRootForm;

        public Frame(List<Occluder> occluders, Form sourceRootForm)
        {
            this.occluders = occluders;
            this.sourceRootForm = sourceRootForm;
        }
    }

    private FormRenderDepth()
    {}

    public static List<Occluder> collectOccluders(IntObjectMap<IEntity> entities, Camera camera, float transition, Function<Integer, Form> sourceFormProvider)
    {
        return collectOccluders(entities, camera.position.x, camera.position.y, camera.position.z, transition, sourceFormProvider);
    }

    public static List<Occluder> collectOccluders(IntObjectMap<IEntity> entities, net.minecraft.client.render.Camera camera, float transition, Function<Integer, Form> sourceFormProvider)
    {
        Vec3d pos = camera.getPos();

        return collectOccluders(entities, pos.x, pos.y, pos.z, transition, sourceFormProvider);
    }

    private static List<Occluder> collectOccluders(IntObjectMap<IEntity> entities, double cameraX, double cameraY, double cameraZ, float transition, Function<Integer, Form> sourceFormProvider)
    {
        List<Occluder> occluders = new ArrayList<>();

        for (Integer index : entities.keySet())
        {
            IEntity entity = entities.get(index);

            if (entity == null || entity.getForm() == null)
            {
                continue;
            }

            Form sourceRoot = sourceFormProvider.apply(index);
            double distanceSq = getEntityDistanceSq(entity, cameraX, cameraY, cameraZ, transition);

            collectFromForm(entity.getForm(), sourceRoot, distanceSq, occluders);
        }

        return occluders;
    }

    public static void collectFromForm(Form form, Form sourceForm, double distanceSq, List<Occluder> occluders)
    {
        if (form == null || !form.render.get())
        {
            return;
        }

        if (isSemiTransparent(form))
        {
            Double depth = getEnabledDepth(form, sourceForm);

            if (depth != null)
            {
                occluders.add(new Occluder(form, depth, distanceSq));
            }
        }

        List<BodyPart> parts = form.parts.getAllTyped();

        for (BodyPart part : parts)
        {
            Form child = part.getForm();

            if (child != null)
            {
                collectFromForm(child, getSourceForm(sourceForm, child), distanceSq, occluders);
            }
        }
    }

    public static Form getSourceForm(Form sourceRoot, Form form)
    {
        if (sourceRoot == null || form == null)
        {
            return null;
        }

        if (sourceRoot == form)
        {
            return sourceRoot;
        }

        String path = FormUtils.getPath(form);

        if (path.isEmpty())
        {
            return sourceRoot;
        }

        Form resolved = FormUtils.getForm(sourceRoot, path);

        return resolved == null ? sourceRoot : resolved;
    }

    public static Double getEnabledDepth(Form form, Form sourceForm)
    {
        if (form == null)
        {
            return null;
        }

        boolean enabled = sourceForm != null ? sourceForm.renderDepthEnabled.get() : form.renderDepthEnabled.get();

        return enabled ? (double) form.renderDepth.get() : null;
    }

    /**
     * Sort key for translucent film layering. Missing / disabled render depth sorts as 0
     * (same as body-part depth sort).
     */
    public static double resolveSortDepth(Form form, Frame frame)
    {
        if (form == null)
        {
            return 0D;
        }

        if (frame == null)
        {
            return form.renderDepthEnabled.get() ? (double) form.renderDepth.get() : 0D;
        }

        Double depth = getEnabledDepth(form, getSourceForm(frame.sourceRootForm, form));

        return depth == null ? 0D : depth;
    }

    public static float getFade(Form form, Form sourceForm, double distanceSq, List<Occluder> occluders)
    {
        Double depth = getEnabledDepth(form, sourceForm);

        if (depth == null || occluders == null)
        {
            return 1F;
        }

        Double maxFrontTransparentDepth = null;

        for (Occluder occluder : occluders)
        {
            if (occluder.form == form)
            {
                continue;
            }

            if (occluder.distanceSq >= distanceSq - 0.0001D)
            {
                continue;
            }

            if (maxFrontTransparentDepth == null || occluder.depth > maxFrontTransparentDepth)
            {
                maxFrontTransparentDepth = occluder.depth;
            }
        }

        if (maxFrontTransparentDepth == null || depth >= maxFrontTransparentDepth)
        {
            return 1F;
        }

        return 0F;
    }

    /**
     * True when a render-depth occluder can force this form into the deferred Iris queue.
     * Only Shape/Billboard panels count — never character meshes. A low-alpha ModelForm
     * ({@code #1b}) must not yank opaque neighbors off Iris (that flattened lighting/shadows
     * on the yellow dummy). Panels still share the deferred queue for render depth.
     */
    public static boolean needsDeferredDepthOcclusion(Form form, double distanceSq, List<Occluder> occluders)
    {
        if (form == null || occluders == null || occluders.isEmpty())
        {
            return false;
        }

        for (Occluder occluder : occluders)
        {
            if (occluder.form == form || !countsAsDeferredDepthOccluder(occluder.form))
            {
                continue;
            }

            /* Closer panel, or same-entity body-part (shared distance). */
            if (occluder.distanceSq <= distanceSq + 0.0001D)
            {
                return true;
            }
        }

        return false;
    }

    private static boolean countsAsDeferredDepthOccluder(Form form)
    {
        return form instanceof ShapeForm || form instanceof BillboardForm;
    }

    public static double getEntityDistanceSq(IEntity entity, Camera camera, float transition)
    {
        return getEntityDistanceSq(entity, camera.position.x, camera.position.y, camera.position.z, transition);
    }

    public static double getEntityDistanceSq(IEntity entity, net.minecraft.client.render.Camera camera, float transition)
    {
        Vec3d pos = camera.getPos();

        return getEntityDistanceSq(entity, pos.x, pos.y, pos.z, transition);
    }

    private static double getEntityDistanceSq(IEntity entity, double cameraX, double cameraY, double cameraZ, float transition)
    {
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition);
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition);
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition);
        double dx = x - cameraX;
        double dy = y - cameraY;
        double dz = z - cameraZ;

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Forms that can wipe lower render-depth neighbors when closer to the camera.
     * Only Shape/Billboard panels — low-opacity character meshes must not become occluders
     * (camera orbit flipped distanceSq and made actors vanish at some angles).
     * Fully invisible (opacity 0) panels also do not occlude.
     */
    public static boolean isSemiTransparent(Form form)
    {
        if (form == null || !(form instanceof ShapeForm || form instanceof BillboardForm))
        {
            return false;
        }

        float opacity = form.getFormOpacity();

        return opacity > 0.001F && opacity < 0.999F;
    }
}
