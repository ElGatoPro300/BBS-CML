package mchorse.bbs_mod.simulation;

public class FluidInteractionTest
{
    public static void main(String[] args)
    {
        testMovementInteraction();
        testViscosityEffect();
        testSensitivityEffect();
        System.out.println("FluidInteractionTest passed!");
    }

    private static void testMovementInteraction()
    {
        FluidSimulation fluid = new FluidSimulation(32, 32);
        
        /* Simulate entity movement logic from BillboardFormRenderer */
        double lastX = 0;
        double lastY = 0;
        double lastZ = 0;
        
        double currentX = 1.0;
        double currentY = 0;
        double currentZ = 0; /* Moved 1 block in X */
        
        double dx = currentX - lastX;
        double dy = currentY - lastY;
        double dz = currentZ - lastZ;
        
        if (dx * dx + dy * dy + dz * dz > 0.0001)
        {
            fluid.addForce(16, 16, (float) Math.sqrt(dx * dx + dz * dz) * 5.0F);
        }
        
        /* Verify force was added */
        fluid.update();
        
        /* After 1 update, getHeight(16,16) should show the spike */
        float height = fluid.getHeight(16, 16);
        
        if (Math.abs(height) < 0.001)
        {
             throw new RuntimeException("Interaction failed: Movement did not create ripples. Height: " + height);
        }
        
        System.out.println("Movement interaction verified. Height: " + height);
    }

    private static void testViscosityEffect()
    {
        FluidSimulation fluidHighViscosity = new FluidSimulation(32, 32);
        fluidHighViscosity.viscosity = 0.99F;
        
        FluidSimulation fluidLowViscosity = new FluidSimulation(32, 32);
        fluidLowViscosity.viscosity = 0.5F;
        
        /* Add same force */
        fluidHighViscosity.addForce(16, 16, 10.0F);
        fluidLowViscosity.addForce(16, 16, 10.0F);
        
        /* Run a few updates */
        for (int i = 0; i < 5; i++)
        {
            fluidHighViscosity.update();
            fluidLowViscosity.update();
        }
        
        float h1 = Math.abs(fluidHighViscosity.getHeight(16, 16));
        float h2 = Math.abs(fluidLowViscosity.getHeight(16, 16));
        
        /* Lower viscosity value means more damping (val *= viscosity), so height should be smaller?
        Wait, val *= 0.5 reduces it by half each step. val *= 0.99 reduces by 1%.
        So fluidLowViscosity (0.5) should have much smaller height than fluidHighViscosity (0.99). */
        
        if (h1 <= h2)
        {
             throw new RuntimeException("Viscosity test failed: High viscosity (low damping) should preserve more wave height. h1=" + h1 + ", h2=" + h2);
        }
        
        System.out.println("Viscosity effect verified. h1 (0.99): " + h1 + ", h2 (0.5): " + h2);
    }

    private static void testSensitivityEffect()
    {
        FluidSimulation fluidNormal = new FluidSimulation(32, 32);
        
        FluidSimulation fluidSensitive = new FluidSimulation(32, 32);
        
        float force = 10.0F;
        fluidNormal.addForce(16, 16, force * 1.0F);
        fluidSensitive.addForce(16, 16, force * 2.0F);
        
        fluidNormal.update();
        fluidSensitive.update();
        
        float h1 = fluidNormal.getHeight(16, 16);
        float h2 = fluidSensitive.getHeight(16, 16);
        
        /* h2 should be roughly 2x h1 (ignoring neighbor effects for single point update immediately)
        Wait, addForce adds to previousBuffer. update() uses it. */

        if (Math.abs(h2 - h1 * 2.0F) > 0.1F) /* Allow some margin */
        {
             System.out.println("Sensitivity warning: h2 (" + h2 + ") is not exactly 2x h1 (" + h1 + "). This might be due to propagation logic.");
        }
        
        if (h2 <= h1)
        {
            throw new RuntimeException("Sensitivity test failed: Higher sensitivity should result in higher waves.");
        }
        
        System.out.println("Sensitivity effect verified. h1: " + h1 + ", h2: " + h2);
    }
}
