package mchorse.bbs_mod.ui.framework;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.discord.DiscordPresenceManager;
import mchorse.bbs_mod.importers.IImportPathProvider;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.Importers;
import mchorse.bbs_mod.importers.types.IImporter;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.IFileDropListener;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.FFMpegUtils;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UIScreen extends class_437 implements IFileDropListener
{
    private UIBaseMenu menu;
    private UIRenderingContext context;

    private int lastGuiScale;

    public static void open(UIBaseMenu menu)
    {
        class_310.method_1551().method_1507(new UIScreen(class_2561.method_43473(), menu));
    }

    public static UIBaseMenu getCurrentMenu()
    {
        class_437 currentScreen = class_310.method_1551().field_1755;

        if (currentScreen instanceof UIScreen uiScreen)
        {
            return uiScreen.menu;
        }

        return null;
    }

    public UIScreen(class_2561 title, UIBaseMenu menu)
    {
        super(title);

        class_310 mc = class_310.method_1551();

        this.field_22787 = mc;

        this.menu = menu;
        this.context = new UIRenderingContext(new class_332(mc, mc.method_22940().method_23000()));

        this.menu.context.setup(this.context);
    }

    public UIBaseMenu getMenu()
    {
        return this.menu;
    }

    public void update()
    {
        this.menu.update();
    }

    public void renderInWorld(WorldRenderContext context)
    {
        this.menu.renderInWorld(context);
    }

    @Override
    public void method_29638(List<Path> paths)
    {
        super.method_29638(paths);

        String[] filePaths = new String[paths.size()];
        int i = 0;

        for (Path path : paths)
        {
            filePaths[i] = path.toAbsolutePath().toString();

            i += 1;
        }

        this.acceptFilePaths(filePaths);
    }

    @Override
    public void method_25432()
    {
        if (!this.menu.preserveMinecraftGuiScale())
        {
            class_310.method_1551().field_1690.method_42474().method_41748(this.lastGuiScale);
            class_310.method_1551().method_15993();
        }

        super.method_25432();

        this.menu.onClose(null);
        DiscordPresenceManager.INSTANCE.onBbsUiClosed();

        class_310.method_1551().field_1690.field_1842 = false;
    }

    @Override
    public void method_49589()
    {
        this.lastGuiScale = class_310.method_1551().field_1690.method_42474().method_41753();

        if (!this.menu.preserveMinecraftGuiScale())
        {
            int scale = this.menu.forcedGuiScale();

            if (scale <= 0)
            {
                scale = BBSModClient.getGUIScale();
            }

            class_310.method_1551().field_1690.method_42474().method_41748(scale);
            class_310.method_1551().method_15993();
        }

        super.method_49589();

        this.menu.onOpen(null);
        DiscordPresenceManager.INSTANCE.onBbsUiOpened(this.menu);

        class_310.method_1551().field_1690.field_1842 = this.menu.canHideHUD();
    }

    @Override
    public boolean method_25421()
    {
        return this.menu.canPause();
    }

    @Override
    protected void method_25426()
    {
        super.method_25426();

        this.menu.resize(this.field_22789, this.field_22790);
    }

    @Override
    public void method_25410(class_310 client, int width, int height)
    {
        super.method_25410(client, width, height);

        this.menu.resize(width, height);
    }

    @Override
    public boolean method_25402(double mouseX, double mouseY, int button)
    {
        return this.menu.mouseClicked((int) mouseX, (int) mouseY, button);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        return this.menu.mouseScrolled((int) mouseX, (int) mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean method_25406(double mouseX, double mouseY, int button)
    {
        return this.menu.mouseReleased((int) mouseX, (int) mouseY, button);
    }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers)
    {
        return this.menu.handleKey(keyCode, scanCode, BBSRendering.lastAction, modifiers);
    }

    @Override
    public boolean method_16803(int keyCode, int scanCode, int modifiers)
    {
        return this.menu.handleKey(keyCode, scanCode, GLFW.GLFW_RELEASE, modifiers);
    }

    @Override
    public boolean method_25400(char chr, int modifiers)
    {
        this.menu.handleTextInput(chr);

        return true;
    }

    @Override
    public void method_25420(class_332 context, int mouseX, int mouseY, float delta)
    {}

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta)
    {
        super.method_25394(context, mouseX, mouseY, delta);

        this.menu.context.setTransition(this.field_22787.method_60646().method_60637(false));
        this.menu.renderMenu(this.context, mouseX, mouseY);
        this.menu.context.render.executeRunnables();

        /* Overlay close can call setScreen(null) mid-render; do not re-hide HUD after that. */
        if (this.field_22787.field_1755 == this)
        {
            this.field_22787.field_1690.field_1842 = this.menu.canHideHUD();
        }
    }

    @Override
    public void acceptFilePaths(String[] paths)
    {
        if (this.menu != null)
        {
            if (!FFMpegUtils.checkFFMPEG())
            {
                this.menu.context.notifyError(UIKeys.IMPORTER_FFMPEG_NOTIFICATION);

                return;
            }

            File directory = null;
            boolean open = true;

            for (IImportPathProvider provider : this.menu.getRoot().getChildren(IImportPathProvider.class))
            {
                directory = provider.getImporterPath();

                if (directory != null)
                {
                    open = false;

                    break;
                }
            }

            List<File> files = new ArrayList<>();

            for (String path : paths)
            {
                File file = new File(path);

                if (file.exists())
                {
                    files.add(file);
                }
            }

            ImporterContext context = new ImporterContext(files, directory);

            for (IImporter importer : Importers.getImporters())
            {
                if (importer.canImport(context))
                {
                    importer.importFiles(context);

                    if (open)
                    {
                        UIUtils.openFolder(context.getDestination(importer));
                    }

                    this.menu.context.notifySuccess(UIKeys.IMPORTER_SUCCESS_NOTIFICATION.format(importer.getName()));

                    return;
                }
            }
        }
    }
}