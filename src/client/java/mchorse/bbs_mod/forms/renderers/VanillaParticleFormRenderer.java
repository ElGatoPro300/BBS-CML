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
import net.minecraft.client.render.Camera;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
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

                    if (settings.particle != null && settings.particle.getPath().contains("effect"))
                    {
                        String args = settings.arguments.trim();
                        boolean parsedEffect = false;

                        if (!args.isEmpty())
                        {
                            try
                            {
                                String[] split = args.split("\\s+");

                                if (split.length >= 3)
                                {
                                    float r = Float.parseFloat(split[0]);
                                    float g = Float.parseFloat(split[1]);
                                    float b = Float.parseFloat(split[2]);

                                    effect = EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, r, g, b);
                                    parsedEffect = true;
                                }
                            }
                            catch (Exception e)
                            {}
                        }

                        if (!parsedEffect && type instanceof SimpleParticleType simple)
                        {
                            effect = simple;
                        }
                    }
                    else if (type instanceof SimpleParticleType simple)
                    {
                        effect = simple;
                    }
                    else if (registries != null)
                    {
                        String full = settings.particle.toString();
                        String args = settings.arguments.trim();

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

                    world.addParticle(effect, true, x, y, z, v.x, v.y, v.z);
                }

                this.tick = frequency;
            }

            this.tick -= 1;
        }
    }
}
