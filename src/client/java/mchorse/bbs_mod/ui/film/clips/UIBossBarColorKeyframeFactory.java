package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.BossBarClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIColorKeyframeFactory;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBossBarColorKeyframeFactory extends UIColorKeyframeFactory
{
    public UIBossBarColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIButton enderDragon = this.createPresetButton(BossBarClip.PRESET_ENDER_DRAGON, UIKeys.CAMERA_PANELS_BOSS_BAR_PRESET_ENDER_DRAGON);
        UIButton wither = this.createPresetButton(BossBarClip.PRESET_WITHER, UIKeys.CAMERA_PANELS_BOSS_BAR_PRESET_WITHER);

        enderDragon.w(1F);
        wither.w(1F);

        UIElement presets = UI.row(enderDragon, wither);

        this.scroll.add(presets);
    }

    private UIButton createPresetButton(int color, IKey label)
    {
        UIButton button = new UIButton(label, (b) -> this.setValue(Color.rgba(color)));

        button.color(color).background(true).h(20);

        return button;
    }
}
