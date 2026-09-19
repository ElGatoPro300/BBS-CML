package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

public class ReplayBatchProcessor
{
    public enum Operation
    {
        RANDOM,
        LINE,
        SHIFT,
        SQUARE,
        SQUARE_OUTLINE,
        CIRCLE,
        CIRCLE_OUTLINE,
        CUBE,
        SPHERE,
        FIT_HEIGHT,
        LOOK_AT
    }

    public enum Error
    {
        NEED_TWO_CHANNELS,
        NEED_THREE_CHANNELS,
        NEED_Y_CHANNEL,
        NEED_TARGET,
        NEED_POSITION_CHANNELS,
        INVALID_EXPRESSION,
        NO_WORLD
    }

    public static class VisibleReplay
    {
        public final Replay replay;
        public final int i;
        public final int o;

        public VisibleReplay(Replay replay, int i, int o)
        {
            this.replay = replay;
            this.i = i;
            this.o = o;
        }
    }

    public static class NormalParams
    {
        public double randomMin;
        public double randomMax;
        public double lineOffset;
        public double size;
        public double shift;
        public boolean fill;
        public Replay lookAtTarget;
        public GroundProvider groundProvider;
    }

    @FunctionalInterface
    public interface GroundProvider
    {
        double getGroundY(double x, double z);
    }

    public static Error applyAdvanced(List<VisibleReplay> selected, List<String> selectedProperties, String expressionText)
    {
        MathBuilder builder = new MathBuilder();

        builder.register("i");
        builder.register("o");
        builder.register("v");
        builder.register("ki");

        IExpression parse;

        try
        {
            parse = builder.parse(expressionText);
        }
        catch (Exception e)
        {
            return Error.INVALID_EXPRESSION;
        }

        for (VisibleReplay replay : selected)
        {
            builder.variables.get("i").set(replay.i);
            builder.variables.get("o").set(replay.o);

            for (String s : selectedProperties)
            {
                KeyframeChannel channel = (KeyframeChannel) replay.replay.keyframes.get(s);

                if (channel == null)
                {
                    continue;
                }

                List keyframes = channel.getKeyframes();

                for (int i = 0; i < keyframes.size(); i++)
                {
                    Keyframe kf = (Keyframe) keyframes.get(i);

                    builder.variables.get("v").set(kf.getFactory().getY(kf.getValue()));
                    builder.variables.get("ki").set(i);

                    kf.setValue(kf.getFactory().yToValue(parse.doubleValue()), true);
                }
            }
        }

        return null;
    }

    public static Error applyNormal(List<VisibleReplay> selected, List<String> selectedProperties, Operation operation, NormalParams params)
    {
        if (operation == null)
        {
            return null;
        }

        return switch (operation)
        {
            case SQUARE, SQUARE_OUTLINE, CIRCLE, CIRCLE_OUTLINE -> apply2DShape(selected, selectedProperties, operation, params);
            case CUBE, SPHERE -> apply3DShape(selected, selectedProperties, operation, params);
            case LINE -> applyLine(selected, selectedProperties, params);
            case SHIFT -> applyShift(selected, selectedProperties, params);
            case RANDOM -> applyRandom(selected, selectedProperties, params);
            case FIT_HEIGHT -> applyFitHeight(selected, selectedProperties, params);
            case LOOK_AT -> applyLookAt(selected, params);
        };
    }

    private static Error apply2DShape(List<VisibleReplay> selected, List<String> selectedProperties, Operation operation, NormalParams params)
    {
        if (selectedProperties.size() < 2)
        {
            return Error.NEED_TWO_CHANNELS;
        }

        String aId = selectedProperties.get(0);
        String bId = selectedProperties.get(1);

        int count = selected.size();

        for (VisibleReplay replay : selected)
        {
            int o = replay.o;
            double a = 0;
            double b = 0;

            if (operation == Operation.SQUARE)
            {
                int side = (int) Math.ceil(Math.sqrt(count));
                int rows = (int) Math.ceil(count / (double) side);
                double half = (side - 1) / 2D;
                double step = side > 1 ? params.size / (side - 1) : 0D;
                int col = o % side;
                int row = o / side;

                a = (col - half) * step;
                if (rows > 1)
                {
                    double rowScaled = row * (side - 1D) / (rows - 1D);
                    b = (rowScaled - half) * step;
                }
                else
                {
                    b = 0D;
                }
            }
            else if (operation == Operation.SQUARE_OUTLINE)
            {
                double size = params.size;
                double half = size / 2D;
                double s = count <= 1 ? 0D : (o / (double) count) * 4D;

                if (s < 1D)
                {
                    a = -half + size * s;
                    b = -half;
                }
                else if (s < 2D)
                {
                    a = half;
                    b = -half + size * (s - 1D);
                }
                else if (s < 3D)
                {
                    a = half - size * (s - 2D);
                    b = half;
                }
                else
                {
                    a = -half;
                    b = half - size * (s - 3D);
                }
            }
            else if (operation == Operation.CIRCLE)
            {
                double radius = params.size / 2D;
                double goldenAngle = Math.PI * (3D - Math.sqrt(5D));
                double t = (o + 0.5D) / count;
                double r = Math.sqrt(t) * radius;
                double angle = o * goldenAngle;

                a = Math.cos(angle) * r;
                b = Math.sin(angle) * r;
            }
            else if (operation == Operation.CIRCLE_OUTLINE)
            {
                double radius = params.size / 2D;
                double angle = (count == 1 ? 0D : (o / (double) count) * Math.PI * 2D);

                a = Math.cos(angle) * radius;
                b = Math.sin(angle) * radius;
            }

            applyDelta(replay.replay, aId, a);
            applyDelta(replay.replay, bId, b);
        }

        return null;
    }

    private static Error apply3DShape(List<VisibleReplay> selected, List<String> selectedProperties, Operation operation, NormalParams params)
    {
        if (selectedProperties.size() < 3)
        {
            return Error.NEED_THREE_CHANNELS;
        }

        String aId = selectedProperties.get(0);
        String bId = selectedProperties.get(1);
        String cId = selectedProperties.get(2);

        int count = selected.size();

        if (operation == Operation.CUBE)
        {
            int side;
            int[] shellCoords = null;

            if (params.fill)
            {
                side = (int) Math.ceil(Math.cbrt(count));
            }
            else
            {
                side = 2;
                while (count > cubeSurfacePoints(side))
                {
                    side++;
                }

                shellCoords = buildCubeShellCoords(count, side);
            }

            double half = (side - 1) / 2D;
            double step = side > 1 ? params.size / (side - 1) : 0D;

            for (VisibleReplay replay : selected)
            {
                int o = replay.o;
                int gx;
                int gy;
                int gz;

                if (params.fill)
                {
                    gx = o % side;
                    gy = (o / side) % side;
                    gz = o / (side * side);
                }
                else
                {
                    int i = o * 3;
                    gx = shellCoords[i];
                    gy = shellCoords[i + 1];
                    gz = shellCoords[i + 2];
                }

                double a = (gx - half) * step;
                double b = (gy - half) * step;
                double c = (gz - half) * step;

                applyDelta(replay.replay, aId, a);
                applyDelta(replay.replay, bId, b);
                applyDelta(replay.replay, cId, c);
            }
        }
        else
        {
            double radius = params.size / 2D;
            double goldenAngle = Math.PI * (3D - Math.sqrt(5D));

            for (VisibleReplay replay : selected)
            {
                int o = replay.o;
                double t = (o + 0.5D) / count;
                double theta = goldenAngle * (o + 0.5D);
                double y = 1D - 2D * t;
                double rxy = Math.sqrt(1D - y * y);
                double s = params.fill ? Math.cbrt(t) : 1D;
                double x = Math.cos(theta) * rxy;
                double z = Math.sin(theta) * rxy;

                double a = x * radius * s;
                double b = y * radius * s;
                double c = z * radius * s;

                applyDelta(replay.replay, aId, a);
                applyDelta(replay.replay, bId, b);
                applyDelta(replay.replay, cId, c);
            }
        }

        return null;
    }

    private static Error applyLine(List<VisibleReplay> selected, List<String> selectedProperties, NormalParams params)
    {
        for (VisibleReplay replay : selected)
        {
            double delta = replay.o * params.lineOffset;

            for (String s : selectedProperties)
            {
                applyDelta(replay.replay, s, delta);
            }
        }

        return null;
    }

    private static Error applyShift(List<VisibleReplay> selected, List<String> selectedProperties, NormalParams params)
    {
        for (VisibleReplay replay : selected)
        {
            for (String s : selectedProperties)
            {
                applyDelta(replay.replay, s, params.shift);
            }
        }

        return null;
    }

    private static Error applyRandom(List<VisibleReplay> selected, List<String> selectedProperties, NormalParams params)
    {
        double minValue = params.randomMin;
        double maxValue = params.randomMax;
        double minRand = Math.min(minValue, maxValue);
        double maxRand = Math.max(minValue, maxValue);
        long seed = ThreadLocalRandom.current().nextLong();

        for (VisibleReplay replay : selected)
        {
            long replaySeed = mix64(seed + (long) replay.o * 0x9e3779b97f4a7c15L);

            for (int ci = 0; ci < selectedProperties.size(); ci++)
            {
                String s = selectedProperties.get(ci);
                long channelSeed = mix64(replaySeed + (long) ci * 0xbf58476d1ce4e5b9L);
                SplittableRandom random = new SplittableRandom(channelSeed);
                double delta = minRand + random.nextDouble() * (maxRand - minRand);

                applyDelta(replay.replay, s, delta);
            }
        }

        return null;
    }

    private static Error applyFitHeight(List<VisibleReplay> selected, List<String> selectedProperties, NormalParams params)
    {
        if (!selectedProperties.contains("y"))
        {
            return Error.NEED_Y_CHANNEL;
        }

        if (params.groundProvider == null)
        {
            return Error.NO_WORLD;
        }

        return fitHeightToGround(selected, params.groundProvider);
    }

    private static Error applyLookAt(List<VisibleReplay> selected, NormalParams params)
    {
        if (params.lookAtTarget == null)
        {
            return Error.NEED_TARGET;
        }

        return lookAt(selected, params.lookAtTarget);
    }

    private static Error fitHeightToGround(List<VisibleReplay> selected, GroundProvider groundProvider)
    {
        for (VisibleReplay replay : selected)
        {
            KeyframeChannel xChannel = (KeyframeChannel) replay.replay.keyframes.get("x");
            KeyframeChannel yChannel = (KeyframeChannel) replay.replay.keyframes.get("y");
            KeyframeChannel zChannel = (KeyframeChannel) replay.replay.keyframes.get("z");

            if (xChannel == null || yChannel == null || zChannel == null)
            {
                return Error.NEED_POSITION_CHANNELS;
            }

            double x = xChannel.getFactory().getY(xChannel.interpolate(0F));
            double y = yChannel.getFactory().getY(yChannel.interpolate(0F));
            double z = zChannel.getFactory().getY(zChannel.interpolate(0F));
            double groundY = groundProvider.getGroundY(x, z);

            if (!Double.isNaN(groundY))
            {
                applyDelta(replay.replay, "y", groundY - y);
            }
        }

        return null;
    }

    private static Error lookAt(List<VisibleReplay> selected, Replay target)
    {
        KeyframeChannel targetX = (KeyframeChannel) target.keyframes.get("x");
        KeyframeChannel targetY = (KeyframeChannel) target.keyframes.get("y");
        KeyframeChannel targetZ = (KeyframeChannel) target.keyframes.get("z");

        if (targetX == null || targetY == null || targetZ == null)
        {
            return Error.NEED_POSITION_CHANNELS;
        }

        for (VisibleReplay replay : selected)
        {
            if (replay.replay == target)
            {
                continue;
            }

            KeyframeChannel srcX = (KeyframeChannel) replay.replay.keyframes.get("x");
            KeyframeChannel srcY = (KeyframeChannel) replay.replay.keyframes.get("y");
            KeyframeChannel srcZ = (KeyframeChannel) replay.replay.keyframes.get("z");

            if (srcX == null || srcY == null || srcZ == null)
            {
                return Error.NEED_POSITION_CHANNELS;
            }

            KeyframeChannel yaw = (KeyframeChannel) replay.replay.keyframes.get("yaw");
            KeyframeChannel pitch = (KeyframeChannel) replay.replay.keyframes.get("pitch");

            if (yaw == null || pitch == null)
            {
                continue;
            }

            applyLookAtToChannel(yaw, srcX, srcY, srcZ, targetX, targetY, targetZ, true);
            applyLookAtToChannel(pitch, srcX, srcY, srcZ, targetX, targetY, targetZ, false);

            KeyframeChannel headYaw = (KeyframeChannel) replay.replay.keyframes.get("headYaw");
            if (headYaw != null)
            {
                applyLookAtToChannel(headYaw, srcX, srcY, srcZ, targetX, targetY, targetZ, true);
            }

            KeyframeChannel bodyYaw = (KeyframeChannel) replay.replay.keyframes.get("bodyYaw");
            if (bodyYaw != null)
            {
                applyLookAtToChannel(bodyYaw, srcX, srcY, srcZ, targetX, targetY, targetZ, true);
            }
        }

        return null;
    }

    private static void applyLookAtToChannel(KeyframeChannel channel, KeyframeChannel srcX, KeyframeChannel srcY, KeyframeChannel srcZ, KeyframeChannel targetX, KeyframeChannel targetY, KeyframeChannel targetZ, boolean yaw)
    {
        List keyframes = channel.getKeyframes();

        if (keyframes.isEmpty())
        {
            double value = computeLookAtValue(0F, srcX, srcY, srcZ, targetX, targetY, targetZ, yaw);
            channel.insert(0F, channel.getFactory().yToValue(value));
            return;
        }

        double prevAngle = Double.NaN;

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe kf = (Keyframe) keyframes.get(i);
            float tick = (float) kf.getTick();
            double value = computeLookAtValue(tick, srcX, srcY, srcZ, targetX, targetY, targetZ, yaw);

            if (!Double.isNaN(prevAngle))
            {
                double diff = (value - prevAngle) % 360D;
                if (diff < -180D)
                {
                    diff += 360D;
                }
                else if (diff > 180D)
                {
                    diff -= 360D;
                }
                value = prevAngle + diff;
            }

            prevAngle = value;
            kf.setValue(kf.getFactory().yToValue(value));
        }
    }

    private static double computeLookAtValue(float tick, KeyframeChannel srcX, KeyframeChannel srcY, KeyframeChannel srcZ, KeyframeChannel targetX, KeyframeChannel targetY, KeyframeChannel targetZ, boolean isYaw)
    {
        double sx = srcX.getFactory().getY(srcX.interpolate(tick));
        double sy = srcY.getFactory().getY(srcY.interpolate(tick));
        double sz = srcZ.getFactory().getY(srcZ.interpolate(tick));
        double tx = targetX.getFactory().getY(targetX.interpolate(tick));
        double ty = targetY.getFactory().getY(targetY.interpolate(tick));
        double tz = targetZ.getFactory().getY(targetZ.interpolate(tick));

        double dx = tx - sx;
        double dy = ty - sy;
        double dz = tz - sz;

        if (isYaw)
        {
            return Math.atan2(dz, dx) * (180D / Math.PI) - 90D;
        }

        double h = Math.sqrt(dx * dx + dz * dz);
        return -Math.atan2(dy, h) * (180D / Math.PI);
    }

    private static void applyDelta(Replay replay, String id, double delta)
    {
        KeyframeChannel channel = (KeyframeChannel) replay.keyframes.get(id);

        if (channel == null || !KeyframeFactories.isNumeric(channel.getFactory()))
        {
            return;
        }

        List keyframes = channel.getKeyframes();

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe kf = (Keyframe) keyframes.get(i);
            double v = kf.getFactory().getY(kf.getValue());

            kf.setValue(kf.getFactory().yToValue(v + delta), true);
        }
    }

    private static long mix64(long z)
    {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int cubeSurfacePoints(int side)
    {
        if (side <= 1)
        {
            return 1;
        }

        int inner = Math.max(0, side - 2);

        return side * side * side - inner * inner * inner;
    }

    private static int[] buildCubeShellCoords(int count, int side)
    {
        int[] out = new int[count * 3];
        int i = 0;

        for (int z = 0; z < side && i < count; z++)
        {
            for (int y = 0; y < side && i < count; y++)
            {
                for (int x = 0; x < side && i < count; x++)
                {
                    if (x != 0 && x != side - 1 && y != 0 && y != side - 1 && z != 0 && z != side - 1)
                    {
                        continue;
                    }

                    int j = i * 3;

                    out[j] = x;
                    out[j + 1] = y;
                    out[j + 2] = z;
                    i++;
                }
            }
        }

        return out;
    }
}
