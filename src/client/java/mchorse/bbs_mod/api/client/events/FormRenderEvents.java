package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Around the drawing of one form — every form BBS draws, anywhere it draws one.
 */
public class FormRenderEvents
{
    public static final Event<Render> BEFORE = EventFactory.createArrayBacked(Render.class, (listeners) -> (form, context) ->
    {
        for (Render listener : listeners)
        {
            listener.onFormRender(form, context);
        }
    });

    public static final Event<Render> AFTER = EventFactory.createArrayBacked(Render.class, (listeners) -> (form, context) ->
    {
        for (Render listener : listeners)
        {
            listener.onFormRender(form, context);
        }
    });

    public static interface Render
    {
        public void onFormRender(Form form, FormRenderingContext context);
    }
}
