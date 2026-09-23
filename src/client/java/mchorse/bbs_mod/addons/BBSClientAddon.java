package mchorse.bbs_mod.addons;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterClipInteractionEvent;
import mchorse.bbs_mod.api.client.events.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterDockLayoutEvent;
import mchorse.bbs_mod.api.client.events.RegisterFilmControllerInteractionEvent;
import mchorse.bbs_mod.api.client.events.RegisterFilmEditorFactoriesEvent;
import mchorse.bbs_mod.api.client.events.RegisterFilmPreviewEvent;
import mchorse.bbs_mod.api.client.events.RegisterFilmSyncEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormBlendEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormCategoriesEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorSectionEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderPhaseEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterGizmoEvent;
import mchorse.bbs_mod.api.client.events.RegisterIconsEvent;
import mchorse.bbs_mod.api.client.events.RegisterImportersEvent;
import mchorse.bbs_mod.api.client.events.RegisterInterpolationsEvent;
import mchorse.bbs_mod.api.client.events.RegisterKeyframeShapesEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.api.client.events.RegisterModelLoadersEvent;
import mchorse.bbs_mod.api.client.events.RegisterParticleComponentsEvent;
import mchorse.bbs_mod.api.client.events.RegisterParticleSchemeUIEvent;
import mchorse.bbs_mod.api.client.events.RegisterPropTransformEvent;
import mchorse.bbs_mod.api.events.RegisterRayTracingEvent;
import mchorse.bbs_mod.api.client.events.RegisterReplayListContextMenuEvent;
import mchorse.bbs_mod.api.client.events.RegisterReplayPanelEvent;
import mchorse.bbs_mod.api.client.events.RegisterSettingsUISectionEvent;
import mchorse.bbs_mod.api.client.events.RegisterStencilMapEvent;
import mchorse.bbs_mod.api.client.events.RegisterUIKeyframeFactoriesEvent;
import mchorse.bbs_mod.api.client.events.RegisterUIThemeEvent;
import mchorse.bbs_mod.api.client.events.RegisterUIValueFactoriesEvent;

/**
 * Base class for BBS client addons.
 *
 * <p>Use this class for client-side only addons.
 * In fabric.mod.json, register this using "bbs-client-addon" (or "bbs-addon-client") entrypoint.</p>
 *
 * @deprecated Use {@link BBSAddonMod} and {@link Subscribe} methods directly as per BBS 3.0 standard.
 */
@Deprecated
public abstract class BBSClientAddon implements BBSAddonMod
{
    @Subscribe
    public void onRegisterClientSettings(RegisterClientSettingsEvent event)
    {
        this.registerClientSettings(event);
    }

    @Subscribe
    public void onRegisterDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        this.registerDashboardPanels(event);
    }

    @Subscribe
    public void onRegisterFormCategories(RegisterFormCategoriesEvent event)
    {
        this.registerFormCategories(event);
    }

    @Subscribe
    public void onRegisterL10n(RegisterL10nEvent event)
    {
        this.registerL10n(event);
    }

    @Subscribe
    public void onRegisterImporters(RegisterImportersEvent event)
    {
        this.registerImporters(event);
    }

    @Subscribe
    public void onRegisterParticleComponents(RegisterParticleComponentsEvent event)
    {
        this.registerParticleComponents(event);
    }

    @Subscribe
    public void onRegisterModelLoaders(RegisterModelLoadersEvent event)
    {
        this.registerModelLoaders(event);
    }

    protected void registerClientSettings(RegisterClientSettingsEvent event)
    {}

    protected void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {}

    protected void registerFormCategories(RegisterFormCategoriesEvent event)
    {}

    protected void registerL10n(RegisterL10nEvent event)
    {}

    protected void registerImporters(RegisterImportersEvent event)
    {}

    protected void registerParticleComponents(RegisterParticleComponentsEvent event)
    {}

    protected void registerModelLoaders(RegisterModelLoadersEvent event)
    {}

    @Subscribe
    public void onRegisterInterpolations(RegisterInterpolationsEvent event)
    {
        this.registerInterpolations(event);
    }

    @Subscribe
    public void onRegisterFilmEditorFactories(RegisterFilmEditorFactoriesEvent event)
    {
        this.registerFilmEditorFactories(event);
    }

    protected void registerFilmEditorFactories(RegisterFilmEditorFactoriesEvent event)
    {}

    @Subscribe
    public void onRegisterFormsRenderers(RegisterFormsRenderersEvent event)
    {
        this.registerFormsRenderers(event);
    }

    @Subscribe
    public void onRegisterGizmos(RegisterGizmoEvent event)
    {
        this.registerGizmos(event);
    }

    protected void registerGizmos(RegisterGizmoEvent event)
    {}

    @Subscribe
    public void onRegisterIcons(RegisterIconsEvent event)
    {
        this.registerIcons(event);
    }

    @Subscribe
    public void onRegisterUIKeyframeFactories(RegisterUIKeyframeFactoriesEvent event)
    {
        this.registerUIKeyframeFactories(event);
    }

    protected void registerInterpolations(RegisterInterpolationsEvent event)
    {}

    protected void registerFormsRenderers(RegisterFormsRenderersEvent event)
    {}

    protected void registerIcons(RegisterIconsEvent event)
    {}

    protected void registerUIKeyframeFactories(RegisterUIKeyframeFactoriesEvent event)
    {}

    @Subscribe
    public void onRegisterFormEditors(RegisterFormEditorsEvent event)
    {
        this.registerFormEditors(event);
    }

    protected void registerFormEditors(RegisterFormEditorsEvent event)
    {}

    @Subscribe
    public void onRegisterKeyframeShapes(RegisterKeyframeShapesEvent event)
    {
        this.registerKeyframeShapes(event);
    }

    protected void registerKeyframeShapes(RegisterKeyframeShapesEvent event)
    {}

    @Subscribe
    public void onRegisterUIValueFactories(RegisterUIValueFactoriesEvent event)
    {
        this.registerUIValueFactories(event);
    }

    protected void registerUIValueFactories(RegisterUIValueFactoriesEvent event)
    {}

    @Subscribe
    public void onRegisterPropTransforms(RegisterPropTransformEvent event)
    {
        this.registerPropTransforms(event);
    }

    protected void registerPropTransforms(RegisterPropTransformEvent event)
    {}

    @Subscribe
    public void onRegisterStencilMap(RegisterStencilMapEvent event)
    {
        this.registerStencilMap(event);
    }

    protected void registerStencilMap(RegisterStencilMapEvent event)
    {}

    @Subscribe
    public void onRegisterRayTracing(RegisterRayTracingEvent event)
    {
        this.registerRayTracing(event);
    }

    protected void registerRayTracing(RegisterRayTracingEvent event)
    {}

    @Subscribe
    public void onRegisterFilmPreview(RegisterFilmPreviewEvent event)
    {
        this.registerFilmPreview(event);
    }

    protected void registerFilmPreview(RegisterFilmPreviewEvent event)
    {}

    @Subscribe
    public void onRegisterReplayListContextMenu(RegisterReplayListContextMenuEvent event)
    {
        this.registerReplayListContextMenu(event);
    }

    protected void registerReplayListContextMenu(RegisterReplayListContextMenuEvent event)
    {}

    @Subscribe
    public void onRegisterReplayPanel(RegisterReplayPanelEvent event)
    {
        this.registerReplayPanel(event);
    }

    protected void registerReplayPanel(RegisterReplayPanelEvent event)
    {}

    @Subscribe
    public void onRegisterUITheme(RegisterUIThemeEvent event)
    {
        this.registerUITheme(event);
    }

    protected void registerUITheme(RegisterUIThemeEvent event)
    {}

    @Subscribe
    public void onRegisterFormEditorSection(RegisterFormEditorSectionEvent event)
    {
        this.registerFormEditorSection(event);
    }

    protected void registerFormEditorSection(RegisterFormEditorSectionEvent event)
    {}

    @Subscribe
    public void onRegisterFormRenderPhase(RegisterFormRenderPhaseEvent event)
    {
        this.registerFormRenderPhase(event);
    }

    protected void registerFormRenderPhase(RegisterFormRenderPhaseEvent event)
    {}

    @Subscribe
    public void onRegisterFormBlend(RegisterFormBlendEvent event)
    {
        this.registerFormBlend(event);
    }

    protected void registerFormBlend(RegisterFormBlendEvent event)
    {}

    @Subscribe
    public void onRegisterClipInteraction(RegisterClipInteractionEvent event)
    {
        this.registerClipInteraction(event);
    }

    protected void registerClipInteraction(RegisterClipInteractionEvent event)
    {}

    @Subscribe
    public void onRegisterDockLayout(RegisterDockLayoutEvent event)
    {
        this.registerDockLayout(event);
    }

    protected void registerDockLayout(RegisterDockLayoutEvent event)
    {}

    @Subscribe
    public void onRegisterParticleSchemeUI(RegisterParticleSchemeUIEvent event)
    {
        this.registerParticleSchemeUI(event);
    }

    protected void registerParticleSchemeUI(RegisterParticleSchemeUIEvent event)
    {}

    @Subscribe
    public void onRegisterFilmControllerInteraction(RegisterFilmControllerInteractionEvent event)
    {
        this.registerFilmControllerInteraction(event);
    }

    protected void registerFilmControllerInteraction(RegisterFilmControllerInteractionEvent event)
    {}

    @Subscribe
    public void onRegisterSettingsUISection(RegisterSettingsUISectionEvent event)
    {
        this.registerSettingsUISection(event);
    }

    protected void registerSettingsUISection(RegisterSettingsUISectionEvent event)
    {}

    @Subscribe
    public void onRegisterFilmSync(RegisterFilmSyncEvent event)
    {
        this.registerFilmSync(event);
    }

    protected void registerFilmSync(RegisterFilmSyncEvent event)
    {}
}
