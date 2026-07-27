package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.BBSClient;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.class_1959;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import java.util.ArrayList;
import java.util.List;

public class UIStringKeyframeFactory extends UIKeyframeFactory<String>
{
    private UITextbox string;

    public UIStringKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.string = new UITextbox(1000, this::setValue);
        this.string.setText(keyframe.getValue());

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        boolean isStructureFile = sheet != null && ("structure_file".equals(sheet.id) || sheet.id.endsWith("/structure_file"));
        boolean isBiomeId = sheet != null && ("biome_id".equals(sheet.id) || sheet.id.endsWith("/biome_id"));
        boolean isModel = sheet != null && ("model".equals(sheet.id) || sheet.id.endsWith("/model"));
        boolean isMob = sheet != null && ("mobId".equals(sheet.id) || sheet.id.endsWith("/mobId"));
        boolean isParticle = sheet != null && ("effect".equals(sheet.id) || sheet.id.endsWith("/effect"));

        if (isStructureFile)
        {
            UIButton pickStructure = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (b) ->
            {
                UIStructureOverlayPanel panel = new UIStructureOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (link) ->
                {
                    String value = link == null ? "" : link.toString();
                    this.editor.getGraph().setValue(value, true);
                });

                String current = this.keyframe.getValue();
                if (current != null && !current.isEmpty())
                {
                    try
                    {
                        panel.set(Link.create(current));
                    }
                    catch (Exception e)
                    {
                        panel.set(Link.assets(current));
                    }
                }

                UIOverlay.addOverlay(this.getContext(), panel, 280, 0.5F);
            });

            this.scroll.add(pickStructure);
        }
        else if (isBiomeId)
        {
            UIButton pickBiome = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (b) ->
            {
                UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (value) ->
                {
                    String id = value == null ? "" : value;
                    this.editor.getGraph().setValue(id, true);
                });

                List<String> ids = new ArrayList<>();
                try
                {
                    if (class_310.method_1551().field_1687 != null)
                    {
                        class_2378<class_1959> reg = class_310.method_1551().field_1687.method_30349().method_30530(class_7924.field_41236);
                        for (class_2960 id : reg.method_10235())
                        {
                            ids.add(id.toString());
                        }
                    }
                }
                catch (Throwable ignored) {}

                overlay.addValues(ids);
                overlay.setValue(this.keyframe.getValue());
                UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
            });

            this.scroll.add(pickBiome);
        }
        else if (isModel)
        {
            UIButton pickModel = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_MODEL, (b) ->
            {
                UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_EDITOR_MODEL_MODELS, (value) ->
                {
                    String id = value == null ? "" : value;
                    this.editor.getGraph().setValue(id, true);
                });

                overlay.addValues(BBSClient.getModels().getAvailableKeys());
                overlay.list.list.sort();
                overlay.setValue(this.keyframe.getValue());
                UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
            });

            this.scroll.add(pickModel);
        }
        else if (isMob)
        {
            UIButton pickMob = new UIButton(UIKeys.FORMS_EDITORS_MOB_TITLE, (b) ->
            {
                UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_MOB_TITLE, (value) ->
                {
                    String id = value == null ? "" : value;
                    this.editor.getGraph().setValue(id, true);
                });

                List<String> ids = new ArrayList<>();
                for (class_2960 id : class_7923.field_41177.method_10235())
                {
                    ids.add(id.toString());
                }

                overlay.addValues(ids);
                overlay.list.list.sort();
                overlay.setValue(this.keyframe.getValue());
                UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
            });

            this.scroll.add(pickMob);
        }
        else if (isParticle)
        {
            UIButton pickParticle = new UIButton(UIKeys.FORMS_CATEGORIES_PARTICLES, (b) ->
            {
                UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_CATEGORIES_PARTICLES, (value) ->
                {
                    String id = value == null ? "" : value;
                    this.editor.getGraph().setValue(id, true);
                });

                overlay.addValues(BBSClient.getParticles().getKeys());
                overlay.list.list.sort();
                overlay.setValue(this.keyframe.getValue());
                UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
            });

            this.scroll.add(pickParticle);
        }

        if (!isStructureFile && !isBiomeId && !isModel && !isMob && !isParticle)
        {
            this.scroll.add(this.string);
        }
    }

    @Override
    public void update()
    {
        super.update();

        this.string.setText(this.keyframe.getValue());
    }
}
