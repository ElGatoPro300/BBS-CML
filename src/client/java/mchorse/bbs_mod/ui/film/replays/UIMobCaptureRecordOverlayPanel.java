package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.film.MobCaptureAreaScanner;
import mchorse.bbs_mod.film.MobCaptureRecordingSetup;
import mchorse.bbs_mod.film.RecordingPauseHelper;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UIMobCaptureRecordOverlayPanel extends UIOverlayPanel
{
    private static final int ICON_COLUMN_WIDTH = 20;
    private static final int TOGGLE_WIDTH = 28;
    private static final int FOOTER_BUTTON_WIDTH = 100;
    private static final int FOOTER_GAP = 8;

    private static UIMobCaptureRecordOverlayPanel opened;

    private final Consumer<MobCaptureRecordingSetup> callback;
    private final MobCaptureRecordingSetup setup = new MobCaptureRecordingSetup();

    private UIToggle captureToggle;
    private UITrackpad radius;
    private UIPoseSectionCollapse mobsSection;
    private UIElement mobsColumn;
    private UIScrollView scroll;
    private UIElement typeList;

    private final Map<String, Boolean> expandedTypes = new HashMap<>();
    private Map<String, MobCaptureAreaScanner.TypeBucket> lastBuckets = new HashMap<>();

    public static void openInGame(Consumer<MobCaptureRecordingSetup> callback)
    {
        if (isOpened())
        {
            return;
        }

        UIMobCaptureRecordOverlayPanel panel = new UIMobCaptureRecordOverlayPanel(callback);

        panel.onClose((event) -> MinecraftClient.getInstance().setScreen(null));

        UIScreen.open(new UIBaseMenu()
        {
            @Override
            public boolean canHideHUD()
            {
                return false;
            }

            @Override
            public boolean canPause()
            {
                return false;
            }

            @Override
            public void onOpen(UIBaseMenu oldMenu)
            {
                super.onOpen(oldMenu);

                UIOverlay.addOverlay(this.context, panel, 480, 440).noBackground();
            }
        });
    }

    public static void openOnContext(UIContext context, Consumer<MobCaptureRecordingSetup> callback)
    {
        if (context == null || isOpened())
        {
            return;
        }

        UIOverlay.addOverlay(context, new UIMobCaptureRecordOverlayPanel(callback), 480, 440);
    }

    public static boolean isOpened()
    {
        return opened != null && opened.hasParent();
    }

    public static void closeOpened()
    {
        if (opened != null)
        {
            opened.close();
        }
    }

    public UIMobCaptureRecordOverlayPanel(Consumer<MobCaptureRecordingSetup> callback)
    {
        super(UIKeys.FILM_MOB_CAPTURE_TITLE);

        opened = this;
        this.callback = callback;
        this.resizable().minSize(380, 400);

        UIText description = new UIText(UIKeys.FILM_MOB_CAPTURE_DESCRIPTION).textAnchorX(0.5F);

        description.relative(this.content).x(0.5F).y(8).w(0.9F).h(38).anchorX(0.5F);

        this.captureToggle = new UIToggle(UIKeys.FILM_MOB_CAPTURE_ENABLE, true, (b) ->
        {
            this.setup.captureMobs = b.getValue();
            this.updateListVisibility();
        });

        this.captureToggle.relative(this.content).x(12).y(50).w(1F, -24);

        UILabel radiusLabel = UI.label(UIKeys.FILM_MOB_CAPTURE_RADIUS);

        radiusLabel.relative(this.content).x(12).y(78).w(0.5F);

        this.radius = new UITrackpad((v) ->
        {
            this.setup.areaSize = v.floatValue();
            this.refreshTypes();
        });
        this.radius.limit(16D, 256D, true).increment(4D).values(16D, 4D, 32D);
        this.radius.setValue(this.setup.areaSize);
        this.radius.relative(this.content).x(0.5F).y(78).w(0.45F).anchorX(0.5F);

        this.typeList = UI.column(4, 0);
        this.scroll = new UIScrollView();
        this.scroll.add(this.typeList);
        this.typeList.relative(this.scroll).w(1F, -12);
        this.scroll.h(180);

        this.mobsSection = new UIPoseSectionCollapse(UIKeys.FILM_MOB_CAPTURE_EMPTY, Colors.ACTIVE, this.scroll);
        this.mobsColumn = new UIElement();

        this.mobsColumn.column(4).vertical().stretch();
        this.mobsColumn.relative(this.content).x(12).y(106).w(1F, -24);
        this.mobsColumn.add(this.mobsSection);

        UIButton start = new UIButton(UIKeys.FILM_MOB_CAPTURE_START, (b) -> this.submit());
        UIButton cancel = new UIButton(UIKeys.CONFIG_CANCEL, (b) -> this.close());

        cancel.w(FOOTER_BUTTON_WIDTH).h(20);
        start.w(FOOTER_BUTTON_WIDTH).h(20);

        UIElement footer = UI.row(FOOTER_GAP, 0, 20, cancel, start);

        footer.w(FOOTER_BUTTON_WIDTH * 2 + FOOTER_GAP).h(20);
        footer.relative(this.content).x(0.5F).y(1F, -12).anchor(0.5F, 1F);

        this.content.add(description, this.captureToggle, radiusLabel, this.radius, this.mobsColumn, footer);

        this.onClose((event) ->
        {
            RecordingPauseHelper.pop();

            if (opened == this)
            {
                opened = null;
            }
        });

        RecordingPauseHelper.push();
        this.refreshTypes();
    }

    private void updateListVisibility()
    {
        boolean visible = this.setup.captureMobs;

        this.mobsColumn.setVisible(visible);
        this.radius.setVisible(visible);

        if (visible)
        {
            this.refreshTypes();
        }
    }

    private void refreshTypes()
    {
        this.lastBuckets = MobCaptureAreaScanner.scan(this.setup.areaSize);

        this.typeList.getChildren().clear();

        int total = 0;

        for (MobCaptureAreaScanner.TypeBucket bucket : this.lastBuckets.values())
        {
            total += bucket.entities.size();
        }

        if (this.lastBuckets.isEmpty())
        {
            this.mobsSection.setBaseLabel(UIKeys.FILM_MOB_CAPTURE_EMPTY);
            this.mobsSection.setExpanded(false);
            this.mobsSection.resize();

            return;
        }

        this.mobsSection.setBaseLabel(UIKeys.FILM_MOB_CAPTURE_SUMMARY.format(String.valueOf(total), String.valueOf(this.lastBuckets.size())));

        this.addColumnHeaderRow();
        this.addSelectAllRow();

        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        for (MobCaptureAreaScanner.TypeBucket bucket : this.lastBuckets.values())
        {
            String typeId = bucket.typeId;
            boolean typeExpanded = this.expandedTypes.getOrDefault(typeId, false);
            String typeLabel = bucket.label + " (" + bucket.entities.size() + ")";
            UIIcon typeIcon = new UIIcon(typeExpanded ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT, (b) ->
            {
                this.expandedTypes.put(typeId, !this.expandedTypes.getOrDefault(typeId, false));
                this.refreshTypes();
            });
            UIElement typeName = UI.label(IKey.raw(typeLabel)).w(1F);
            UIToggle typeSelectToggle = new UIToggle(IKey.EMPTY, this.isTypeFullySelected(bucket), (b) ->
            {
                this.setTypeSelected(bucket, b.getValue());
                this.syncTypeSelection(bucket);
                this.refreshTypes();
            });
            UIToggle typeVanillaToggle = new UIToggle(IKey.EMPTY, this.setup.vanillaPlaybackTypeIds.contains(typeId), (b) ->
            {
                this.setTypeVanilla(bucket, b.getValue());
                this.refreshTypes();
            });

            typeSelectToggle.w(TOGGLE_WIDTH);
            typeVanillaToggle.w(TOGGLE_WIDTH);
            typeVanillaToggle.tooltip(UIKeys.FILM_REPLAY_VANILLA_MOB_PLAYBACK_TOOLTIP);

            UIElement typeRow = UI.row(4, 0, 20, typeIcon, typeName, typeSelectToggle, typeVanillaToggle);

            typeRow.relative(this.typeList).w(1F).h(14);
            this.typeList.add(typeRow);
            this.syncTypeSelection(bucket);

            if (typeExpanded)
            {
                UIElement entityList = UI.column(2, 0);
                int index = 0;

                for (Entity entity : bucket.entities)
                {
                    int entityId = entity.getId();
                    String entityLabel = MobCaptureAreaScanner.getEntityLabel(entity, index, player);
                    UIElement entityName = UI.label(IKey.raw(entityLabel)).w(1F);
                    UIToggle entitySelectToggle = new UIToggle(IKey.EMPTY, this.setup.selectedEntityIds.contains(entityId), (b) ->
                    {
                        if (b.getValue())
                        {
                            this.setup.selectedEntityIds.add(entityId);
                        }
                        else
                        {
                            this.setup.selectedEntityIds.remove(entityId);
                        }

                        this.syncTypeSelection(bucket);
                        this.refreshTypes();
                    });
                    UIToggle entityVanillaToggle = new UIToggle(IKey.EMPTY, this.setup.vanillaPlaybackEntityIds.contains(entityId), (b) ->
                    {
                        if (b.getValue())
                        {
                            this.setup.vanillaPlaybackEntityIds.add(entityId);
                        }
                        else
                        {
                            this.setup.vanillaPlaybackEntityIds.remove(entityId);
                        }

                        this.syncTypeVanilla(bucket);
                        this.refreshTypes();
                    });

                    entitySelectToggle.w(TOGGLE_WIDTH);
                    entityVanillaToggle.w(TOGGLE_WIDTH);
                    entityVanillaToggle.tooltip(UIKeys.FILM_REPLAY_VANILLA_MOB_PLAYBACK_TOOLTIP);

                    UIElement entityIndent = new UIElement();

                    entityIndent.w(ICON_COLUMN_WIDTH).h(20);

                    UIElement entityRow = UI.row(4, 0, 20, entityIndent, entityName, entitySelectToggle, entityVanillaToggle);

                    entityRow.relative(entityList).w(1F).h(20);
                    entityList.add(entityRow);
                    index += 1;
                }

                entityList.relative(this.typeList).w(1F);
                this.typeList.add(entityList);
            }
        }

        this.typeList.resize();
        this.scroll.resize();
        this.mobsSection.resize();
    }

    private void addColumnHeaderRow()
    {
        UIElement spacer = new UIElement();

        spacer.w(ICON_COLUMN_WIDTH).h(14);

        UIElement nameSpacer = new UIElement();

        nameSpacer.w(1F).h(14);

        UILabel addHeader = UI.label(UIKeys.FILM_MOB_CAPTURE_COLUMN_ADD);

        addHeader.labelAnchor(0.5F, 0.5F).color(Colors.LIGHTER_GRAY);
        addHeader.w(TOGGLE_WIDTH).h(14);

        UILabel vanillaHeader = UI.label(UIKeys.FILM_MOB_CAPTURE_COLUMN_VA);

        vanillaHeader.labelAnchor(0.5F, 0.5F).color(Colors.LIGHTER_GRAY);
        vanillaHeader.tooltip(UIKeys.FILM_REPLAY_VANILLA_MOB_PLAYBACK_TOOLTIP);
        vanillaHeader.w(TOGGLE_WIDTH).h(14);

        UIElement headerRow = UI.row(4, 0, 20, spacer, nameSpacer, addHeader, vanillaHeader);

        headerRow.relative(this.typeList).w(1F).h(14);
        this.typeList.add(headerRow);
    }

    private void addSelectAllRow()
    {
        UIElement spacer = new UIElement();

        spacer.w(ICON_COLUMN_WIDTH).h(20);

        UILabel selectAllLabel = UI.label(UIKeys.FILM_MOB_CAPTURE_SELECT_ALL);

        selectAllLabel.w(1F).h(20);

        UIToggle selectAllAdd = new UIToggle(IKey.EMPTY, this.isAllSelected(), (b) ->
        {
            this.setAllSelected(b.getValue());
            this.refreshTypes();
        });
        UIToggle selectAllVanilla = new UIToggle(IKey.EMPTY, this.isAllVanilla(), (b) ->
        {
            this.setAllVanilla(b.getValue());
            this.refreshTypes();
        });

        selectAllAdd.w(TOGGLE_WIDTH);
        selectAllVanilla.w(TOGGLE_WIDTH);
        selectAllVanilla.tooltip(UIKeys.FILM_REPLAY_VANILLA_MOB_PLAYBACK_TOOLTIP);

        UIElement selectAllRow = UI.row(4, 0, 20, spacer, selectAllLabel, selectAllAdd, selectAllVanilla);

        selectAllRow.relative(this.typeList).w(1F).h(20);
        this.typeList.add(selectAllRow);
    }

    private boolean isAllSelected()
    {
        if (this.lastBuckets.isEmpty())
        {
            return false;
        }

        for (MobCaptureAreaScanner.TypeBucket bucket : this.lastBuckets.values())
        {
            for (Entity entity : bucket.entities)
            {
                if (!this.setup.selectedEntityIds.contains(entity.getId()))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isAllVanilla()
    {
        if (this.lastBuckets.isEmpty())
        {
            return false;
        }

        for (MobCaptureAreaScanner.TypeBucket bucket : this.lastBuckets.values())
        {
            for (Entity entity : bucket.entities)
            {
                if (!this.setup.vanillaPlaybackEntityIds.contains(entity.getId()))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private void setAllSelected(boolean selected)
    {
        this.setup.selectedEntityIds.clear();
        this.setup.selectedTypeIds.clear();

        if (!selected)
        {
            return;
        }

        for (MobCaptureAreaScanner.TypeBucket bucket : this.lastBuckets.values())
        {
            this.setup.selectedTypeIds.add(bucket.typeId);

            for (Entity entity : bucket.entities)
            {
                this.setup.selectedEntityIds.add(entity.getId());
            }
        }
    }

    private void setAllVanilla(boolean enabled)
    {
        this.setup.vanillaPlaybackEntityIds.clear();
        this.setup.vanillaPlaybackTypeIds.clear();

        if (!enabled)
        {
            return;
        }

        for (MobCaptureAreaScanner.TypeBucket bucket : this.lastBuckets.values())
        {
            this.setup.vanillaPlaybackTypeIds.add(bucket.typeId);

            for (Entity entity : bucket.entities)
            {
                this.setup.vanillaPlaybackEntityIds.add(entity.getId());
            }
        }
    }

    private boolean isTypeFullySelected(MobCaptureAreaScanner.TypeBucket bucket)
    {
        if (bucket.entities.isEmpty())
        {
            return false;
        }

        for (Entity entity : bucket.entities)
        {
            if (!this.setup.selectedEntityIds.contains(entity.getId()))
            {
                return false;
            }
        }

        return true;
    }

    private void setTypeSelected(MobCaptureAreaScanner.TypeBucket bucket, boolean selected)
    {
        for (Entity entity : bucket.entities)
        {
            if (selected)
            {
                this.setup.selectedEntityIds.add(entity.getId());
            }
            else
            {
                this.setup.selectedEntityIds.remove(entity.getId());
            }
        }
    }

    private void syncTypeSelection(MobCaptureAreaScanner.TypeBucket bucket)
    {
        if (this.isTypeFullySelected(bucket))
        {
            this.setup.selectedTypeIds.add(bucket.typeId);
        }
        else
        {
            this.setup.selectedTypeIds.remove(bucket.typeId);
        }
    }

    private void setTypeVanilla(MobCaptureAreaScanner.TypeBucket bucket, boolean enabled)
    {
        if (enabled)
        {
            this.setup.vanillaPlaybackTypeIds.add(bucket.typeId);

            for (Entity entity : bucket.entities)
            {
                this.setup.vanillaPlaybackEntityIds.add(entity.getId());
            }
        }
        else
        {
            this.setup.vanillaPlaybackTypeIds.remove(bucket.typeId);

            for (Entity entity : bucket.entities)
            {
                this.setup.vanillaPlaybackEntityIds.remove(entity.getId());
            }
        }
    }

    private void syncTypeVanilla(MobCaptureAreaScanner.TypeBucket bucket)
    {
        if (this.isTypeFullyVanilla(bucket))
        {
            this.setup.vanillaPlaybackTypeIds.add(bucket.typeId);
        }
        else
        {
            this.setup.vanillaPlaybackTypeIds.remove(bucket.typeId);
        }
    }

    private boolean isTypeFullyVanilla(MobCaptureAreaScanner.TypeBucket bucket)
    {
        if (bucket.entities.isEmpty())
        {
            return false;
        }

        for (Entity entity : bucket.entities)
        {
            if (!this.setup.vanillaPlaybackEntityIds.contains(entity.getId()))
            {
                return false;
            }
        }

        return true;
    }

    private void submit()
    {
        MobCaptureRecordingSetup.pending = this.setup;
        this.close();

        if (this.callback != null)
        {
            this.callback.accept(this.setup);
        }
    }
}
