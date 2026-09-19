package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;

import net.minecraft.client.MinecraftClient;

public class UIFilmPlayerSettingsOverlayPanel extends UIMessageBarOverlayPanel
{
    private final Film film;

    public final UITrackpad hp;
    public final UITrackpad hunger;
    public final UITrackpad xpLevel;
    public final UITrackpad xpProgress;

    public final UIButton replaceInventory;
    public final UIButton applyToPlayer;
    public final UIScrollView editor;

    public UIFilmPlayerSettingsOverlayPanel(Film film)
    {
        super(UIKeys.FILM_PLAYER_SETTINGS_TITLE, UIKeys.FILM_PLAYER_SETTINGS_DESCRIPTION);

        this.film = film;

        this.message.textAnchorX(0F);
        this.message.relative(this.content).xy(6, 6).w(1F, -12).anchorX(0F);

        this.hp = new UITrackpad((v) -> BaseValue.edit(this.film.hp, (value) -> value.set(v.floatValue())));
        this.hp.limit(1, 20, true).setValue(this.film.hp.get());

        this.hunger = new UITrackpad((v) -> BaseValue.edit(this.film.hunger, (value) -> value.set(v.floatValue())));
        this.hunger.limit(1, 20, true).setValue(this.film.hunger.get());

        this.xpLevel = new UITrackpad((v) -> BaseValue.edit(this.film.xpLevel, (value) -> value.set(v.intValue())));
        this.xpLevel.limit(0).integer().setValue(this.film.xpLevel.get());

        this.xpProgress = new UITrackpad((v) -> BaseValue.edit(this.film.xpProgress, (value) -> value.set(v.floatValue())));
        this.xpProgress.limit(0, 1).increment(0.01D).setValue(this.film.xpProgress.get());

        this.replaceInventory = new UIButton(UIKeys.FILM_REPLACE_INVENTORY, (b) ->
            BaseValue.edit(this.film.inventory, (inv) -> inv.fromPlayer(MinecraftClient.getInstance().player)));
        this.replaceInventory.setEnabled(MinecraftClient.getInstance().player != null);
        this.replaceInventory.w(1F);

        this.applyToPlayer = new UIButton(UIKeys.FILM_APPLY_PLAYER_SETTINGS_TO_PLAYER, (b) -> ClientNetwork.sendApplyFilmPlayerSettingsToPlayer(this.film));
        this.applyToPlayer.setEnabled(MinecraftClient.getInstance().player != null);
        this.applyToPlayer.w(1F);

        this.editor = UI.scrollView(5, 6,
            UI.label(UIKeys.FILM_PLAYER_SETTINGS_HP),
            this.hp,
            UI.label(UIKeys.FILM_PLAYER_SETTINGS_HUNGER).marginTop(UIConstants.SECTION_GAP),
            this.hunger,
            UI.label(UIKeys.FILM_PLAYER_SETTINGS_XP_LEVEL).marginTop(UIConstants.SECTION_GAP),
            this.xpLevel,
            UI.label(UIKeys.FILM_PLAYER_SETTINGS_XP_PROGRESS).marginTop(UIConstants.SECTION_GAP),
            this.xpProgress,
            this.replaceInventory.marginTop(10),
            this.applyToPlayer.marginTop(4)
        );
        this.editor.relative(this.message).x(0).w(1F).y(1F, 6).hTo(this.bar.area, -6);

        this.content.add(this.editor);
    }
}
