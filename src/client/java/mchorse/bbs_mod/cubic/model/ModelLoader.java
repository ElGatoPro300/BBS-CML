package mchorse.bbs_mod.cubic.model;

import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Background model loader. Uses several worker threads so opening a morph
 * category does not sit on one BBS spinner for every model in sequence.
 */
public class ModelLoader implements Runnable
{
    private static final int WORKER_COUNT = Math.max(4, Math.min(8, Runtime.getRuntime().availableProcessors()));

    private final ModelManager manager;
    private final LinkedList<String> queue = new LinkedList<>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();
    private int activeWorkers;

    public ModelLoader(ModelManager manager)
    {
        this.manager = manager;
    }

    public void add(String key)
    {
        this.add(key, false);
    }

    /**
     * @param priority when true, jump to the front of the queue (visible UI cells).
     */
    public synchronized void add(String key, boolean priority)
    {
        if (key == null || key.isEmpty())
        {
            return;
        }

        if (this.loading.contains(key))
        {
            if (priority && this.queue.remove(key))
            {
                this.queue.addFirst(key);
            }

            return;
        }

        if (priority)
        {
            this.queue.addFirst(key);
        }
        else
        {
            this.queue.offer(key);
        }

        this.loading.add(key);
        this.ensureWorkersLocked();
    }

    public boolean isLoading(String key)
    {
        return key != null && this.loading.contains(key);
    }

    public synchronized int getQueuedCount()
    {
        return this.queue.size() + this.activeWorkers;
    }

    private void ensureWorkersLocked()
    {
        while (this.activeWorkers < WORKER_COUNT && !this.queue.isEmpty())
        {
            this.activeWorkers += 1;
            Thread thread = new Thread(this, "BBS model loader-" + this.activeWorkers);

            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            String model;

            synchronized (this)
            {
                model = this.queue.poll();

                if (model == null)
                {
                    this.activeWorkers -= 1;

                    return;
                }
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
                this.loading.remove(model);
            }
        }
    }
}
