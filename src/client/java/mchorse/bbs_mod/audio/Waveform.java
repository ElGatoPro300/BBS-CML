package mchorse.bbs_mod.audio;

import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.List;

public class Waveform
{
    public float[] average;
    public float[] maximum;

    private List<Texture> sprites = new ArrayList<>();
    private int w;
    private int h;
    private int pixelsPerSecond;
    private float duration;

    public void generate(Wave data, List<ColorCode> colorCodes, int pixelsPerSecond, int height)
    {
        if (data.getBytesPerSample() != 2)
        {
            throw new IllegalStateException("Waveform generation doesn't support non 16-bit audio data!");
        }

        this.populate(data, pixelsPerSecond, height);
        this.render(colorCodes, data.getCues());
    }

    public void render(List<ColorCode> colorCodes, float[] cues)
    {
        this.delete();

        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE) / 2;
        int count = (int) Math.ceil(this.w / (double) maxTextureSize);
        int offset = 0;
        float time = 0;
        ColorCode code = this.getColorCode(colorCodes, time);
        Color tmp = new Color();

        for (int t = 0; t < count; t++)
        {
            Texture texture = new Texture();
            int width = Math.min(this.w - offset, maxTextureSize);

            Pixels pixels = Pixels.fromSize(width, this.h);

            for (int i = offset, j = 0, c = Math.min(offset + width, this.average.length); i < c; i++, j++)
            {
                float average = this.average[i];
                float maximum = this.maximum[i];

                int maxHeight = (int) (maximum * this.h);
                int avgHeight = (int) (average * (this.h - 1)) + 1;

                int color = Colors.WHITE;
                boolean background = false;

                if (code != null && !code.isInside(time)) code = null;
                if (code == null) code = this.getColorCode(colorCodes, time);
                if (code != null)
                {
                    color = Colors.setA(code.color, 1F);
                    background = true;
                }

                if (this.hasCue(cues, time))
                {
                    pixels.drawRect(j, 0, 1, this.h, Colors.ACTIVE | Colors.A75);
                }

                if (avgHeight > 0)
                {
                    if (background)
                    {
                        tmp.set(color);

                        for (int k = 0; k < this.h; k++)
                        {
                            tmp.a = 0.125F + 0.25F * (k / (float) this.h);

                            pixels.setColor(j, k, tmp);
                        }
                    }

                    pixels.drawRect(j, this.h / 2 - maxHeight / 2, 1, maxHeight, color);
                    pixels.drawRect(j, this.h / 2 - avgHeight / 2, 1, avgHeight, Colors.mulRGB(color, 0.8F));
                }

                time += 1 / (float) this.pixelsPerSecond;
            }

            pixels.rewindBuffer();

            texture.bind();
            texture.uploadTexture(pixels);
            texture.setFilter(GL11.GL_NEAREST);
            texture.setParameter(GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
            texture.unbind();

            this.sprites.add(texture);

            offset += maxTextureSize;
        }
    }

    private boolean hasCue(float[] cues, float time)
    {
        if (cues == null)
        {
            return false;
        }

        for (float cue : cues)
        {
            if (time >= cue && time - cue < 1.5F / this.pixelsPerSecond)
            {
                return true;
            }
        }

        return false;
    }

    private ColorCode getColorCode(List<ColorCode> colorCodes, float time)
    {
        if (colorCodes == null)
        {
            return null;
        }

        for (ColorCode colorCode : colorCodes)
        {
            if (colorCode.isInside(time))
            {
                return colorCode;
            }
        }

        return null;
    }

    public void populate(Wave data, int pixelsPerSecond, int height)
    {
        this.pixelsPerSecond = pixelsPerSecond;
        this.w = (int) (data.getDuration() * pixelsPerSecond);
        this.h = height;
        this.average = new float[this.w];
        this.maximum = new float[this.w];

        int region = data.getScanRegion(pixelsPerSecond);

        for (int i = 0; i < this.w; i ++)
        {
            int offset = i * region;
            int count = 0;
            float average = 0;
            float maximum = 0;

            for (int j = 0; j < region; j += 2 * data.numChannels)
            {
                if (offset + j + 1 >= data.data.length)
                {
                    break;
                }

                byte a = data.data[offset + j];
                byte b = data.data[offset + j + 1];
                float sample = a + (b << 8);

                maximum = Math.max(maximum, Math.abs(sample));
                average += Math.abs(sample);
                count++;
            }

            average /= count;
            average /= 0xffff / 2;
            maximum /= 0xffff / 2;

            this.average[i] = average;
            this.maximum[i] = maximum;
        }

        this.duration = data.getDuration();
    }

    public void delete()
    {
        for (Texture sprite : this.sprites)
        {
            sprite.delete();
        }

        this.sprites.clear();
    }

    public boolean isCreated()
    {
        return !this.sprites.isEmpty();
    }

    public int getPixelsPerSecond()
    {
        return this.pixelsPerSecond;
    }

    public int getWidth()
    {
        return this.w;
    }

    public int getHeight()
    {
        return this.h;
    }

    public float getDuration()
    {
        return this.duration;
    }

    public List<Texture> getSprites()
    {
        return this.sprites;
    }

    /**
     * Draw the waveform out of multiple sprites of desired cropped region
     */
    public void render(Batcher2D batcher, int color, int x, int y, int w, int h, float startTime, float endTime)
    {
        float pixelsPerSecond = w / (endTime - startTime);
        float xOffset = x - (startTime * pixelsPerSecond);
        float timeOffset = 0F;

        batcher.clip(x, y, w, h, 0, 0);

        for (Texture sprite : this.sprites)
        {
            float duration = sprite.width / (float) this.pixelsPerSecond;
            float timeEnd = timeOffset + duration;

            float spriteW = duration * pixelsPerSecond;
            float u1 = 0F;
            float u2 = sprite.width;

            if (timeOffset >= endTime)
            {
                break;
            }

            batcher.texturedBox(sprite, color, xOffset, y, spriteW, h, u1, 0, u2, sprite.height, sprite.width, sprite.height);

            timeOffset = timeEnd;
            xOffset += spriteW;
        }

        batcher.unclip(0, 0);
    }
}