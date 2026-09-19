package mchorse.bbs_mod.cubic.physics;

import java.util.Map;

/**
 * Authoring config for spring chains on a model: per-root-bone chain definitions
 * plus optional global wind.
 */
public record SpringChainsConfig(Map<String, SpringChainDef> chains, WindDef wind)
{
    public SpringChainsConfig
    {
        if (wind == null)
        {
            wind = WindDef.NONE;
        }
    }
}
