package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

public class UIWelcomePanel extends UIElement {
    private enum PopupState {
        WELCOME, BETA_WARNING, DOWNGRADE_WARNING
    }

    private PopupState state = PopupState.WELCOME;
    private long startTime = System.currentTimeMillis();
    private long popupStartTime = 0;

    public UIButton buttonYes;
    public UIButton buttonNo;
    public UIButton buttonAccept;
    public UIButton buttonExit;

    public UIWelcomePanel() {
        super();

        this.mousePropagation = EventPropagation.BLOCK;
        this.keyboardPropagation = EventPropagation.BLOCK;

        this.buttonYes = new UIButton(UIKeys.WELCOME_YES, (b) -> {
            this.setPopupState(PopupState.BETA_WARNING);
        });
        this.buttonNo = new UIButton(UIKeys.WELCOME_NO, (b) -> {
            this.setPopupState(PopupState.DOWNGRADE_WARNING);
        });
        this.buttonAccept = new UIButton(UIKeys.WELCOME_ACCEPT, (b) -> {
            this.acceptBeta();
        });
        this.buttonExit = new UIButton(UIKeys.WELCOME_EXIT, (b) -> {
            this.exitGame();
        });

        this.add(this.buttonYes, this.buttonNo, this.buttonAccept, this.buttonExit);
        this.setPopupState(PopupState.WELCOME);
    }

    private void setPopupState(PopupState state) {
        this.state = state;
        this.popupStartTime = System.currentTimeMillis();

        boolean isWelcome = state == PopupState.WELCOME;
        this.buttonYes.setVisible(isWelcome);
        this.buttonNo.setVisible(isWelcome);

        this.buttonAccept.setVisible(state == PopupState.BETA_WARNING);
        this.buttonExit.setVisible(state == PopupState.DOWNGRADE_WARNING);
    }

    private void acceptBeta() {
        BBSSettings.welcomePanelAcceptedBeta1.set(true);
        this.removeFromParent();
    }

    private void exitGame() {
        MinecraftClient.getInstance().scheduleStop();
    }

    @Override
    public boolean subKeyPressed(UIContext context) {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE)) {
            return true;
        }
        return super.subKeyPressed(context);
    }

    @Override
    public void resize() {
        super.resize();

        int btnW = 120;
        int btnH = 20;
        int gap = 20;

        int centerY = this.area.my() + 55;
        int totalW = btnW * 2 + gap;
        int startX = this.area.mx() - totalW / 2;

        this.buttonYes.relative(this).xy(startX, centerY).wh(btnW, btnH);
        this.buttonNo.relative(this).xy(startX + btnW + gap, centerY).wh(btnW, btnH);

        int popupCenterY = this.area.my();
        int popupBtnY = popupCenterY + 50;

        this.buttonAccept.relative(this).xy(this.area.mx() - 100, popupBtnY).wh(200, 20);
        this.buttonExit.relative(this).xy(this.area.mx() - 60, popupBtnY).wh(120, 20);
    }

    private void outline(Batcher2D batcher, float x1, float y1, float x2, float y2, int color) {
        batcher.box(x1, y1, x2, y1 + 1, color);
        batcher.box(x1, y2 - 1, x2, y2, color);
        batcher.box(x1, y1, x1 + 1, y2, color);
        batcher.box(x2 - 1, y1, x2, y2, color);
    }

    private void drawPlayerHead(DrawContext drawContext, Identifier skinTexture, int x, int y, int size) {
        drawContext.drawTexture(skinTexture, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        drawContext.drawTexture(skinTexture, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    @Override
    public void render(UIContext context) {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xEB0F0F11);

        long now = System.currentTimeMillis();
        float elapsed = (now - this.startTime) / 1000.0F;

        float textAlpha = Math.min(elapsed / 1.0F, 1.0F);
        float buttonElapsed = Math.max(0.0F, elapsed - 0.5F);
        float buttonAlpha = Math.min(buttonElapsed / 0.8F, 1.0F);
        float buttonOffset = (1.0F - buttonAlpha) * 15.0F;

        if (this.state == PopupState.WELCOME) {
            int centerY = this.area.my() + 55;
            this.buttonYes.y(0F, centerY + (int) buttonOffset);
            this.buttonNo.y(0F, centerY + (int) buttonOffset);
            this.buttonYes.custom = true;
            this.buttonYes.customColor = Colors.setA(BBSSettings.primaryColor.get(), buttonAlpha);
            this.buttonNo.custom = true;
            this.buttonNo.customColor = Colors.setA(0xffd9534f, buttonAlpha);
            this.buttonYes.textColor = Colors.setA(Colors.WHITE, buttonAlpha);
            this.buttonNo.textColor = Colors.setA(Colors.WHITE, buttonAlpha);
            this.buttonYes.resize();
            this.buttonNo.resize();
        }

        if (this.state == PopupState.WELCOME) {
            float floatOffset = (float) Math.sin(elapsed * 2.5F) * 5.0F;
            Texture logo = BBSModClient.getTextures().getTexture(Link.assets("textures/bbs_cml.png"));
            if (logo != null) {
                float logoW = logo.width * 4.5F;
                float logoH = logo.height * 4.5F;
                float logoX = this.area.mx() - logoW / 2.0F;
                float logoY = this.area.my() - 140 + floatOffset;
                context.batcher.texturedBox(logo, Colors.setA(Colors.WHITE, textAlpha), logoX, logoY, logoW, logoH, 0,
                        0, logo.width, logo.height);
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            String username = mc.player != null ? mc.player.getGameProfile().getName() : mc.getSession().getUsername();
            Identifier skinTexture = null;
            if (mc.player != null) {
                try {
                    skinTexture = mc.player.getSkinTexture();
                } catch (Exception e) {
                }
            }

            FontRenderer font = context.batcher.getFont();
            String welcomePart1 = UIKeys.WELCOME_TITLE1.get();
            String welcomePart2 = " " + username + UIKeys.WELCOME_TITLE2.get();

            int w1 = font.getWidth(welcomePart1);
            int headSize = 14;
            int gapText = 4;
            int w2 = font.getWidth(welcomePart2);
            int totalW = w1 + headSize + gapText + w2;

            float scale = 1.25F;
            float realX = this.area.mx();
            float greetRealY = this.area.my() - 15;

            float drawX = (realX / scale) - (totalW / 2.0F);
            float drawY = greetRealY / scale;

            MatrixStack matrices = context.batcher.getContext().getMatrices();
            matrices.push();
            matrices.scale(scale, scale, 1.0F);

            context.batcher.textShadow(welcomePart1, drawX, drawY, Colors.setA(Colors.WHITE, textAlpha));
            float headX = drawX + w1;
            float headY = drawY - 2;

            if (skinTexture != null) {
                this.drawPlayerHead(context.batcher.getContext(), skinTexture, (int) headX, (int) headY, headSize);
            } else {
                context.batcher.iconArea(Icons.USER, Colors.setA(Colors.WHITE, textAlpha), headX, headY, headSize,
                        headSize);
            }

            context.batcher.textShadow(welcomePart2, headX + headSize + gapText, drawY,
                    Colors.setA(Colors.WHITE, textAlpha));

            matrices.pop();

            float readyScale = 1.4F;
            String readyText = UIKeys.WELCOME_READY.get();
            float readyW = font.getWidth(readyText);
            float readyRealY = this.area.my() + 20;

            float readyDrawX = (realX / readyScale) - (readyW / 2.0F);
            float readyDrawY = readyRealY / readyScale;

            matrices.push();
            matrices.scale(readyScale, readyScale, 1.0F);
            context.batcher.textShadow(readyText, readyDrawX, readyDrawY, Colors.setA(Colors.WHITE, textAlpha));
            matrices.pop();
        } else {
            float popupProgress = Math.min((System.currentTimeMillis() - this.popupStartTime) / 400.0F, 1.0F);
            float easeProgress = 1.0F - (1.0F - popupProgress) * (1.0F - popupProgress);
            float slideOffset = (1.0F - easeProgress) * this.area.h;

            float popupW = 340;
            float popupH = 200;
            float popupX = this.area.mx() - popupW / 2.0F;
            float popupY = this.area.my() - popupH / 2.0F + slideOffset;

            if (this.state == PopupState.BETA_WARNING) {
                this.buttonAccept.y(0F, (int) (popupY + popupH - 35));
                this.buttonAccept.resize();
            } else if (this.state == PopupState.DOWNGRADE_WARNING) {
                this.buttonExit.y(0F, (int) (popupY + popupH - 35));
                this.buttonExit.resize();
            }

            context.batcher.box(popupX, popupY, popupX + popupW, popupY + popupH, 0xFA141416);
            this.outline(context.batcher, popupX, popupY, popupX + popupW, popupY + popupH, 0x40FFFFFF);

            float iconSize = 24;
            float iconX = this.area.mx() - iconSize / 2.0F;
            float iconY = popupY + 15;

            if (Icons.WARNING.texture != null) {
                Texture iconTex = BBSModClient.getTextures().getTexture(Icons.WARNING.texture);
                if (iconTex != null) {
                    context.batcher.texturedBox(iconTex, 0xffffaa00, iconX, iconY, iconSize, iconSize, Icons.WARNING.x,
                            Icons.WARNING.y, Icons.WARNING.x + Icons.WARNING.w, Icons.WARNING.y + Icons.WARNING.h,
                            Icons.WARNING.textureW, Icons.WARNING.textureH);
                }
            }

            float textY = iconY + iconSize + 12;
            int textWidth = (int) popupW - 30;

            if (this.state == PopupState.BETA_WARNING) {
                String warningText = UIKeys.WELCOME_BETA_WARNING.get();
                context.batcher.wallText(warningText, (int) (popupX + 15), (int) textY, Colors.WHITE, textWidth, 11,
                        0.5F, 0.0F);
            } else if (this.state == PopupState.DOWNGRADE_WARNING) {
                String downgradeText = UIKeys.WELCOME_DOWNGRADE_WARNING.get();
                context.batcher.wallText(downgradeText, (int) (popupX + 15), (int) textY, Colors.WHITE, textWidth, 11,
                        0.5F, 0.0F);
            }
        }

        super.render(context);
    }
}
