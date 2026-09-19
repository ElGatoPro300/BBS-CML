package mchorse.bbs_mod.ui.particles;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Status controls shown on the top-right document tab bar while editing a particle scheme.
 * Matches the style and save animation of the film editor.
 */
public class UIParticleStatusIcons extends UIElement
{
    public static final int WIDTH = 20;

    private static final int ICON_SIZE = 20;
    private static final long SAVE_FLASH_MS = 3000L;
    private static final int SAVE_FLASH_COLOR = 0xCC1A5C1A;

    private final UIParticleSchemePanel panel;
    private final UIIcon saveIcon;

    private long saveFlashStart = -1L;

    public UIParticleStatusIcons(UIParticleSchemePanel panel)
    {
        this.panel = panel;
        this.h(ICON_SIZE);

        this.saveIcon = new UIIcon(Icons.SAVED, (b) -> this.saveFromIcon());
        this.saveIcon.tooltip(UIKeys.GENERAL_SAVE);

        this.add(this.saveIcon);
    }

    public void layoutInTabBar(int x, int y, int h)
    {
        this.area.set(x, y, WIDTH, h);
        this.saveIcon.area.set(x, y, WIDTH, h);
    }

    public void flashAutosave()
    {
        this.saveFlashStart = System.currentTimeMillis();
    }

    private void saveFromIcon()
    {
        if (this.panel.getData() == null)
        {
            return;
        }

        this.panel.manualSave();
    }

    @Override
    public void render(UIContext context)
    {
        if (this.saveFlashStart >= 0L)
        {
            long elapsed = System.currentTimeMillis() - this.saveFlashStart;

            if (elapsed >= SAVE_FLASH_MS)
            {
                this.saveFlashStart = -1L;
            }
            else
            {
                float fade = 1F - elapsed / (float) SAVE_FLASH_MS;
                int green = Colors.setA(SAVE_FLASH_COLOR, fade);

                context.batcher.box(this.saveIcon.area.x, this.saveIcon.area.y, this.saveIcon.area.ex(), this.saveIcon.area.ey(), green);
            }
        }

        super.render(context);
    }
}
