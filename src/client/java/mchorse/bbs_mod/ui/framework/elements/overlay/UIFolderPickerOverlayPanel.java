package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIFileLinkList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.io.File;
import java.util.function.Consumer;

/**
 * Folder picker overlay panel
 * 
 * This overlay allows the user to browse and select a folder using
 * the same interface as the texture picker.
 */
public class UIFolderPickerOverlayPanel extends UIOverlayPanel
{
    public UIFileLinkList picker;
    public UIButton confirmBtn;
    public UIButton openFolder;
    
    private Consumer<File> callback;
    
    public UIFolderPickerOverlayPanel(IKey title, IKey description, Consumer<File> callback)
    {
        super(title);
        
        this.callback = callback;
        
        // Description label
        UIElement descElement = null;
        if (description != null)
        {
            descElement = UI.label(description).background();
            this.content.add(descElement);
        }
        
        this.picker = new UIFileLinkList((link) -> {})
        {
            @Override
            public void setPath(Link folder, boolean fastForward)
            {
                super.setPath(folder, fastForward);
                UIFolderPickerOverlayPanel.this.updateOpenFolderButton();
            }
        };

        this.picker.filter((l) ->
        {
            String path = l.path.toLowerCase();

            return path.endsWith("/") || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg");
        });
        this.picker.background();
        
        // Buttons
        this.confirmBtn = new UIButton(UIKeys.GENERAL_CONFIRM, (b) -> this.confirmSelection());
        this.openFolder = new UIButton(UIKeys.TEXTURE_OPEN_FOLDER, (b) -> this.openCurrentFolder());
        
        // Layout
        if (descElement != null)
        {
            descElement.relative(this.content).x(5).y(10).w(1F, -10).h(40).anchor(0F, 0F);
            this.picker.relative(this.content).y(60).w(1F, -10).h(1F, -90).anchor(0F, 0F);
        }
        else
        {
            this.picker.relative(this.content).y(10).w(1F, -10).h(1F, -50).anchor(0F, 0F);
        }
        
        this.confirmBtn.relative(this.content).x(1F, -105).y(1F, -25).wh(100, 20);
        this.openFolder.relative(this.content).x(5).y(1F, -25).wh(100, 20);
        
        this.content.add(this.picker, this.confirmBtn, this.openFolder);
        
        /* Start at assets folder */
        this.picker.setPath(Link.assets(""));
        this.picker.clearNavigationHistory();
        this.updateOpenFolderButton();
    }

    private void confirmSelection()
    {
        File folder = BBSMod.getProvider().getFile(this.picker.path);
        
        if (folder != null && folder.isDirectory())
        {
            if (this.callback != null)
            {
                this.callback.accept(folder);
            }
            
            super.close();
        }
        else
        {
            UIOverlay.addOverlay(this.getContext(),
                new UIMessageOverlayPanel(UIKeys.GENERAL_ERROR,
                    UIKeys.FOLDER_PICKER_ERROR_INVALID_FOLDER));
        }
    }
    
    private void openCurrentFolder()
    {
        File folder = BBSMod.getProvider().getFile(this.picker.path);
        
        if (folder != null && folder.isDirectory())
        {
            UIUtils.openFolder(folder);
        }
    }
    
    private void updateOpenFolderButton()
    {
        File folder = BBSMod.getProvider().getFile(this.picker.path);
        this.openFolder.setEnabled(folder != null && folder.isDirectory());
    }
}
