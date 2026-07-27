package mchorse.bbs_mod.simulation;

public class FluidBenchmarkTest
{
    public static void main(String[] args)
    {
        benchmarkUpdate();
    }

    private static void benchmarkUpdate()
    {
        FluidSimulation fluid = new FluidSimulation(64, 64);
        int iterations = 10000;
        
        long start = System.nanoTime();
        
        for (int i = 0; i < iterations; i++)
        {
            fluid.update();
        }
        
        long end = System.nanoTime();
        double durationMs = (end - start) / 1000000.0;
        
        System.out.println("Benchmark: " + iterations + " updates on 64x64 grid took " + durationMs + " ms");
        System.out.println("Average per update: " + (durationMs / iterations) + " ms");
        
        if (durationMs / iterations > 1.0) 
        {
            System.out.println("WARNING: Performance is slow (>1ms per update)");
        }
        else
        {
            System.out.println("Performance is good (<1ms per update)");
        }
    }
}
