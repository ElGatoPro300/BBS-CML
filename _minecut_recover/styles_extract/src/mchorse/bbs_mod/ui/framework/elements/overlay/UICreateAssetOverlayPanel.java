package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;

import java.util.function.Consumer;

public class UICreateAssetOverlayPanel extends UIOverlayPanel
{
    public final UILabel typeLabel;
    public final UILabel typeValue;
    public final UILabel nameLabel;
    public final UITextbox nameField;
    public final UILabel folderLabel;
    public final UIButton folderButton;
    public final UIButton confirm;
    public final UIButton cancel;

    private final ContentType type;
    private final Consumer<String> callback;
    private String selectedFolder = "";

    public static IKey getTypeName(ContentType type)
    {
        if (type == ContentType.FILMS)
        {
            return UIKeys.CREATE_ASSET_TYPE_FILM;
        }
        if (type == ContentType.MODELS)
        {
            return UIKeys.CREATE_ASSET_TYPE_MODEL;
        }
        if (type == ContentType.PARTICLES)
        {
            return UIKeys.CREATE_ASSET_TYPE_PARTICLE_SCHEME;
        }

        String id = type.getId();

        if (id.isEmpty())
        {
            return IKey.constant("");
        }

        return IKey.constant(Character.toUpperCase(id.charAt(0)) + id.substring(1));
    }

    public UICreateAssetOverlayPanel(ContentType type, Consumer<String> callback)
    {
        this(type, "", callback);
    }

    public UICreateAssetOverlayPanel(ContentType type, String defaultFolder, Consumer<String> callback)
    {
        super(UIKeys.CREATE_ASSET_TITLE);

        this.type = type;
        this.callback = callback;
        this.selectedFolder = defaultFolder != null ? defaultFolder : "";

        /* Consistent spacing: 12px outer margin, 26px row pitch, labels in a fixed 84px column. */
        int margin = 12;
        int labelW = 84;
        int fieldX = margin + labelW;
        int rowH = 20;
        int pitch = 26;

        /* Row 1: Type */
        this.typeLabel = new UILabel(UIKeys.CREATE_ASSET_TYPE);
        this.typeLabel.relative(this.content).x(margin).y(margin).w(labelW).h(rowH);

        this.typeValue = new UILabel(getTypeName(type));
        this.typeValue.relative(this.content).x(fieldX).y(margin).w(1F, -fieldX - margin).h(rowH);

        /* Row 2: File Name */
        this.nameLabel = new UILabel(UIKeys.CREATE_ASSET_FILE_NAME);
        this.nameLabel.relative(this.content).x(margin).y(margin + pitch).w(labelW).h(rowH);

        this.nameField = new UITextbox(120, (str) -> {});
        this.nameField.filename();
        this.nameField.relative(this.content).x(fieldX).y(margin + pitch).w(1F, -fieldX - margin).h(rowH);

        /* Row 3: Save Folder */
        this.folderLabel = new UILabel(UIKeys.CREATE_ASSET_SAVE_FOLDER);
        this.folderLabel.relative(this.content).x(margin).y(margin + pitch * 2).w(labelW).h(rowH);

        IKey btnLabel = this.selectedFolder.isEmpty() ? UIKeys.CREATE_ASSET_ROOT : IKey.constant(this.selectedFolder);
        UIButton folderButton = new UIButton(btnLabel, (b) ->
        {
            UIRepositoryFolderPickerOverlayPanel picker = new UIRepositoryFolderPickerOverlayPanel(
                type,
                this.selectedFolder,
                (folder) ->
                {
                    this.selectedFolder = folder;
                    b.label = folder.isEmpty() ? UIKeys.CREATE_ASSET_ROOT : IKey.constant(folder);
                }
            );
            UIOverlay.addOverlay(this.getContext(), picker, 520, 320);
        });
        this.folderButton = folderButton;
        this.folderButton.relative(this.content).x(fieldX).y(margin + pitch * 2).w(1F, -fieldX - margin).h(rowH);

        /* Row 4: Buttons (bottom-right, same 12px margin and 6px gap). */
        this.confirm = new UIButton(UIKeys.GENERAL_CONFIRM, (b) -> this.submit());
        this.confirm.relative(this.content).x(1F, -margin - 80 - 6 - 80).y(1F, -margin - rowH).w(80).h(rowH);

        this.cancel = new UIButton(UIKeys.ADDON_CANCEL, (b) -> this.close());
        this.cancel.relative(this.content).x(1F, -margin - 80).y(1F, -margin - rowH).w(80).h(rowH);

        this.content.add(this.typeLabel, this.typeValue, this.nameLabel, this.nameField, this.folderLabel, this.folderButton, this.confirm, this.cancel);
    }

    private void submit()
    {
        String name = this.nameField.getText().trim();
        if (name.isEmpty())
        {
            return;
        }

        this.close();

        if (this.callback != null)
        {
            this.callback.accept(this.selectedFolder + name);
        }
    }

    @Override
    protected void onAdd(UIElement parent)
    {
        super.onAdd(parent);
        this.nameField.textbox.moveCursorToEnd();
        parent.getContext().focus(this.nameField);
    }
}
