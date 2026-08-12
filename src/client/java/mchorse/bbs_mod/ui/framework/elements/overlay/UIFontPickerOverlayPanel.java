package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * In-game font picker that lists {@code .ttf} files inside {@code config/bbs/settings/fonts/}.
 */
public class UIFontPickerOverlayPanel extends UIOverlayPanel
{
    private final Consumer<File> callback;

    private UIStringList list;

    public UIFontPickerOverlayPanel(Consumer<File> callback)
    {
        super(UIKeys.SETTINGS_FONT_BROWSE);

        this.callback = callback;

        File fontsFolder = getFontsFolder();

        fontsFolder.mkdirs();

        this.list = new UIStringList((l) ->
        {
            if (l.isEmpty())
            {
                return;
            }

            String name = l.get(0);
            File file = new File(fontsFolder, name);

            if (file.isFile() && this.callback != null)
            {
                this.callback.accept(file);
                this.close();
            }
        });
        this.list.background().sorting();

        this.reloadList(fontsFolder);

        UIButton openFolder = new UIButton(UIKeys.TEXTURE_OPEN_FOLDER, (b) -> UIUtils.openFolder(fontsFolder));

        this.list.relative(this.content).x(5).y(5).w(1F, -10).h(1F, -35);
        openFolder.relative(this.content).x(5).y(1F, -25).w(120).h(20);

        this.content.add(this.list, openFolder);
    }

    public static File getFontsFolder()
    {
        return BBSMod.getSettingsPath("fonts");
    }

    private void reloadList(File fontsFolder)
    {
        fontsFolder.mkdirs();

        File[] files = fontsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf"));

        this.list.clear();

        if (files != null)
        {
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (File file : files)
            {
                this.list.add(file.getName());
            }
        }

        this.list.update();
    }
}
