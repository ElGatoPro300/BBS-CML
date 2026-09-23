package mchorse.bbs_mod.addons;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterActionClipsEvent;
import mchorse.bbs_mod.api.events.RegisterActionConfigsEvent;
import mchorse.bbs_mod.api.events.RegisterBBSSettingsEvent;
import mchorse.bbs_mod.api.events.RegisterCameraClipsEvent;
import mchorse.bbs_mod.api.events.RegisterEntityCaptureHandlersEvent;
import mchorse.bbs_mod.api.events.RegisterFormsEvent;
import mchorse.bbs_mod.api.events.RegisterKeyframeFactoriesEvent;
import mchorse.bbs_mod.api.events.RegisterMolangFunctionsEvent;
import mchorse.bbs_mod.api.events.RegisterParticleSimulationsEvent;
import mchorse.bbs_mod.api.events.RegisterSettingsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;

/**
 * Base class for BBS addons.
 *
 * <p>Extend this class to create a BBS addon. This class provides convenient methods
 * to register content to the mod.</p>
 *
 * @deprecated Use {@link BBSAddonMod} and {@link Subscribe} methods directly as per BBS 3.0 standard.
 */
@Deprecated
public abstract class BBSAddon implements BBSAddonMod
{
    @Subscribe
    public void onRegisterForms(RegisterFormsEvent event)
    {
        this.registerForms(event);
    }

    @Subscribe
    public void onRegisterCameraClips(RegisterCameraClipsEvent event)
    {
        this.registerCameraClips(event);
    }

    @Subscribe
    public void onRegisterActionClips(RegisterActionClipsEvent event)
    {
        this.registerActionClips(event);
    }

    @Subscribe
    public void onRegisterSettings(RegisterSettingsEvent event)
    {
        this.registerSettings(event);
    }

    @Subscribe
    public void onRegisterSourcePacks(RegisterSourcePacksEvent event)
    {
        this.registerSourcePacks(event);
    }

    @Subscribe
    public void onRegisterBBSSettings(RegisterBBSSettingsEvent event)
    {
        this.registerBBSSettings(event);
    }

    protected void registerForms(RegisterFormsEvent event)
    {}

    protected void registerCameraClips(RegisterCameraClipsEvent event)
    {}

    protected void registerActionClips(RegisterActionClipsEvent event)
    {}

    @Subscribe
    public void onRegisterEntityCaptureHandlers(RegisterEntityCaptureHandlersEvent event)
    {
        this.registerEntityCaptureHandlers(event);
    }

    protected void registerEntityCaptureHandlers(RegisterEntityCaptureHandlersEvent event)
    {}

    protected void registerSettings(RegisterSettingsEvent event)
    {}

    protected void registerSourcePacks(RegisterSourcePacksEvent event)
    {}

    protected void registerBBSSettings(RegisterBBSSettingsEvent event)
    {}

    @Subscribe
    public void onRegisterKeyframeFactories(RegisterKeyframeFactoriesEvent event)
    {
        this.registerKeyframeFactories(event);
    }

    protected void registerKeyframeFactories(RegisterKeyframeFactoriesEvent event)
    {}

    @Subscribe
    public void onRegisterMolangFunctions(RegisterMolangFunctionsEvent event)
    {
        this.registerMolangFunctions(event);
    }

    protected void registerMolangFunctions(RegisterMolangFunctionsEvent event)
    {}

    @Subscribe
    public void onRegisterActionConfigs(RegisterActionConfigsEvent event)
    {
        this.registerActionConfigs(event);
    }

    protected void registerActionConfigs(RegisterActionConfigsEvent event)
    {}

    @Subscribe
    public void onRegisterParticleSimulations(RegisterParticleSimulationsEvent event)
    {
        this.registerParticleSimulations(event);
    }

    protected void registerParticleSimulations(RegisterParticleSimulationsEvent event)
    {}
}
