package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;

import org.joml.Vector4f;

public class ImageOverlay
{
    public Link texture;
    public boolean linear;
    public boolean mipmap;
    public boolean resizeCrop;
    public Vector4f crop = new Vector4f();
    public Color color = new Color();
    public float offsetX;
    public float offsetY;
    public float rotation;
    public TextureBlend textureBlend;

    public float x;
    public float y;
    public float width;
    public float height;
    public float anchorX;
    public float anchorY;
    public float windowX;
    public float windowY;
    public float opacity;

    public int renderOrder;

    public void updateTexture(Link texture, boolean linear, boolean mipmap, boolean resizeCrop, Vector4f crop, Color color, float offsetX, float offsetY, float rotation, TextureBlend textureBlend)
    {
        this.texture = texture;
        this.linear = linear;
        this.mipmap = mipmap;
        this.resizeCrop = resizeCrop;
        this.crop.set(crop);
        this.color.copy(color);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotation = rotation;
        this.textureBlend = textureBlend;
    }

    public void updateLayout(float x, float y, float width, float height, float anchorX, float anchorY, float windowX, float windowY, float opacity)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.windowX = windowX;
        this.windowY = windowY;
        this.opacity = opacity;
    }
}
