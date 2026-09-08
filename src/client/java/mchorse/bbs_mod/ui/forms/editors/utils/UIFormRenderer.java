package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;

import mchorse.bbs_mod.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class UIFormRenderer extends UIModelRenderer
{
    public Form form;

    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.form == null)
        {
            return;
        }

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.entity, this.createCameraStack(), LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, context.getTransition())
            .camera(this.camera)
            .modelRenderer();

        FormUtilsClient.render(this.form, formContext);
    }
}