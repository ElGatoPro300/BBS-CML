package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UICropOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;

public class UIBillboardFormPanel extends UIFormPanel<BillboardForm>
{
    public UIButton pick;
    public UIToggle billboard;
    public UIToggle linear;
    public UIToggle mipmap;

    public UIButton openCrop;
    public UIToggle resizeCrop;
    public UIColor color;

    public UITrackpad offsetX;
    public UITrackpad offsetY;
    public UITrackpad rotation;

    public UIToggle shading;

    public UIBillboardFormPanel(UIForm editor)
    {
        super(editor);

        this.pick = new UIButton(UIKeys.FORMS_EDITORS_BILLBOARD_PICK_TEXTURE, (b) ->
        {
            UITexturePicker.open(this.getContext(), this.form.texture.get(), (l) -> this.form.texture.set(l));
        });
        this.billboard = new UIToggle(UIKeys.FORMS_EDITORS_BILLBOARD_TITLE, false, (b) -> this.form.billboard.set(b.getValue()));
        this.linear = new UIToggle(UIKeys.TEXTURES_LINEAR, false, (b) -> this.form.linear.set(b.getValue()));
        this.mipmap = new UIToggle(UIKeys.TEXTURES_MIPMAP, false, (b) -> this.form.mipmap.set(b.getValue()));
        this.openCrop = new UIButton(UIKeys.FORMS_EDITORS_BILLBOARD_EDIT_CROP, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), new UICropOverlayPanel(this.form.texture.get(), this.form.crop.get()), 0.5F, 0.5F);
        });
        this.resizeCrop = new UIToggle(UIKeys.FORMS_EDITORS_BILLBOARD_RESIZE_CROP, false, (b) -> this.form.resizeCrop.set(b.getValue()));
        this.color = new UIColor((value) -> this.form.color.set(Color.rgba(value))).direction(Direction.LEFT).withAlpha();

        this.offsetX = new UITrackpad((value) -> this.form.offsetX.set(value.floatValue()));
        this.offsetX.tooltip(UIKeys.FORMS_EDITORS_BILLBOARD_OFFSET_X);
        this.offsetY = new UITrackpad((value) -> this.form.offsetY.set(value.floatValue()));
        this.offsetY.tooltip(UIKeys.FORMS_EDITORS_BILLBOARD_OFFSET_Y);
        this.rotation = new UITrackpad((value) -> this.form.rotation.set(value.floatValue()));
        this.rotation.tooltip(UIKeys.FORMS_EDITORS_BILLBOARD_ROTATION);

        this.shading = new UIToggle(UIKeys.FORMS_EDITORS_BILLBOARD_SHADING, false, (b) -> this.form.shading.set(b.getValue()));

        this.options.add(this.pick, this.color, this.billboard, this.linear, this.mipmap);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BILLBOARD_CROP).marginTop(8), this.openCrop, this.resizeCrop);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BILLBOARD_UV_SHIFT).marginTop(8), UI.row(this.offsetX, this.offsetY), this.rotation, this.shading);
    }

    @Override
    public void startEdit(BillboardForm form)
    {
        super.startEdit(form);

        this.billboard.setValue(form.billboard.get());
        this.linear.setValue(form.linear.get());
        this.mipmap.setValue(form.mipmap.get());

        this.resizeCrop.setValue(form.resizeCrop.get());
        this.color.setColor(form.color.get().getARGBColor());

        this.offsetX.setValue(form.offsetX.get());
        this.offsetY.setValue(form.offsetY.get());
        this.rotation.setValue(form.rotation.get());

        this.shading.setValue(form.shading.get());
    }
}