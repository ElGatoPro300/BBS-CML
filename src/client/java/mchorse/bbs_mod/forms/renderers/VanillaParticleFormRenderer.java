package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

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
        public Particle particle;
        public mchorse.bbs_mod.utils.colors.Color startColor;
        public mchorse.bbs_mod.utils.colors.Color endColor;

        public TrackedParticle(Particle particle, mchorse.bbs_mod.utils.colors.Color startColor, mchorse.bbs_mod.utils.colors.Color endColor)
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
    protected void render3D(FormRenderingContext context)
    {
        super.render3D(context);

        Matrix4f positionMatrix;

        if (context.type == FormRenderType.PREVIEW)
        {
            net.minecraft.client.render.Camera realCamera = MinecraftClient.getInstance().gameRenderer.getCamera();

            positionMatrix = new Matrix4f().rotation(realCamera.getRotation());
            positionMatrix.mul(context.stack.peek().getPositionMatrix());

            Vector3f translation = positionMatrix.getTranslation(new Vector3f());

            this.pos.set(
                translation.x + (float) realCamera.getPos().x,
                translation.y + (float) realCamera.getPos().y,
                translation.z + (float) realCamera.getPos().z
            );
        }
        else
        {
            positionMatrix = new Matrix4f(context.stack.peek().getPositionMatrix());

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
        World world = entity == null ? null : entity.getWorld();

        if (world == null)
        {
            world = MinecraftClient.getInstance().world;
        }

        boolean paused = this.form.paused.get();
        Vector3f temp3f = new Vector3f();

        if (world != null && MinecraftClient.getInstance().world != null && !paused)
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

                    int maxAge = tracked.particle.maxAge;
                    int age = tracked.particle.age;

                    float progress = maxAge > 0 ? (float) age / (float) maxAge : 1F;
                    progress = MathUtils.clamp(progress, 0F, 1F);

                    float r = Lerps.lerp(tracked.startColor.r, tracked.endColor.r, progress);
                    float g = Lerps.lerp(tracked.startColor.g, tracked.endColor.g, progress);
                    float b = Lerps.lerp(tracked.startColor.b, tracked.endColor.b, progress);
                    float a = Lerps.lerp(tracked.startColor.a, tracked.endColor.a, progress);

                    tracked.particle.setColor(r, g, b);
                    tracked.particle.setAlpha(a);
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
                ParticleType<?> type = Registries.PARTICLE_TYPE.get(settings.particle);
                ParticleEffect effect = ParticleTypes.FLAME;

                if (type != null)
                {
                    RegistryWrapper.WrapperLookup registries = world.getRegistryManager();
                    String path = settings.particle != null ? settings.particle.getPath() : "";
                    String args = settings.arguments.trim();

                    float colorR = -1F;
                    float colorG = -1F;
                    float colorB = -1F;
                    float colorA = 1F;

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

                    if (colorR < 0F && color1 != null)
                    {
                        colorR = color1.r;
                        colorG = color1.g;
                        colorB = color1.b;
                        colorA = color1.a;
                    }

                    boolean parsedCustom = false;

                    if (colorR >= 0F)
                    {
                        if (path.contains("effect"))
                        {
                            effect = EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, colorR, colorG, colorB);
                            parsedCustom = true;
                        }
                        else if (path.equals("dust_color_transition"))
                        {
                            float scale = colorA > 0F ? colorA : 1F;
                            int rgb = new mchorse.bbs_mod.utils.colors.Color(colorR, colorG, colorB).getRGBColor();

                            effect = new DustColorTransitionParticleEffect(rgb, rgb, scale);
                            parsedCustom = true;
                        }
                        else if (path.contains("dust"))
                        {
                            float scale = colorA > 0F ? colorA : 1F;
                            int rgb = new mchorse.bbs_mod.utils.colors.Color(colorR, colorG, colorB).getRGBColor();

                            effect = new DustParticleEffect(rgb, scale);
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
                                effect = ParticleEffectArgumentType.readParameters(new StringReader(full), registries);
                            }
                            catch (Exception e)
                            {
                                /* Manual fallbacks for common complex particles using direct registry lookups */
                                if (!args.isEmpty())
                                {
                                    try
                                    {
                                        Identifier id = Identifier.tryParse(args);

                                        if (id != null)
                                        {
                                            /* Try to find as block first */
                                            Block block = Registries.BLOCK.get(id);

                                            if (block != Blocks.AIR)
                                            {
                                                effect = new BlockStateParticleEffect(ParticleTypes.BLOCK, block.getDefaultState());
                                            }
                                            else
                                            {
                                                /* Try to find as item */
                                                Item item = Registries.ITEM.get(id);

                                                if (item != Items.AIR)
                                                {
                                                    effect = new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(item));
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

                    for (int i = 0; i < count; i++)
                    {
                        float velocityX = this.vel.x * velocity;
                        float velocityY = this.vel.y * velocity;
                        float velocityZ = this.vel.z * velocity;
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
                            this.rot.transform(temp3f);
                        }

                        double x = this.pos.x + temp3f.x;
                        double y = this.pos.y + temp3f.y;
                        double z = this.pos.z + temp3f.z;

                        MinecraftClient mc = MinecraftClient.getInstance();
                        Particle particleObj = (mc.world != null && mc.particleManager != null) ? mc.particleManager.addParticle(effect, x, y, z, v.x, v.y, v.z) : null;

                        if (particleObj != null && pR >= 0F)
                        {
                            particleObj.setColor(pR, pG, pB);
                            particleObj.setAlpha(pA);

                            if (colorMode == 1 && color1 != null && color2 != null)
                            {
                                this.trackedParticles.add(new TrackedParticle(particleObj, color1, color2));
                            }
                        }
                        else if (particleObj == null && world != null)
                        {
                            world.addImportantParticle(effect, x, y, z, v.x, v.y, v.z);
                        }
                    }

                    this.tick = frequency;
                }
            }

            this.tick -= 1;
        }
    }
}
