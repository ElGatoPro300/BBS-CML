package mchorse.bbs_mod.cubic.model;

import java.util.LinkedList;
import java.util.Queue;

public class ModelLoader implements Runnable
{
    private ModelManager manager;
    private Thread thread;
    private Queue<String> queue = new LinkedList<>();

    public ModelLoader(ModelManager manager)
    {
        this.manager = manager;
    }

    public void add(String key)
    {
        this.queue.offer(key);

        if (this.thread == null)
        {
            this.thread = new Thread(this, "BBS model loader");
            this.thread.start();
        }
    }

    @Override
    public void run()
    {
        while (!this.queue.isEmpty())
        {
            String model = this.queue.poll();

            try
            {
                this.manager.loadModel(model);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        this.thread = null;
    }
}