package mchorse.bbs_mod.camera.clips.misc;

public class Subtitle
{
    public String label = "";
    public int x;
    public int y;
    public float size;
    public float anchorX;
    public float anchorY;
    public float windowX;
    public float windowY;
    public int color;
    public boolean textShadow;
    public int backgroundColor;
    public float backgroundOffset;
    public float shadow;
    public boolean shadowOpaque;

    public float lineHeight;
    public float maxWidth;
    public int renderOrder;

    public void update(String label, int x, int y, float size, float anchorX, float anchorY, int color, boolean textShadow)
    {
        this.label = label;
        this.x = x;
        this.y = y;
        this.size = size;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.color = color;
        this.textShadow = textShadow;
    }

    public void updateWindow(float x, float y)
    {
        this.windowX = x;
        this.windowY = y;
    }

    public void updateBackground(int backgroundColor, float backgroundOffset, float shadow, boolean shadowOpaque)
    {
        this.backgroundColor = backgroundColor;
        this.backgroundOffset = backgroundOffset;
        this.shadow = shadow;
        this.shadowOpaque = shadowOpaque;
    }

    public void updateConstraints(float lineHeight, float maxWidth)
    {
        this.lineHeight = lineHeight;
        this.maxWidth = maxWidth;
    }
}
