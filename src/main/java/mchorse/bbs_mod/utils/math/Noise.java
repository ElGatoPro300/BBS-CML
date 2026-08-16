package mchorse.bbs_mod.utils.math;

import java.util.Random;

public class Noise
{
    private final int[] p = new int[512];
    private final int[] permutation = new int[256];

    public Noise(long seed)
    {
        this.setSeed(seed);
    }

    public void setSeed(long seed)
    {
        Random random = new Random(seed);

        for (int i = 0; i < 256; i++)
        {
            this.permutation[i] = i;
        }

        for (int i = 0; i < 256; i++)
        {
            int j = random.nextInt(256 - i) + i;
            int temp = this.permutation[i];

            this.permutation[i] = this.permutation[j];
            this.permutation[j] = temp;
            this.p[i] = this.p[i + 256] = this.permutation[i];
        }
    }

    public double noise(double x, double y, double z)
    {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        int Z = (int) Math.floor(z) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),
            grad(p[BA], x - 1, y, z)),
            lerp(u, grad(p[AB], x, y - 1, z),
                grad(p[BB], x - 1, y - 1, z))),
            lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),
                grad(p[BA + 1], x - 1, y, z - 1)),
                lerp(u, grad(p[AB + 1], x, y - 1, z - 1),
                    grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    public double voronoi(double x, double y, double z)
    {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);

        double minDist = 1.0;

        for (int xx = -1; xx <= 1; xx++)
        {
            for (int yy = -1; yy <= 1; yy++)
            {
                for (int zz = -1; zz <= 1; zz++)
                {
                    int gx = xi + xx;
                    int gy = yi + yy;
                    int gz = zi + zz;

                    double rx = gx + random(gx, gy, gz) - x;
                    double ry = gy + random(gx + 412, gy + 51, gz + 124) - y;
                    double rz = gz + random(gx + 124, gy + 412, gz + 51) - z;

                    double d = Math.sqrt(rx * rx + ry * ry + rz * rz);

                    if (d < minDist)
                    {
                        minDist = d;
                    }
                }
            }
        }

        return minDist;
    }

    private double random(int x, int y, int z)
    {
        int n = x + y * 57 + z * 131;
        n = (n << 13) ^ n;
        return (1.0 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0);
    }

    private static double fade(double t)
    {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b)
    {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z)
    {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
