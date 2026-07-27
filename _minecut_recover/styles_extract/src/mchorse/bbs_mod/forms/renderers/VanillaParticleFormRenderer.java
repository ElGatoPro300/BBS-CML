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
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_2223;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2388;
import net.minecraft.class_2392;
import net.minecraft.class_2394;
import net.minecraft.class_2396;
import net.minecraft.class_2398;
import net.minecraft.class_2400;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_7225;
import net.minecraft.class_7923;
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
            net.minecraft.class_4184 realCamera = class_310.method_1551().field_1773.method_19418();

            positionMatrix = new Matrix4f().rotation(realCamera.method_23767());
            positionMatrix.mul(context.stack.method_23760().method_23761());

            Vector3f translation = positionMatrix.getTranslation(new Vector3f());

            this.pos.set(
                translation.x + (float) realCamera.method_19326().field_1352,
                translation.y + (float) realCamera.method_19326().field_1351,
                translation.z + (float) realCamera.method_19326().field_1350
            );
        }
        else
        {
            positionMatrix = new Matrix4f(context.stack.method_23760().method_23761());

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
        class_1937 world = entity.getWorld();
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
                class_2396<?> type = class_7923.field_41180.method_10223(settings.particle);
                class_2394 effect = class_2398.field_11240;

                if (type != null)
                {
                    class_7225.class_7874 registries = world.method_30349();

                    if (type instanceof class_2400 simple)
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
                            effect = class_2223.method_9418(new StringReader(full), registries);
                        }
                        catch (Exception e)
                        {
                            /* Manual fallbacks for common complex particles using direct registry lookups */
                            if (!args.isEmpty())
                            {
                                try
                                {
                                    class_2960 id = class_2960.method_12829(args);

                                    if (id != null)
                                    {
                                        /* Try to find as block first */
                                        class_2248 block = class_7923.field_41175.method_10223(id);

                                        if (block != class_2246.field_10124)
                                        {
                                            effect = new class_2388(class_2398.field_11217, block.method_9564());
                                        }
                                        else
                                        {
                                            /* Try to find as item */
                                            class_1792 item = class_7923.field_41178.method_10223(id);

                                            if (item != class_1802.field_8162)
                                            {
                                                effect = new class_2392(class_2398.field_11218, new class_1799(item));
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

                    world.method_8466(effect, true, x, y, z, v.x, v.y, v.z);
                }

                this.tick = frequency;
            }

            this.tick -= 1;
        }
    }
}
