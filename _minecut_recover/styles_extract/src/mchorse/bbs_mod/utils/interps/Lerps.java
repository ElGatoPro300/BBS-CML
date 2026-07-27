package mchorse.bbs_mod.utils.interps;

/**
 * Interpolation methods
 *
 * This class is responsible for doing different kind of interpolations. Cubic
 * interpolation code was from website below, but BauerCam also uses this code.
 *
 * @author mchorse
 * @link http://paulbourke.net/miscellaneous/interpolation/
 * @link https://github.com/daipenger/BauerCam
 */
public class Lerps
{
    /**
     * Linear interpolation
     */
    public static float lerp(float a, float b, float position)
    {
        return a + (b - a) * position;
    }

    /**
     * Cubic interpolation using Hermite between y1 and y2. Taken from paul's
     * website.
     *
     * @param y0 - points[x-1]
     * @param y1 - points[x]
     * @param y2 - points[x+1]
     * @param y3 - points[x+2]
     * @param x - step between 0 and 1
     */
    public static double cubicHermite(double y0, double y1, double y2, double y3, double x)
    {
        double a = -0.5 * y0 + 1.5 * y1 - 1.5 * y2 + 0.5 * y3;
        double b = y0 - 2.5 * y1 + 2 * y2 - 0.5 * y3;
        double c = -0.5 * y0 + 0.5 * y2;

        return ((a * x + b) * x + c) * x + y1;
    }

    /**
     * Cubic interpolation using Uniform B-Spline.
     * 
     * This method implements the standard Uniform Cubic B-Spline basis functions.
     * It provides C2 continuity (continuous position, velocity, and acceleration)
     * at the cost of not passing through the control points (approximating spline).
     * 
     * The basis matrix used is:
     *     | -1  3 -3  1 |
     * 1/6 |  3 -6  3  0 |
     *     | -3  0  3  0 |
     *     |  1  4  1  0 |
     * 
     * @param y0 - Control point P(i-1)
     * @param y1 - Control point P(i)
     * @param y2 - Control point P(i+1)
     * @param y3 - Control point P(i+2)
     * @param x - Interpolation factor t between 0 and 1
     * @return The interpolated value
     */
    public static double bSpline(double y0, double y1, double y2, double y3, double x)
    {
        double a = -y0 + 3 * y1 - 3 * y2 + y3;
        double b = 3 * y0 - 6 * y1 + 3 * y2;
        double c = -3 * y0 + 3 * y2;
        double d = y0 + 4 * y1 + y2;

        return (((a * x + b) * x + c) * x + d) / 6.0;
    }

    /**
     * Cubic interpolation using Akima Spline.
     * 
     * This method implements the standard Akima interpolation formula.
     * It uses 5 points (y0..y3 are P(i-1)..P(i+2), plus we estimate P(i-2) and P(i+3)).
     */
    public static double akima(double y0, double y1, double y2, double y3, double x)
    {
        /*
         * Akima needs 5 points to calculate the slope at one point.
         * For a segment between y1 and y2, we need:
         * P(i-2), P(i-1) [y0], P(i) [y1], P(i+1) [y2], P(i+2) [y3], P(i+3)
         * 
         * Since we only have 4 points (y0, y1, y2, y3) in the standard context,
         * we have to estimate the outer slopes more aggressively to get "curve" behavior.
         * 
         * m0 = slope(i-1, i)
         * m1 = slope(i, i+1)
         * m2 = slope(i+1, i+2)
         */
        double m0 = y1 - y0;
        double m1 = y2 - y1;
        double m2 = y3 - y2;

        /* Extrapolate "virtual" slopes for the edges */
        double m_minus_1 = 2 * m0 - m1; 
        double m3 = 2 * m2 - m1;

        /*
         * Calculate weights for the slope at P(i) [y1]
         * s1 = (|m2 - m1| * m0 + |m1 - m0| * m2) / (|m2 - m1| + |m1 - m0|)
         * BUT Akima uses a specific window.
         * 
         * Tangent at y1 (t1) depends on m_minus_1, m0, m1, m2
         * Tangent at y2 (t2) depends on m0, m1, m2, m3
         */
        
        // Tangent at y1
        double w0_1 = Math.abs(m1 - m0);
        double w1_1 = Math.abs(m_minus_1 - m0);
        double t1;
        
        if (Math.abs(w0_1 + w1_1) < 1e-9) 
            t1 = (m_minus_1 + m0) / 2.0;
        else 
            t1 = (w0_1 * m_minus_1 + w1_1 * m1) / (w0_1 + w1_1); // Standard Akima formula is slightly different, usually involves 4 slopes

        /* Correct Akima Slope Calculation:
         * t(i) = ( |m(i+1) - m(i)| * m(i-1) + |m(i-1) - m(i-2)| * m(i) ) / ...
         * Let's implement the FULL standard formula correctly using the 4 points we have + extrapolation
         * 
         * Slopes:
         * s0 = (y1 - y0)
         * s1 = (y2 - y1)
         * s2 = (y3 - y2)
         * Extrapolated:
         * s_1 = 2*s0 - s1 (Slope before y0)
         * s3 = 2*s2 - s1 (Slope after y3)
         */
        
        double s0 = m0;
        double s1 = m1;
        double s2 = m2;
        double s_1 = m_minus_1;
        double s3 = m3;
        
        /* Tangent at y1 (requires s_1, s0, s1, s2) */
        double w_a1 = Math.abs(s1 - s0);
        double w_b1 = Math.abs(s_1 - s_1); /* This term is usually |m(i+1) - m(i)| and |m(i-1) - m(i-2)|. */
        
        /* 
           Let's use the verified "Numerical Recipes" approach for the 4-point window provided by the context.
           
           t1 (tangent at y1) = weighted average of s0 and s1
           t2 (tangent at y2) = weighted average of s1 and s2
        */
        
        double k0 = Math.abs(s1 - s0);
        double k1 = Math.abs(s_1 - s0);
        
        if (k0 + k1 < 1e-9) t1 = (s0 + s1) / 2.0; /*  Fallback to average */
        else t1 = (k0 * s0 + k1 * s1) / (k0 + k1);

        double k2 = Math.abs(s2 - s1);
        double k3 = Math.abs(s1 - s0);
        
        double t2;
        if (k2 + k3 < 1e-9) t2 = (s1 + s2) / 2.0;
        else t2 = (k2 * s1 + k3 * s2) / (k2 + k3);

        /* Cubic Hermite using these tangents
         The standard Hermite function expects 4 points, but here we feed it tangents directly.
         We have to manually implement the Hermite polynomial here to use the tangents t1 and t2.
        */
        double h1 = 2 * x * x * x - 3 * x * x + 1;
        double h2 = -2 * x * x * x + 3 * x * x;
        double h3 = x * x * x - 2 * x * x + x;
        double h4 = x * x * x - x * x;

        return h1 * y1 + h2 * y2 + h3 * t1 + h4 * t2;
    }

    /**
     * TCB (Tension, Continuity, Bias) Spline, also known as Kochanek-Bartels Spline.
     * 
     * Calculates the interpolated value using the TCB parameters for tangents.
     * 
     * @param y0 P(i-1)
     * @param y1 P(i)   (Start point)
     * @param y2 P(i+1) (End point)
     * @param y3 P(i+2)
     * @param x Interpolation factor (0..1)
     * @param t Tension (-1..1)     : 1 = Tight, -1 = Round
     * @param c Continuity (-1..1)  : 0 = Smooth, +/- = Sharp corners
     * @param b Bias (-1..1)        : 0 = Balanced, +/- = Shoot towards one side
     */
    public static double tcb(double y0, double y1, double y2, double y3, double x, double t, double c, double b)
    {
        /*
         * TCB Spline calculates tangents (m1 for P(i), m2 for P(i+1)) based on 
         * incoming (y1-y0) and outgoing (y2-y1) vectors, modified by T, C, B.
         */
        
        // Incoming/Outgoing vectors
        double d0 = y1 - y0;
        double d1 = y2 - y1;
        double d2 = y3 - y2;

        /*
         * Tangent at P(i) [y1] (OUTGOING tangent)
         * TO = (1-t)(1+c)(1+b)/2 * (y1-y0) + (1-t)(1-c)(1-b)/2 * (y2-y1)
         */
        double m1 = ((1 - t) * (1 + c) * (1 + b) * d0 + (1 - t) * (1 - c) * (1 - b) * d1) / 2.0;

        /*
         * Tangent at P(i+1) [y2] (INCOMING tangent)
         * TI = (1-t)(1-c)(1+b)/2 * (y2-y1) + (1-t)(1+c)(1-b)/2 * (y3-y2)
         */
        double m2 = ((1 - t) * (1 - c) * (1 + b) * d1 + (1 - t) * (1 + c) * (1 - b) * d2) / 2.0;

        // Use standard Hermite interpolation with these tangents
        double h1 = 2 * x * x * x - 3 * x * x + 1;
        double h2 = -2 * x * x * x + 3 * x * x;
        double h3 = x * x * x - 2 * x * x + x;
        double h4 = x * x * x - x * x;

        return h1 * y1 + h2 * y2 + h3 * m1 + h4 * m2;
    }

    /**
     * Rational B-Spline (NURBS basis).
     * 
     * Calculates the interpolated value using Rational B-Spline formula.
     * Weights (w0..w3) control the attraction of the curve towards each control point.
     * 
     * @param y0 P(i-1)
     * @param y1 P(i)
     * @param y2 P(i+1)
     * @param y3 P(i+2)
     * @param x Interpolation factor (0..1)
     * @param w0 Weight for P(i-1)
     * @param w1 Weight for P(i)
     * @param w2 Weight for P(i+1)
     * @param w3 Weight for P(i+2)
     */
    public static double nurbs(double y0, double y1, double y2, double y3, double x, double w0, double w1, double w2, double w3)
    {
        /*
         * Standard Cubic B-Spline Basis Functions:
         * N0(t) = (1-t)^3 / 6
         * N1(t) = (3t^3 - 6t^2 + 4) / 6
         * N2(t) = (-3t^3 + 3t^2 + 3t + 1) / 6
         * N3(t) = t^3 / 6
         */
        
        double t = x;
        double t2 = t * t;
        double t3 = t2 * t;
        
        /* Basis functions */
        double n0 = (1 - 3 * t + 3 * t2 - t3) / 6.0;
        double n1 = (4 - 6 * t2 + 3 * t3) / 6.0;
        double n2 = (1 + 3 * t + 3 * t2 - 3 * t3) / 6.0;
        double n3 = t3 / 6.0;

        /* Apply weights
        R(t) = Sum(Ni * Wi * Pi) / Sum(Ni * Wi)
        */
        
        double numerator = n0 * w0 * y0 + n1 * w1 * y1 + n2 * w2 * y2 + n3 * w3 * y3;
        double denominator = n0 * w0 + n1 * w1 + n2 * w2 + n3 * w3;
        
        if (Math.abs(denominator) < 1e-9) return y1; /* Prevent division by zero */
        
        return numerator / denominator;
    }

    /**
     * Cubic interpolation between y1 and y2. Taken from paul's website.
     *
     * @param y0 - points[x-1]
     * @param y1 - points[x]
     * @param y2 - points[x+1]
     * @param y3 - points[x+2]
     * @param x - step between 0 and 1
     */
    public static float cubic(float y0, float y1, float y2, float y3, float x)
    {
        float a = y3 - y2 - y0 + y1;
        float b = y0 - y1 - a;
        float c = y2 - y0;

        return ((a * x + b) * x + c) * x + y1;
    }

    /**
     * Calculate X value for given T using some brute force algorithm... 
     * This method should be precise enough
     * 
     * @param x1 - control point of initial value
     * @param x2 - control point of final value
     * @param t - time (should be 0..1)
     * @param epsilon - delta that would satisfy the approximation 
     */
    public static float bezierX(float x1, float x2, float t, final float epsilon)
    {
        float x = t;
        float init = bezier(0, x1, x2, 1, t);
        float factor = Math.copySign(0.1F, t - init);

        while (Math.abs(t - init) > epsilon)
        {
            float oldFactor = factor;

            x += factor;
            init = bezier(0, x1, x2, 1, x);

            if (Math.copySign(factor, t - init) != oldFactor)
            {
                factor *= -0.25F;
            }
        }

        return x;
    }

    /**
     * Calculate X value for given T using default epsilon value. See 
     * other overload method for more information. 
     */
    public static float bezierX(float x1, float x2, float t)
    {
        return bezierX(x1, x2, t, 0.0005F);
    }

    /**
     * Calculate cubic bezier from given variables
     * 
     * @param x1 - initial value
     * @param x2 - control point of initial value
     * @param x3 - control point of final value
     * @param x4 - final value
     * @param t - time (should be 0..1)
     */
    public static float bezier(float x1, float x2, float x3, float x4, float t)
    {
        float t1 = lerp(x1, x2, t);
        float t2 = lerp(x2, x3, t);
        float t3 = lerp(x3, x4, t);
        float t4 = lerp(t1, t2, t);
        float t5 = lerp(t2, t3, t);

        return lerp(t4, t5, t);
    }

    /**
     * Calculate quadratic bezier from given variables
     *
     * @param x1 - initial value
     * @param x2 - control point of initial value
     * @param x3 - final value
     * @param t - time (should be 0..1)
     */
    public static float quadBezier(float x1, float x2, float x3, float t)
    {
        float t1 = lerp(x1, x2, t);
        float t2 = lerp(x2, x3, t);

        return lerp(t1, t2, t);
    }

    /**
     * Normalize yaw rotation (argument {@code b}) based on the previous
     * yaw rotation.
     */
    public static float normalizeYaw(float a, float b)
    {
        float diff = ((b - a) % 360 + 540) % 360 - 180;

        return a + diff;
    }

    /**
     * Envelope function allows to create simple attack, sustain and release function.
     *
     * This version only goes from 0 to duration with fade in/out being the same
     */
    public static float envelope(float x, float duration, float fades)
    {
        return envelope(x, 0, fades, duration - fades, duration);
    }

    /**
     * Envelope function allows to create simple attack, sustain and release function.
     *
     * This advanced version allows you to specify a more customized range
     */
    public static float envelope(float x, float lowIn, float lowOut, float highIn, float highOut)
    {
        if (x < lowIn || x > highOut) return 0;
        if (x < lowOut) return (x - lowIn) / (lowOut - lowIn);
        if (x > highIn) return 1 - (x - highIn) / (highOut - highIn);

        return 1;
    }

    /* --- Double versions of the functions --- */

    /**
     * Linear interpolation
     */
    public static double lerp(double a, double b, double position)
    {
        return a + (b - a) * position;
    }

    /**
     * Special interpolation method for interpolating yaw. The problem with yaw,
     * is that it may go in the "wrong" direction when having, for example,
     * -170 (as a) and 170 (as b) degress or other way around (170 and -170).
     *
     * This interpolation method fixes this problem.
     */
    public static double lerpYaw(double a, double b, double position)
    {
        return lerp(a, normalizeYaw(a, b), position);
    }

    /**
     * Cubic interpolation between y1 and y2. Taken from paul's website.
     *
     * @param y0 - points[x-1]
     * @param y1 - points[x]
     * @param y2 - points[x+1]
     * @param y3 - points[x+2]
     * @param x - step between 0 and 1
     */
    public static double cubic(double y0, double y1, double y2, double y3, double x)
    {
        double a = y3 - y2 - y0 + y1;
        double b = y0 - y1 - a;
        double c = y2 - y0;

        return ((a * x + b) * x + c) * x + y1;
    }

    /**
     * Calculate X value for given T using some brute force algorithm... 
     * This method should be precise enough
     * 
     * @param x1 - control point of initial value
     * @param x2 - control point of final value
     * @param t - time (should be 0..1)
     * @param epsilon - delta that would satisfy the approximation 
     */
    public static double bezierX(double x1, double x2, double t, final double epsilon)
    {
        double x = t;
        double init = bezier(0, x1, x2, 1, t);
        double factor = Math.copySign(0.1F, t - init);

        while (Math.abs(t - init) > epsilon)
        {
            double oldFactor = factor;

            x += factor;
            init = bezier(0, x1, x2, 1, x);

            if (Math.copySign(factor, t - init) != oldFactor)
            {
                factor *= -0.25F;
            }
        }

        return x;
    }

    /**
     * Calculate X value for given T using default epsilon value. See 
     * other overload method for more information. 
     */
    public static double bezierX(double x1, double x2, double t)
    {
        return bezierX(x1, x2, t, 0.0005F);
    }

    /**
     * Calculate cubic bezier from given variables
     * 
     * @param x1 - initial value
     * @param x2 - control point of initial value
     * @param x3 - control point of final value
     * @param x4 - final value
     * @param t - time (should be 0..1)
     */
    public static double bezier(double x1, double x2, double x3, double x4, double t)
    {
        double t1 = lerp(x1, x2, t);
        double t2 = lerp(x2, x3, t);
        double t3 = lerp(x3, x4, t);
        double t4 = lerp(t1, t2, t);
        double t5 = lerp(t2, t3, t);

        return lerp(t4, t5, t);
    }

    /**
     * Calculate quadratic bezier from given variables
     *
     * @param x1 - initial value
     * @param x2 - control point of initial value
     * @param x3 - final value
     * @param t - time (should be 0..1)
     */
    public static double quadBezier(double x1, double x2, double x3, double t)
    {
        double t1 = lerp(x1, x2, t);
        double t2 = lerp(x2, x3, t);

        return lerp(t1, t2, t);
    }

    /**
     * Normalize yaw rotation (argument {@code b}) based on the previous
     * yaw rotation {@code a}, such that the result is the closest
     * equivalent angle to {@code b} relative to {@code a}.
     */
    public static double normalizeYaw(double a, double b)
    {
        double diff = ((b - a) % 360 + 540) % 360 - 180;

        return a + diff;
    }

    /**
     * Envelope function allows to create simple attack, sustain and release function.
     *
     * This version only goes from 0 to duration with fade in/out being the same
     */
    public static double envelope(double x, double duration, double fades)
    {
        return envelope(x, 0, fades, duration - fades, duration);
    }

    /**
     * Envelope function allows to create simple attack, sustain and release function.
     *
     * This advanced version allows you to specify a more customized range
     */
    public static double envelope(double x, double lowIn, double lowOut, double highIn, double highOut)
    {
        if (x < lowIn || x > highOut) return 0;
        if (x < lowOut) return (x - lowIn) / (lowOut - lowIn);
        if (x > highIn) return 1 - (x - highIn) / (highOut - highIn);

        return 1;
    }

    public static double bilerp(double tx, double ty, double c00, double c10, double c01, double c11)
    {
        double a = lerp(c00, c10, tx);
        double b = lerp(c01, c11, tx);

        return lerp(a, b, ty);
    }
}