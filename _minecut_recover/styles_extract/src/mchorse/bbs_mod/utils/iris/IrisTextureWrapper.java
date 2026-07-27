package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.CollectionUtils;
import net.minecraft.class_1044;
import net.minecraft.class_3300;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class IrisTextureWrapper extends class_1044
{
    public enum PBRMapType
    {
        NONE,
        NORMAL,
        SPECULAR
    }

    public final Link texture;
    public final class_1044 fallback;
    public final int index;
    public final float normalIntensity;
    public final float specularIntensity;
    public final PBRMapType pbrMapType;

    public IrisTextureWrapper(Link texture, int index)
    {
        this(texture, null, index, 1F, 1F, PBRMapType.NONE);
    }

    public IrisTextureWrapper(Link texture, int index, float normalIntensity, float specularIntensity)
    {
        this(texture, null, index, normalIntensity, specularIntensity, PBRMapType.NONE);
    }

    public IrisTextureWrapper(Link texture, class_1044 fallback, int index)
    {
        this(texture, fallback, index, 1F, 1F, PBRMapType.NONE);
    }

    public IrisTextureWrapper(Link texture, class_1044 fallback, int index, float normalIntensity, float specularIntensity, PBRMapType pbrMapType)
    {
        this.texture = texture;
        this.fallback = fallback;
        this.index = index;
        this.normalIntensity = normalIntensity;
        this.specularIntensity = specularIntensity;
        this.pbrMapType = pbrMapType;
    }

    @Override
    public void method_4625(class_3300 manager) throws IOException
    {}

    @Override
    public int method_4624()
    {
        Texture texture = BBSModClient.getTextures().getTexture(this.texture, GL11.GL_NEAREST, true);

        if (texture == null || texture == BBSModClient.getTextures().getError())
        {
            return this.fallback == null ? -1 : this.fallback.method_4624();
        }

        if (this.index >= 0 && texture.getParent() != null)
        {
            Texture frame = CollectionUtils.getSafe(texture.getParent().textures, this.index);

            if (frame != null)
            {
                texture = frame;
            }
        }

        if (this.pbrMapType == PBRMapType.NORMAL)
        {
            float intensity = IrisUtils.getActivePBRNormalIntensity();

            if (Math.abs(intensity - 1F) < 0.0001F)
            {
                intensity = this.normalIntensity;
            }

            return IrisPBRTextureProcessor.getTextureId(texture, intensity, this.pbrMapType);
        }
        else if (this.pbrMapType == PBRMapType.SPECULAR)
        {
            float intensity = IrisUtils.getActivePBRSpecularIntensity();

            if (Math.abs(intensity - 1F) < 0.0001F)
            {
                intensity = this.specularIntensity;
            }

            return IrisPBRTextureProcessor.getTextureId(texture, intensity, this.pbrMapType);
        }

        return texture.id;
    }

    @Override
    public void close()
    {
        BBSModClient.getTextures().delete(this.texture);
    }
}