package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.particles.components.ParticleComponentBase;

import java.util.Map;

public class RegisterParticleComponentsEvent
{
    public final Map<String, Class<? extends ParticleComponentBase>> components;

    public RegisterParticleComponentsEvent(Map<String, Class<? extends ParticleComponentBase>> components)
    {
        this.components = components;
    }
}
