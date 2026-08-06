package mchorse.bbs_mod.cubic.physics;

public class PhysBoneState
{
    private static final int MOTION_BUFFER_SIZE = 4;

    public float yaw;
    public float pitch;
    public float roll;
    public float yawVelocity;
    public float pitchVelocity;
    public float rollVelocity;
    public float prevStrafeMotion;
    public float prevForwardMotion;
    public float prevVerticalVelocity;
    public float prevParentYaw;
    public float prevParentPitch;
    public boolean parentInitialized;
    public boolean initialized;

    /* Motion smoothing ring buffer */
    public float[] strafeHistory = new float[MOTION_BUFFER_SIZE];
    public float[] forwardHistory = new float[MOTION_BUFFER_SIZE];
    public float[] verticalHistory = new float[MOTION_BUFFER_SIZE];
    public int historyIndex;
    public int historyCount;

    /* Lag spike detection */
    public long lastUpdateNanos;

    public float getSmoothedStrafe()
    {
        return this.computeSmoothedValue(this.strafeHistory);
    }

    public float getSmoothedForward()
    {
        return this.computeSmoothedValue(this.forwardHistory);
    }

    public float getSmoothedVertical()
    {
        return this.computeSmoothedValue(this.verticalHistory);
    }

    public void recordMotion(float strafe, float forward, float vertical)
    {
        this.strafeHistory[this.historyIndex] = strafe;
        this.forwardHistory[this.historyIndex] = forward;
        this.verticalHistory[this.historyIndex] = vertical;
        this.historyIndex = (this.historyIndex + 1) % MOTION_BUFFER_SIZE;

        if (this.historyCount < MOTION_BUFFER_SIZE)
        {
            this.historyCount++;
        }
    }

    public void resetMotionHistory()
    {
        this.historyIndex = 0;
        this.historyCount = 0;

        for (int i = 0; i < MOTION_BUFFER_SIZE; i++)
        {
            this.strafeHistory[i] = 0F;
            this.forwardHistory[i] = 0F;
            this.verticalHistory[i] = 0F;
        }
    }

    private float computeSmoothedValue(float[] buffer)
    {
        if (this.historyCount == 0)
        {
            return 0F;
        }

        float sum = 0F;
        float weight = 0F;
        int count = this.historyCount;

        for (int i = 0; i < count; i++)
        {
            /* Newer samples weighted more: 1, 2, 3, 4 */
            float w = (float) (i + 1);
            int idx = (this.historyIndex - count + i + MOTION_BUFFER_SIZE) % MOTION_BUFFER_SIZE;

            sum += buffer[idx] * w;
            weight += w;
        }

        return weight > 0F ? sum / weight : 0F;
    }
}
