package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.resources.Link;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FramebufferManager
{
    public final Map<Link, Framebuffer> framebuffers = new HashMap<>();

    public Framebuffer getFramebuffer(Link key, Consumer<Framebuffer> setup)
    {
        Framebuffer framebuffer = this.framebuffers.get(key);

        if (framebuffer == null)
        {
            framebuffer = new Framebuffer();

            setup.accept(framebuffer);

            this.framebuffers.put(key, framebuffer);
        }

        return framebuffer;
    }

    public void delete()
    {
        for (Framebuffer framebuffer : this.framebuffers.values())
        {
            framebuffer.delete();
        }

        this.framebuffers.clear();
    }
}