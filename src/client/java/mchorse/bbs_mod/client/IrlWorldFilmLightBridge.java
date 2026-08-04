package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Optional IRLights bridge: register in-world film replay lights in pure world
 * coordinates before the addon's SSBO flush.
 *
 * <p>IRLights' {@code LightCollector} already scans ModelBlocks and dashboard
 * editor replays that way (roll/view independent). In-world playback
 * ({@link WorldFilmController} via Right-Ctrl) only registered on the form
 * render-path, where the spectator camera view can mix into light poses — lights
 * then drift when looking around outside the 3D viewport. Shadow casters already
 * walk {@link Films#getControllers()}; this mirrors that for light registration
 * via reflection (no hard dependency on irlite).</p>
 */
public final class IrlWorldFilmLightBridge
{
    private static final String POINT_FORM = "qualet.irlite.forms.PointLightForm";
    private static final String SPOT_FORM = "qualet.irlite.forms.SpotlightForm";
    private static final String LIGHT_REGISTRY = "org.qualet.irl.light.LightRegistry";
    private static final String LIGHT_MATH = "org.qualet.irl.light.LightMath";

    private static final double MAX_DIST_SQ = 256.0 * 256.0;

    private static Boolean present;
    private static Class<?> pointClass;
    private static Class<?> spotClass;
    private static Method registerPoint;
    private static Method registerSpot;
    private static Method normalizeDir;
    private static Method coneFactory;
    private static Method coneCosOuter;
    private static Method coneCosInner;

    private IrlWorldFilmLightBridge()
    {}

    /**
     * Walk active world-film controllers and register root (non-bone) IRLights
     * forms. Safe no-op when the addon is absent.
     */
    public static void collectBeforeFlush(float tickDelta)
    {
        if (!ensureResolved())
        {
            return;
        }

        Films films = BBSModClient.getFilms();

        if (films == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null)
        {
            return;
        }

        double camX = mc.gameRenderer.getCamera().getPos().x;
        double camY = mc.gameRenderer.getCamera().getPos().y;
        double camZ = mc.gameRenderer.getCamera().getPos().z;

        for (BaseFilmController controller : films.getControllers())
        {
            if (!(controller instanceof WorldFilmController) || controller.film == null || controller.film.replays == null)
            {
                continue;
            }

            List<Replay> replays = controller.film.replays.getList();

            if (replays == null || replays.isEmpty())
            {
                continue;
            }

            for (Map.Entry<Integer, IEntity> entry : controller.getEntities().entrySet())
            {
                int replayId = entry.getKey();
                Replay replay = CollectionUtils.getSafe(replays, replayId);

                if (replay == null || replay.actor.get() || !replay.enabled.get())
                {
                    continue;
                }

                IEntity entity = entry.getValue();

                if (entity == null)
                {
                    continue;
                }

                Form root = entity.getForm();

                if (root == null)
                {
                    continue;
                }

                double wx = MathHelper.lerp(tickDelta, entity.getPrevX(), entity.getX());
                double wy = MathHelper.lerp(tickDelta, entity.getPrevY(), entity.getY());
                double wz = MathHelper.lerp(tickDelta, entity.getPrevZ(), entity.getZ());
                double dx = wx - camX;
                double dy = wy - camY;
                double dz = wz - camZ;

                if (dx * dx + dy * dy + dz * dz > MAX_DIST_SQ)
                {
                    continue;
                }

                float bodyYaw = MathHelper.lerp(tickDelta, entity.getPrevBodyYaw(), entity.getBodyYaw());
                Matrix4f rootMatrix = new Matrix4f().identity();

                rootMatrix.rotateY((float) Math.toRadians(-bodyYaw));

                walk(root, rootMatrix, wx, wy, wz, tickDelta);
            }
        }
    }

    private static void walk(Form form, Matrix4f parent, double baseX, double baseY, double baseZ, float transition)
    {
        if (form == null)
        {
            return;
        }

        form.applyStates(transition);

        try
        {
            if (!form.visible.get() || !form.render.get())
            {
                return;
            }

            Matrix4f local = new Matrix4f(parent);
            Transform transform = form.transform.get();

            if (transform != null)
            {
                Matrix4f tm = new Matrix4f();

                transform.setupMatrix(tm);
                local.mul(tm);
            }

            if (pointClass.isInstance(form))
            {
                emitPoint(form, local, baseX, baseY, baseZ);
            }
            else if (spotClass.isInstance(form))
            {
                emitSpot(form, local, baseX, baseY, baseZ);
            }

            if (form.parts == null)
            {
                return;
            }

            List<BodyPart> parts = form.parts.getAllTyped();

            if (parts == null)
            {
                return;
            }

            for (BodyPart part : parts)
            {
                if (part == null)
                {
                    continue;
                }

                String bone = part.bone.get();

                /* Bone-attached lights still need the render-path rig pose. */
                if (bone != null && !bone.isEmpty())
                {
                    continue;
                }

                Form child = part.getForm();

                if (child == null)
                {
                    continue;
                }

                Matrix4f childMatrix = new Matrix4f(local);
                Transform partTransform = part.transform.get();

                if (partTransform != null)
                {
                    Matrix4f ptm = new Matrix4f();

                    partTransform.setupMatrix(ptm);
                    childMatrix.mul(ptm);
                }

                walk(child, childMatrix, baseX, baseY, baseZ, transition);
            }
        }
        finally
        {
            form.unapplyStates();
        }
    }

    private static void emitPoint(Form form, Matrix4f matrix, double baseX, double baseY, double baseZ)
    {
        Vector4f origin = new Vector4f(0F, 0F, 0F, 1F);

        matrix.transform(origin);

        Color color = readColor(form, "color");
        float intensity = readFloat(form, "intensity", 1F);
        float radius = readFloat(form, "radius", 6F);
        float anisotropy = readFloat(form, "anisotropy", 0.4F);
        float vlDensity = readFloat(form, "vlDensity", 0.05F);
        float beam = readFloat(form, "beamStrength", 1F);
        float bulb = readFloat(form, "bulbSize", 0F);
        boolean entitiesOnly = readBoolean(form, "entitiesOnly", false);
        boolean blocksOnly = readBoolean(form, "blocksOnly", false);
        boolean shadows = readBoolean(form, "shadows", true);

        if (color == null)
        {
            color = Color.white();
        }

        try
        {
            registerPoint.invoke(null,
                baseX + origin.x, baseY + origin.y, baseZ + origin.z,
                color.r, color.g, color.b,
                intensity, radius,
                entitiesOnly, blocksOnly,
                anisotropy, vlDensity, beam, bulb, shadows,
                (long) System.identityHashCode(form)
            );
        }
        catch (Throwable ignored)
        {}
    }

    private static void emitSpot(Form form, Matrix4f matrix, double baseX, double baseY, double baseZ)
    {
        Vector4f origin = new Vector4f(0F, 0F, 0F, 1F);
        Vector4f forward = new Vector4f(0F, 0F, 1F, 0F);

        matrix.transform(origin);
        matrix.transform(forward);

        try
        {
            normalizeDir.invoke(null, forward.x, forward.y, forward.z, 0F, 0F, 1F, forward);

            float radius = readFloat(form, "radius", 35F);
            float innerRadius = readFloat(form, "innerRadius", 25F);
            Object cone = coneFactory.invoke(null, radius, innerRadius);
            float cosOuter = ((Float) coneCosOuter.invoke(cone)).floatValue();
            float cosInner = ((Float) coneCosInner.invoke(cone)).floatValue();

            Color color = readColor(form, "color");
            float intensity = readFloat(form, "intensity", 1F);
            float range = readFloat(form, "range", 12F);
            float anisotropy = readFloat(form, "anisotropy", 0.4F);
            float vlDensity = readFloat(form, "vlDensity", 0.05F);
            float beam = readFloat(form, "beamStrength", 1F);
            float bulb = readFloat(form, "bulbSize", 0F);
            boolean entitiesOnly = readBoolean(form, "entitiesOnly", false);
            boolean blocksOnly = readBoolean(form, "blocksOnly", false);
            boolean shadows = readBoolean(form, "shadows", true);

            if (color == null)
            {
                color = Color.white();
            }

            registerSpot.invoke(null,
                baseX + origin.x, baseY + origin.y, baseZ + origin.z,
                forward.x, forward.y, forward.z,
                color.r, color.g, color.b,
                intensity, range, cosOuter, cosInner,
                entitiesOnly, blocksOnly,
                anisotropy, vlDensity, beam, bulb, shadows,
                (long) System.identityHashCode(form)
            );
        }
        catch (Throwable ignored)
        {}
    }

    private static Color readColor(Form form, String field)
    {
        try
        {
            Object value = form.getClass().getField(field).get(form);
            Object color = value.getClass().getMethod("get").invoke(value);

            if (color instanceof Color c)
            {
                return c;
            }
        }
        catch (Throwable ignored)
        {}

        return null;
    }

    private static float readFloat(Form form, String field, float fallback)
    {
        try
        {
            Object value = form.getClass().getField(field).get(form);
            Object number = value.getClass().getMethod("get").invoke(value);

            if (number instanceof Number n)
            {
                return n.floatValue();
            }
        }
        catch (Throwable ignored)
        {}

        return fallback;
    }

    private static boolean readBoolean(Form form, String field, boolean fallback)
    {
        try
        {
            Object value = form.getClass().getField(field).get(form);
            Object flag = value.getClass().getMethod("get").invoke(value);

            if (flag instanceof Boolean b)
            {
                return b;
            }
        }
        catch (Throwable ignored)
        {}

        return fallback;
    }

    private static boolean ensureResolved()
    {
        if (present == Boolean.FALSE)
        {
            return false;
        }

        if (present == Boolean.TRUE)
        {
            return true;
        }

        try
        {
            pointClass = Class.forName(POINT_FORM);
            spotClass = Class.forName(SPOT_FORM);

            Class<?> registry = Class.forName(LIGHT_REGISTRY);
            Class<?> math = Class.forName(LIGHT_MATH);

            registerPoint = registry.getMethod("registerPoint",
                double.class, double.class, double.class,
                float.class, float.class, float.class,
                float.class, float.class,
                boolean.class, boolean.class,
                float.class, float.class, float.class, float.class, boolean.class,
                long.class
            );
            registerSpot = registry.getMethod("registerSpot",
                double.class, double.class, double.class,
                float.class, float.class, float.class,
                float.class, float.class, float.class,
                float.class, float.class, float.class, float.class,
                boolean.class, boolean.class,
                float.class, float.class, float.class, float.class, boolean.class,
                long.class
            );
            normalizeDir = math.getMethod("normalizeDir",
                float.class, float.class, float.class,
                float.class, float.class, float.class,
                Vector4f.class
            );
            coneFactory = math.getMethod("cone", float.class, float.class);

            Class<?> coneClass = coneFactory.getReturnType();

            coneCosOuter = coneClass.getMethod("cosOuter");
            coneCosInner = coneClass.getMethod("cosInner");
            present = Boolean.TRUE;

            return true;
        }
        catch (Throwable t)
        {
            present = Boolean.FALSE;
            pointClass = null;
            spotClass = null;
            registerPoint = null;
            registerSpot = null;
            normalizeDir = null;
            coneFactory = null;
            coneCosOuter = null;
            coneCosInner = null;

            return false;
        }
    }
}
