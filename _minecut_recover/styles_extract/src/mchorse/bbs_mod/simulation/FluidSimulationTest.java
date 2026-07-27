package mchorse.bbs_mod.simulation;

public class FluidSimulationTest
{
    public static void main(String[] args)
    {
        testInitialization();
        testUpdate();
        testInteraction();
        System.out.println("All tests passed!");
    }

    private static void testInitialization()
    {
        FluidSimulation fluid = new FluidSimulation(10, 10);
        if (fluid.getWidth() != 10 || fluid.getHeight() != 10)
        {
            throw new RuntimeException("Initialization failed: Wrong dimensions");
        }
        System.out.println("testInitialization passed");
    }

    private static void testUpdate()
    {
        FluidSimulation fluid = new FluidSimulation(10, 10);
        fluid.addForce(5, 5, 10.0f);
        
        /* Initial state check
         Note: addForce updates previousBuffer, so currentBuffer is still 0 until update()
        But update() swaps buffers. */
        
        fluid.update();
        
        /* After one update, the force should propagate */
        /* Check center */
        float height = fluid.getHeight(5, 5);
        if (height == 0)
        {
            throw new RuntimeException("Update failed: Force did not propagate");
        }
        System.out.println("testUpdate passed");
    }

    private static void testInteraction()
    {
        FluidSimulation fluid = new FluidSimulation(10, 10);
        fluid.addForce(5, 5, 1.0f * 2.0f);
        
        /* Check if sensitivity is applied (internal logic dependent, but assuming addForce uses it)
         In my impl: this.previousBuffer[index] += force * this.sensitivity;
         So previousBuffer should be 2.0 */
        
        /* Accessing private fields is hard without reflection, but we can verify effect after update */
        fluid.update();
        
        /* The value should be roughly related to input * sensitivity
         val = (neighbors)/2 - current
         neighbors are 0 initially.
         Wait, wave equation: 
         val = (prev[i-1] + ...)/2 - curr[i][j]
         If we added force to prev buffer at (5,5), then at next update:
         (5,5) calculation depends on neighbors in prev buffer (which are 0)
         So (5,5) becomes 0?
         Wait.
         current[i][j] = (prev[left] + prev[right] + prev[up] + prev[down]) / 2 - current[i][j]
         If we add to prev[5][5], then neighbors of (5,5) will pick it up in the NEXT step.
         The point (5,5) itself:
         val = (0+0+0+0)/2 - 0 = 0.
         So (5,5) stays 0 in the first frame!
         But (4,5) will see prev[5][5] (if it's a neighbor).
         My loop:
         for i=1..w-1
         val = (prev[i-1]...)
         At (4,5), neighbor (i+1) is (5,5). prev[5][5] has value.
         So (4,5) should become non-zero. */
        
        if (fluid.getHeight(4, 5) == 0 && fluid.getHeight(6, 5) == 0)
        {
             /* It might be that 1 update is not enough or my understanding of the wave algo implementation needs check.
              Let's check implementation. */
        }
        
        /* My impl:
           this.previousBuffer[index] += force * this.sensitivity;
           update():
           val = (prev[neighbors]) / 2 - current
           current = val
           swap buffers
        
           Step 1: addForce at 5,5. prev[5,5] = 2.0. current is all 0.
           Step 2: update()
           Loop calculates new values for currentBuffer based on previousBuffer.
           current[4,5] calculation: uses prev[5,5] (neighbor). So current[4,5] becomes non-zero.
           current[5,5] calculation: uses prev[4,5], prev[6,5], etc. (all 0). So current[5,5] becomes 0.
           End of loop: swap buffers.
           now previousBuffer has the calculated values (ripples), currentBuffer has the OLD previousBuffer (with the spike at 5,5).
           getHeight returns currentBuffer.
        
           So getHeight(5,5) should be 2.0 (the spike we put in prev, which is now current).
           Wait, swap:
           temp = prev; prev = curr; curr = temp;
           currentBuffer was the one we just wrote to (the ripples).
           So currentBuffer has ripples. previousBuffer has the spike.
           getHeight returns currentBuffer value.
           So current[5,5] is 0. current[4,5] is non-zero. 
        */
        
        if (fluid.getHeight(4, 5) == 0)
        {
             throw new RuntimeException("Interaction failed: Sensitivity or propagation issue");
        }
        
        System.out.println("testInteraction passed");
    }
}
