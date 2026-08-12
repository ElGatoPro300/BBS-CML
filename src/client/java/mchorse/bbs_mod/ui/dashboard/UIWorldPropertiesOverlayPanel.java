package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

/**
 * World Properties overlay (World menu bar button). Collapsible Time, Weather, and Mobs sections. Toggle states
 * are read from the integrated server's gamerules when the panel opens. Time-slider updates use the
 * same client-side sun override as the Sun Rotation curve for smooth dragging, with throttled server sync.
 */
public class UIWorldPropertiesOverlayPanel extends UIOverlayPanel
{
    private static final long THROTTLE_MS = 10L;

    private static int openCount;

    private UIScrollView scroll;

    private UIToggle freezeTime;
    private UITrackpad time;
    private UIElement timeContent;
    private UIToggle pauseWeather;
    private UIElement weatherContent;
    private UIToggle mobSpawning;
    private UIButton killMobs;
    private UIElement mobsContent;
    private UITrackpad gamma;
    private UIToggle nightVision;
    private UIElement gammaContent;

    private boolean timeExpanded = false;
    private boolean weatherExpanded = false;
    private boolean mobsExpanded = false;
    private boolean gammaExpanded = false;

    private int pendingTime = -1;
    private int lastSentTime = -1;
    private long lastSentAt;
    private static final int KILL_MOBS_SUCCESS_GREEN = 0x1B6620;

    private int killMobsHighlightColor;

    public static boolean isOpen()
    {
        return openCount > 0;
    }

    public UIWorldPropertiesOverlayPanel()
    {
        super(UIKeys.WORLD_PROPERTIES);

        openCount++;

        this.resizable().minSize(240, 200);

        this.freezeTime = new UIToggle(UIKeys.WORLD_FREEZE_TIME, !WorldPropertiesHelper.readGamerule("doDaylightCycle", true), (b) ->
            WorldPropertiesHelper.setGamerule("doDaylightCycle", !b.getValue()));

        this.time = new UITrackpad((v) -> this.pendingTime = (int) v.doubleValue());
        this.time.limit(0D, 24000D, true).increment(100D).values(100D, 10D, 1000D);

        UIButton day = new UIButton(UIKeys.WORLD_TIME_DAY, (b) -> this.setTime(1000));
        UIButton noon = new UIButton(UIKeys.WORLD_TIME_NOON, (b) -> this.setTime(6000));
        UIButton night = new UIButton(UIKeys.WORLD_TIME_NIGHT, (b) -> this.setTime(13000));
        UIButton midnight = new UIButton(UIKeys.WORLD_TIME_MIDNIGHT, (b) -> this.setTime(18000));

        this.timeContent = UI.column(5, 0,
            this.freezeTime,
            UI.label(UIKeys.WORLD_TIME_LABEL).marginTop(4),
            this.time,
            UI.row(4, day, noon, night, midnight)
        );

        this.pauseWeather = new UIToggle(UIKeys.WORLD_PAUSE_WEATHER, !WorldPropertiesHelper.readGamerule("doWeatherCycle", true), (b) ->
            WorldPropertiesHelper.setGamerule("doWeatherCycle", !b.getValue()));

        UIButton clear = new UIButton(UIKeys.WORLD_WEATHER_CLEAR, (b) -> WorldPropertiesHelper.setWeatherClear());
        UIButton rain = new UIButton(UIKeys.WORLD_WEATHER_RAIN, (b) -> WorldPropertiesHelper.setWeatherRain());
        UIButton thunder = new UIButton(UIKeys.WORLD_WEATHER_THUNDER, (b) -> WorldPropertiesHelper.setWeatherThunder());

        this.weatherContent = UI.column(5, 0,
            this.pauseWeather,
            UI.row(4, clear, rain, thunder)
        );

        this.mobSpawning = new UIToggle(UIKeys.WORLD_MOB_SPAWN, WorldPropertiesHelper.readGamerule("doMobSpawning", true), (b) ->
            WorldPropertiesHelper.setGamerule("doMobSpawning", b.getValue()));

        this.killMobs = new UIButton(UIKeys.WORLD_KILL_ALL_MOBS, this::killAllMobsClicked)
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                boolean shiftHover = Window.isShiftPressed() && this.area.isInside(context);
                boolean greenAfter = UIWorldPropertiesOverlayPanel.this.killMobsHighlightColor != 0;

                if (shiftHover || greenAfter)
                {
                    int bg = shiftHover ? (Colors.RED | Colors.A100) : (UIWorldPropertiesOverlayPanel.this.killMobsHighlightColor | Colors.A100);

                    this.area.render(context.batcher, bg);

                    FontRenderer font = context.batcher.getFont();
                    String label = font.limitToWidth(this.label.get(), this.area.w - 6);

                    context.batcher.text(label, this.area.mx(font.getWidth(label)), this.area.my(font.getHeight()), Colors.WHITE);

                    return;
                }

                super.renderSkin(context);
            }
        };

        this.mobsContent = UI.column(5, 0, this.mobSpawning, this.killMobs);

        this.gamma = new UITrackpad((v) -> WorldPropertiesHelper.setGammaPercent(v));
        this.gamma.limit(0D, 1500D, true).increment(50D).values(10D, 1D, 100D);
        this.gamma.setValue(WorldPropertiesHelper.getGammaPercent());

        UIButton gammaNormal = new UIButton(UIKeys.WORLD_GAMMA_NORMAL, (b) -> { this.gamma.setValue(100D); WorldPropertiesHelper.setGammaPercent(100D); });
        UIButton gammaSemi = new UIButton(UIKeys.WORLD_GAMMA_SEMI, (b) -> { this.gamma.setValue(750D); WorldPropertiesHelper.setGammaPercent(750D); });
        UIButton gammaFull = new UIButton(UIKeys.WORLD_GAMMA_FULL, (b) -> { this.gamma.setValue(1500D); WorldPropertiesHelper.setGammaPercent(1500D); });

        this.nightVision = new UIToggle(UIKeys.WORLD_GAMMA_NIGHT_VISION, WorldPropertiesHelper.hasNightVision(), (b) ->
            WorldPropertiesHelper.setNightVision(b.getValue()));

        this.gammaContent = UI.column(5, 0,
            UI.label(UIKeys.WORLD_GAMMA_LABEL),
            this.gamma,
            UI.row(4, gammaNormal, gammaSemi, gammaFull),
            this.nightVision
        );

        this.scroll = new UIScrollView();
        this.scroll.relative(this.content).w(1F).h(1F);
        this.scroll.column(6).vertical().stretch().scroll().padding(6);

        this.content.add(this.scroll);
        this.syncFromWorld();
        this.rebuild();
    }

    private void syncFromWorld()
    {
        ClientWorld world = MinecraftClient.getInstance().world;
        long timeOfDay = world == null ? 6000L : world.getTimeOfDay() % 24000L;

        if (timeOfDay < 0L)
        {
            timeOfDay += 24000L;
        }

        this.lastSentTime = (int) timeOfDay;
        this.pendingTime = this.lastSentTime;
        this.time.setValue(this.lastSentTime);
        WorldPropertiesHelper.setClientTimeOverride(timeOfDay);

        this.freezeTime.setValue(!WorldPropertiesHelper.readGamerule("doDaylightCycle", true));
        this.pauseWeather.setValue(!WorldPropertiesHelper.readGamerule("doWeatherCycle", true));
        this.mobSpawning.setValue(WorldPropertiesHelper.readGamerule("doMobSpawning", true));
    }

    private void killAllMobsClicked(UIButton button)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        if (Window.isShiftPressed())
        {
            this.executeKillAllMobs();

            return;
        }

        UIOverlay.addOverlay(context, new UIConfirmOverlayPanel(
            UIKeys.WORLD_KILL_ALL_MOBS,
            UIKeys.WORLD_KILL_ALL_MOBS_CONFIRM,
            (confirmed) ->
            {
                if (confirmed)
                {
                    this.executeKillAllMobs();
                }
            }
        ), 260, 140);
    }

    private void executeKillAllMobs()
    {
        this.killMobsHighlightColor = 0;

        WorldPropertiesHelper.killAllMobs((count) ->
        {
            if (count == 0)
            {
                this.killMobsHighlightColor = KILL_MOBS_SUCCESS_GREEN;
            }
        });
    }

    private void rebuild()
    {
        this.scroll.removeAll();

        this.scroll.add(this.header(UIKeys.WORLD_SECTION_TIME, this.timeExpanded, () ->
        {
            this.timeExpanded = !this.timeExpanded;
            this.rebuild();
        }));

        if (this.timeExpanded)
        {
            this.scroll.add(this.timeContent);
        }

        this.scroll.add(this.header(UIKeys.WORLD_SECTION_WEATHER, this.weatherExpanded, () ->
        {
            this.weatherExpanded = !this.weatherExpanded;
            this.rebuild();
        }));

        if (this.weatherExpanded)
        {
            this.scroll.add(this.weatherContent);
        }

        this.scroll.add(this.header(UIKeys.WORLD_SECTION_MOBS, this.mobsExpanded, () ->
        {
            this.mobsExpanded = !this.mobsExpanded;
            this.rebuild();
        }));

        if (this.mobsExpanded)
        {
            this.scroll.add(this.mobsContent);
        }

        this.scroll.add(this.header(UIKeys.WORLD_SECTION_GAMMA, this.gammaExpanded, () ->
        {
            this.gammaExpanded = !this.gammaExpanded;
            this.rebuild();
        }));

        if (this.gammaExpanded)
        {
            this.scroll.add(this.gammaContent);
        }

        if (this.scroll.hasParent())
        {
            this.scroll.resize();
        }
    }

    private UIElement header(IKey label, boolean expanded, Runnable toggle)
    {
        UIIcon icon = new UIIcon(expanded ? Icons.MOVE_DOWN : Icons.MOVE_RIGHT, (b) -> toggle.run());
        UIButton title = new UIButton(label, (b) -> toggle.run());

        title.w(1F);

        return UI.row(4, 0, 20, icon, title);
    }

    private void setTime(int value)
    {
        this.time.setValue(value);
        this.pendingTime = value;
        this.flushTime(true);
    }

    private void flushTime(boolean force)
    {
        if (this.pendingTime < 0)
        {
            return;
        }

        WorldPropertiesHelper.setClientTimeOverride(this.pendingTime);

        if (this.pendingTime == this.lastSentTime)
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (!force && now - this.lastSentAt < THROTTLE_MS)
        {
            return;
        }

        this.lastSentAt = now;
        this.lastSentTime = this.pendingTime;
        WorldPropertiesHelper.setTimeOfDay(this.pendingTime);
    }

    @Override
    public void render(UIContext context)
    {
        this.flushTime(false);

        super.render(context);
    }

    @Override
    public void onClose()
    {
        openCount = Math.max(0, openCount - 1);

        WorldPropertiesHelper.clearClientTimeOverride();

        super.onClose();
    }
}
