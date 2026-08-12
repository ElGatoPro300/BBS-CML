package mchorse.bbs_mod.simulation;

public class FluidSimulation
{
    private float[] buffer1;
    private float[] buffer2;
    private int width;
    private int height;
    public float viscosity = 0.96f;

    public FluidSimulation(int width, int height)
    {
        this.width = width;
        this.height = height;
        this.buffer1 = new float[width * height];
        this.buffer2 = new float[width * height];
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public void update()
    {
        for (int i = this.width; i < this.width * this.height - this.width; i++)
        {
            /* Check boundaries to avoid wrapping artifacts */
            int x = i % this.width;
            if (x == 0 || x == this.width - 1) continue;

            float val = (this.buffer1[i - 1] +
                         this.buffer1[i + 1] +
                         this.buffer1[i - this.width] +
                         this.buffer1[i + this.width]) / 2.0f - this.buffer2[i];
            
            val *= this.viscosity;
            this.buffer2[i] = val;
        }

        /* Swap buffers */
        float[] temp = this.buffer1;
        this.buffer1 = this.buffer2;
        this.buffer2 = temp;
    }

    public void addForce(int x, int y, float force)
    {
        if (x > 0 && x < this.width - 1 && y > 0 && y < this.height - 1)
        {
            this.buffer1[x + y * this.width] += force;
        }
    }

    public float getHeight(int x, int y)
    {
        if (x >= 0 && x < this.width && y >= 0 && y < this.height)
        {
            return this.buffer1[x + y * this.width];
        }
        
        return 0.0f;
    }
}
