package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VanillaParticleFormRenderer extends FormRenderer<VanillaParticleForm> implements ITickable
{
    public static final Link PARTICLE_PREVIEW = new Link("minecraft", "textures/particle/flame.png");

    private Vector3d pos = new Vector3d();
    private Vector3f vel = new Vector3f();
    private Matrix3f rot = new Matrix3f();
    private int tick;
    private List<TrackedParticle> trackedParticles = new ArrayList<>();

    private static class TrackedParticle
    {
        public SingleQuadParticle particle;
        public mchorse.bbs_mod.utils.colors.Color startColor;
        public mchorse.bbs_mod.utils.colors.Color endColor;

        public TrackedParticle(SingleQuadParticle particle, mchorse.bbs_mod.utils.colors.Color startColor, mchorse.bbs_mod.utils.colors.Color endColor)
        {
            this.particle = particle;
            this.startColor = startColor.copy();
            this.endColor = endColor.copy();
        }
    }

    public VanillaParticleFormRenderer(VanillaParticleForm form)
    {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Texture texture = context.render.getTextures().getTexture(PARTICLE_PREVIEW);

        float min = Math.min(texture.width, texture.height);
        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;

        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);

        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;

        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    @Override
    public boolean is3D()
    {
        return false;
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        super.render3D(context);

        /* Illusion re-draws must not overwrite the primary form pose used for spawning. */
        if (context.trailInstance != 0)
        {
            return;
        }

        Matrix4f positionMatrix;

        if (context.type == FormRenderType.PREVIEW)
        {
            Camera realCamera = Minecraft.getInstance().gameRenderer.mainCamera();

            positionMatrix = new Matrix4f().rotation(realCamera.rotation());
            positionMatrix.mul(context.stack.last().pose());

            Vector3f translation = positionMatrix.getTranslation(new Vector3f());

            this.pos.set(
                translation.x + (float) realCamera.position().x,
                translation.y + (float) realCamera.position().y,
                translation.z + (float) realCamera.position().z
            );
        }
        else
        {
            positionMatrix = new Matrix4f(context.stack.last().pose());

            Vector3f translation = positionMatrix.getTranslation(new Vector3f());

            this.pos.set(
                translation.x + context.camera.position.x,
                translation.y + context.camera.position.y,
                translation.z + context.camera.position.z
            );
        }

        positionMatrix.get3x3(this.rot);

        this.vel.set(0F, 0F, 1F);
        this.rot.transform(this.vel);
    }

    @Override
    public void tick(IEntity entity)
    {
        Level world = entity == null ? null : entity.getWorld();

        if (world == null)
        {
            world = Minecraft.getInstance().level;
        }

        boolean paused = this.form.paused.get();
        Vector3f temp3f = new Vector3f();

        if (world != null && Minecraft.getInstance().level != null && !paused)
        {
            if (!this.trackedParticles.isEmpty())
            {
                Iterator<TrackedParticle> iterator = this.trackedParticles.iterator();

                while (iterator.hasNext())
                {
                    TrackedParticle tracked = iterator.next();

                    if (!tracked.particle.isAlive())
                    {
                        iterator.remove();
                        continue;
                    }

                    int maxAge = tracked.particle.lifetime;
                    int age = tracked.particle.age;

                    float progress = maxAge > 0 ? (float) age / (float) maxAge : 1F;
                    progress = MathUtils.clamp(progress, 0F, 1F);

                    float r = Lerps.lerp(tracked.startColor.r, tracked.endColor.r, progress);
                    float g = Lerps.lerp(tracked.startColor.g, tracked.endColor.g, progress);
                    float b = Lerps.lerp(tracked.startColor.b, tracked.endColor.b, progress);
                    float a = Lerps.lerp(tracked.startColor.a, tracked.endColor.a, progress);

                    if (tracked.particle instanceof SingleQuadParticle bbp)
                    {
                        bbp.setColor(r, g, b);
                        bbp.setAlpha(a);
                    }
                }
            }

            float velocity = this.form.velocity.get();
            int count = this.form.count.get();
            int frequency = this.form.frequency.get();

            if (this.tick <= 0)
            {
                Matrix3f m = Matrices.TEMP_3F;
                Vector3f v = Vectors.TEMP_3F;
                ParticleSettings settings = this.form.settings.get();
                ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(settings.particle);
                ParticleOptions effect = ParticleTypes.FLAME;

                if (type != null)
                {
                    HolderLookup.Provider registries = world.registryAccess();
                    String path = settings.particle != null ? settings.particle.getPath() : "";
                    String args = settings.arguments.trim();

                    float colorR = -1F;
                    float colorG = -1F;
                    float colorB = -1F;
                    float colorA = 1F;

                    boolean hasExplicitArgColor = false;

                    if (!args.isEmpty())
                    {
                        try
                        {
                            String[] split = args.split("\\s+");

                            if (split.length >= 3)
                            {
                                colorR = Float.parseFloat(split[0]);
                                colorG = Float.parseFloat(split[1]);
                                colorB = Float.parseFloat(split[2]);
                                hasExplicitArgColor = true;

                                if (split.length >= 4)
                                {
                                    colorA = Float.parseFloat(split[3]);
                                }
                            }
                        }
                        catch (Exception e)
                        {}
                    }

                    mchorse.bbs_mod.utils.colors.Color color1 = this.form.color.get();
                    mchorse.bbs_mod.utils.colors.Color color2 = this.form.color2.get();
                    int colorMode = this.form.colorMode.get();

                    boolean isEffect = path.contains("effect");
                    boolean isDust = path.contains("dust");
                    boolean hasCustomRgb = colorMode != 0
                        || hasExplicitArgColor
                        || (color1 != null && (color1.r != 1F || color1.g != 1F || color1.b != 1F));
                    boolean hasCustomAlpha = (color1 != null && color1.a != 1F) || (hasExplicitArgColor && colorA != 1F);

                    if (colorR < 0F && color1 != null && (hasCustomRgb || hasCustomAlpha || isEffect || isDust))
                    {
                        colorR = color1.r;
                        colorG = color1.g;
                        colorB = color1.b;
                        colorA = color1.a;
                    }

                    boolean parsedCustom = false;

                    if (colorR >= 0F)
                    {
                        if (isEffect)
                        {
                            @SuppressWarnings("unchecked")
                            ParticleType<ColorParticleOption> entityEffectType = (ParticleType<ColorParticleOption>) ParticleTypes.ENTITY_EFFECT;
                            effect = ColorParticleOption.create(entityEffectType, colorR, colorG, colorB);
                            parsedCustom = true;
                        }
                        else if (path.equals("dust_color_transition"))
                        {
                            float scale = colorA > 0F ? colorA : 1F;
                            int rgb = new mchorse.bbs_mod.utils.colors.Color(colorR, colorG, colorB).getRGBColor();
                            int rgb2 = (colorMode == 1 && color2 != null) ? color2.getRGBColor() : rgb;

                            effect = new DustColorTransitionOptions(rgb, rgb2, scale);
                            parsedCustom = true;
                        }
                        else if (isDust)
                        {
                            float scale = colorA > 0F ? colorA : 1F;
                            int rgb = new mchorse.bbs_mod.utils.colors.Color(colorR, colorG, colorB).getRGBColor();

                            effect = new DustParticleOptions(rgb, scale);
                            parsedCustom = true;
                        }
                    }

                    if (!parsedCustom)
                    {
                        if (type instanceof SimpleParticleType simple)
                        {
                            effect = simple;
                        }
                        else if (registries != null)
                        {
                            String full = settings.particle.toString();

                            if (!args.isEmpty())
                            {
                                full += " " + args;
                            }

                            try
                            {
                                effect = ParticleArgument.readParticle(new StringReader(full), registries);
                            }
                            catch (Exception e)
                            {
                                /* Manual fallbacks for common complex particles using direct registry lookups */
                                if (!args.isEmpty())
                                {
                                    try
                                    {
                                        Identifier id = Identifier.tryParse(args.contains(":") ? args : "minecraft:" + args);

                                        if (id != null)
                                        {
                                            /* Try to find as block first */
                                            Block block = BuiltInRegistries.BLOCK.getValue(id);

                                            if (block != Blocks.AIR)
                                            {
                                                effect = new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
                                            }
                                            else
                                            {
                                                /* Try to find as item */
                                                Item item = BuiltInRegistries.ITEM.getValue(id);

                                                if (item != Items.AIR)
                                                {
                                                    effect = new ItemParticleOption(ParticleTypes.ITEM, item);
                                                }
                                            }
                                        }
                                    }
                                    catch (Exception e2)
                                    {}
                                }
                            }
                        }
                    }

                    List<FormIllusionRenderer.EmissionSite> sites = FormIllusionRenderer.collectEmissionSites(this.form, entity);
                    boolean distribute = FormIllusionRenderer.shouldDistributeParticles(this.form) && sites.size() > 1;
                    Matrix3f siteRot = new Matrix3f();
                    Vector3f siteForward = new Vector3f();
                    Vector3f siteOrigin = new Vector3f();

                    if (distribute)
                    {
                        for (int i = 0; i < count; i++)
                        {
                            this.resolveEmissionSite(sites.get(i % sites.size()), siteRot, siteForward, siteOrigin);
                            this.spawnParticle(world, effect, path, velocity, colorR, colorG, colorB, colorA, color1, color2, colorMode, hasCustomRgb, hasCustomAlpha, siteRot, siteForward, siteOrigin, m, v, temp3f);
                        }
                    }
                    else
                    {
                        for (FormIllusionRenderer.EmissionSite site : sites)
                        {
                            this.resolveEmissionSite(site, siteRot, siteForward, siteOrigin);

                            for (int i = 0; i < count; i++)
                            {
                                this.spawnParticle(world, effect, path, velocity, colorR, colorG, colorB, colorA, color1, color2, colorMode, hasCustomRgb, hasCustomAlpha, siteRot, siteForward, siteOrigin, m, v, temp3f);
                            }
                        }
                    }

                    this.tick = frequency;
                }
            }

            this.tick -= 1;
        }
    }

    private void resolveEmissionSite(FormIllusionRenderer.EmissionSite site, Matrix3f siteRot, Vector3f siteForward, Vector3f siteOrigin)
    {
        Matrix4f local = new Matrix4f().translation(site.localX, site.localY, site.localZ);

        if (site.transform != null && !site.transform.isDefault())
        {
            local.mul(site.transform.createMatrix());
        }

        local.transformPosition(siteOrigin.set(0F, 0F, 0F));
        this.rot.transform(siteOrigin);

        siteRot.set(this.rot);

        if (site.transform != null && !site.transform.isDefault())
        {
            Matrix3f extra = new Matrix3f();

            site.transform.createMatrix().get3x3(extra);
            siteRot.mul(extra);
        }

        siteForward.set(0F, 0F, 1F);
        siteRot.transform(siteForward);
    }

    private void spawnParticle(Level world, ParticleOptions effect, String path, float velocity, float colorR, float colorG, float colorB, float colorA, mchorse.bbs_mod.utils.colors.Color color1, mchorse.bbs_mod.utils.colors.Color color2, int colorMode, boolean hasCustomRgb, boolean hasCustomAlpha, Matrix3f siteRot, Vector3f siteForward, Vector3f siteOrigin, Matrix3f m, Vector3f v, Vector3f temp3f)
    {
        float velocityX = siteForward.x * velocity;
        float velocityY = siteForward.y * velocity;
        float velocityZ = siteForward.z * velocity;
        float sh = MathUtils.toRad(this.form.scatteringYaw.get()) * (float) (Math.random() - 0.5D);
        float sv = MathUtils.toRad(this.form.scatteringPitch.get()) * (float) (Math.random() - 0.5D);

        m.identity()
            .rotateY(sh)
            .rotateX(sv)
            .transform(v.set(velocityX, velocityY, velocityZ));

        float pR = colorR;
        float pG = colorG;
        float pB = colorB;
        float pA = colorA;

        if (colorMode == 2 && color1 != null && color2 != null)
        {
            float factor = (float) Math.random();

            pR = Lerps.lerp(color1.r, color2.r, factor);
            pG = Lerps.lerp(color1.g, color2.g, factor);
            pB = Lerps.lerp(color1.b, color2.b, factor);
            pA = Lerps.lerp(color1.a, color2.a, factor);
        }

        if (pR >= 0F)
        {
            if (path.equals("note"))
            {
                int ir = (int) Math.min(255F, Math.max(0F, pR * 255F));
                int ig = (int) Math.min(255F, Math.max(0F, pG * 255F));
                int ib = (int) Math.min(255F, Math.max(0F, pB * 255F));
                float[] hsb = java.awt.Color.RGBtoHSB(ir, ig, ib, null);

                v.x = hsb[0];
                v.y = 0F;
                v.z = 0F;
            }
            else if (path.contains("effect") || path.equals("witch"))
            {
                v.x = pR;
                v.y = pG;
                v.z = pB;
            }
        }

        temp3f.set(
            (Math.random() * 2F - 1F) * this.form.offsetX.get(),
            (Math.random() * 2F - 1F) * this.form.offsetY.get(),
            (Math.random() * 2F - 1F) * this.form.offsetZ.get()
        );

        if (this.form.local.get())
        {
            siteRot.transform(temp3f);
        }

        double x = this.pos.x + siteOrigin.x + temp3f.x;
        double y = this.pos.y + siteOrigin.y + temp3f.y;
        double z = this.pos.z + siteOrigin.z + temp3f.z;

        Minecraft mc = Minecraft.getInstance();
        Particle particleObj = (mc.level != null && mc.particleEngine != null) ? mc.particleEngine.createParticle(effect, x, y, z, v.x, v.y, v.z) : null;

        if (particleObj instanceof SingleQuadParticle bbp)
        {
            if (hasCustomRgb && pR >= 0F)
            {
                bbp.setColor(pR, pG, pB);
            }

            if (hasCustomAlpha && pA >= 0F)
            {
                bbp.setAlpha(pA);
            }

            if (colorMode == 1 && color1 != null && color2 != null)
            {
                this.trackedParticles.add(new TrackedParticle(bbp, color1, color2));
            }
        }
        else if (particleObj == null && world instanceof ClientLevel clientWorld)
        {
            clientWorld.addAlwaysVisibleParticle(effect, x, y, z, v.x, v.y, v.z);
        }
    }
}
