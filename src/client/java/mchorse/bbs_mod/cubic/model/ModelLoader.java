package mchorse.bbs_mod.cubic.model;

import java.util.LinkedList;
import java.util.Queue;

public class ModelLoader implements Runnable
{
    private ModelManager manager;
    private Thread thread;
    private final Queue<String> queue = new LinkedList<>();
    private volatile String current;

    public ModelLoader(ModelManager manager)
    {
        this.manager = manager;
    }

    public synchronized void add(String key)
    {
        this.queue.offer(key);

        if (this.thread == null)
        {
            this.thread = new Thread(this, "BBS model loader");
            this.thread.start();
        }
    }

    public synchronized boolean isLoading(String key)
    {
        return this.queue.contains(key) || (this.current != null && this.current.equals(key));
    }

    @Override
    public void run()
    {
        while (true)
        {
            String model;

            synchronized (this)
            {
                if (this.queue.isEmpty())
                {
                    this.thread = null;
                    break;
                }

                model = this.queue.poll();
                this.current = model;
            }

            try
            {
                this.manager.loadModel(model);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                synchronized (this)
                {
                    this.current = null;
                }
            }
        }
    }
}