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

public class VanillaParticleFormRenderer extends FormRenderer<VanillaParticleForm> implements ITickable
{
    public static final Link PARTICLE_PREVIEW = new Link("minecraft", "textures/particle/flame.png");

    private Vector3d pos = new Vector3d();
    private Vector3f vel = new Vector3f();
    private Matrix3f rot = new Matrix3f();
    private int tick;

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
        World world = entity.getWorld();
        boolean paused = this.form.paused.get();
        Vector3f temp3f = new Vector3f();

        if (world != null && !paused)
        {
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

                    if (colorR < 0F && this.form != null)
                    {
                        mchorse.bbs_mod.utils.colors.Color formColor = this.form.color.get();

                        if (formColor != null)
                        {
                            colorR = formColor.r;
                            colorG = formColor.g;
                            colorB = formColor.b;
                            colorA = formColor.a;
                        }
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

                            effect = new DustColorTransitionParticleEffect(new Vector3f(colorR, colorG, colorB), new Vector3f(colorR, colorG, colorB), scale);
                            parsedCustom = true;
                        }
                        else if (path.contains("dust"))
                        {
                            float scale = colorA > 0F ? colorA : 1F;

                            effect = new DustParticleEffect(new Vector3f(colorR, colorG, colorB), scale);
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

                        if (colorR >= 0F)
                        {
                            if (path.equals("note"))
                            {
                                int ir = (int) Math.min(255F, Math.max(0F, colorR * 255F));
                                int ig = (int) Math.min(255F, Math.max(0F, colorG * 255F));
                                int ib = (int) Math.min(255F, Math.max(0F, colorB * 255F));
                                float[] hsb = java.awt.Color.RGBtoHSB(ir, ig, ib, null);

                                v.x = hsb[0];
                                v.y = 0F;
                                v.z = 0F;
                            }
                            else if (path.contains("effect") || path.equals("witch"))
                            {
                                v.x = colorR;
                                v.y = colorG;
                                v.z = colorB;
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

                        Particle particleObj = MinecraftClient.getInstance().particleManager.addParticle(effect, x, y, z, v.x, v.y, v.z);

                        if (particleObj != null && colorR >= 0F)
                        {
                            particleObj.setColor(colorR, colorG, colorB);
                            particleObj.setAlpha(colorA);
                        }
                        else if (particleObj == null)
                        {
                            world.addParticle(effect, true, x, y, z, v.x, v.y, v.z);
                        }
                    }

                    this.tick = frequency;
                }
            }

            this.tick -= 1;
        }
    }
}
