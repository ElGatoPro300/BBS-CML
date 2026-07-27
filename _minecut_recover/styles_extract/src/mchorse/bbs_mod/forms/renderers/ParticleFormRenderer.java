package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.class_1937;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_5944;
import net.minecraft.class_757;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Supplier;

public class ParticleFormRenderer extends FormRenderer<ParticleForm> implements ITickable
{
    public static long lastUpdate = 0L;

    private ParticleEmitter emitter;
    private boolean checked;
    private boolean restart;
    private long lastParticleUpdate = lastUpdate;

    public ParticleFormRenderer(ParticleForm form)
    {
        super(form);
    }

    public ParticleEmitter getEmitter()
    {
        return this.emitter;
    }

    public void ensureEmitter(class_1937 world, float transition)
    {
        if (this.lastParticleUpdate < lastUpdate)
        {
            this.lastParticleUpdate = lastUpdate;
            this.checked = false;
        }

        if (!this.checked)
        {
            ParticleScheme scheme = BBSModClient.getParticles().load(this.form.effect.get());

            if (scheme != null)
            {
                this.emitter = new ParticleEmitter();
                this.emitter.setScheme(scheme);
                this.emitter.setWorld(world);
            }

            this.checked = true;
        }

        if (this.emitter != null && !BBSRendering.isIrisShadowPass())
        {
            boolean lastPaused = this.emitter.paused;

            this.emitter.paused = this.form.paused.get();

            if (lastPaused != this.emitter.paused && !this.emitter.paused && this.emitter.age > 0 && !this.restart)
            {
                this.restart = true;
            }
        }
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.ensureEmitter(class_310.method_1551().field_1687, context.getTransition());

        ParticleEmitter emitter = this.emitter;

        if (emitter != null)
        {
            class_4587 stack = context.batcher.getContext().method_51448();
            int scale = (y2 - y1) / 2;
            boolean wasPaused = emitter.paused;

            if (!FormUtilsClient.isUIPreviewAnimating())
            {
                emitter.paused = true;
            }

            stack.method_22903();
            stack.method_46416((x2 + x1) / 2, (y2 + y1) / 2, 40);
            MatrixStackUtils.scaleStack(stack, scale, scale, scale);

            this.updateTexture(context.getTransition());
            emitter.lastGlobal.set(new Vector3f(0, 0, 0));
            emitter.rotation.identity();

            emitter.setGlow(this.form.glowSettings.get(), this.form.glowingColor.get(), 1F);
            emitter.renderUI(stack, context.getTransition());
            emitter.clearGlow();

            stack.method_22909();
            emitter.paused = wasPaused;
        }
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        this.ensureEmitter(class_310.method_1551().field_1687, context.transition);

        ParticleEmitter emitter = this.emitter;

        if (emitter != null)
        {
            emitter.setUserVariables(
                this.form.user1.get(),
                this.form.user2.get(),
                this.form.user3.get(),
                this.form.user4.get(),
                this.form.user5.get(),
                this.form.user6.get()
            );

            this.updateTexture(context.getTransition());

            boolean useGameCamera = !context.modelRenderer && context.type != FormRenderType.PREVIEW;
            
            if (useGameCamera)
            {
                /* For game rendering, use the main camera for emitter properties to ensure
                 * correct yaw/pitch for billboards (avoiding 180 degree flip in Camera wrapper) */
                emitter.setupCameraProperties(class_310.method_1551().field_1773.method_19418());
            }
            else
            {
                if (context.modelRenderer)
                {
                    float originalPitch = context.camera.rotation.x;
                    float originalYaw = context.camera.rotation.y;
                    double originalX = context.camera.position.x;
                    double originalY = context.camera.position.y;
                    double originalZ = context.camera.position.z;

                    context.camera.rotation.set(0, 0, 0);
                    context.camera.position.set(0, 0, 0);

                    emitter.setupCameraProperties(context.camera);

                    context.camera.rotation.x = originalPitch;
                    context.camera.rotation.y = originalYaw;
                    context.camera.position.set(originalX, originalY, originalZ);
                }
                else
                {
                    emitter.setupCameraProperties(context.camera);
                }
            }

            Matrix4f modelMatrix = new Matrix4f(context.stack.method_23760().method_23761());

            Vector3d translation = new Vector3d(modelMatrix.getTranslation(Vectors.TEMP_3F));
            
            if (!context.modelRenderer)
            {
                translation.add(context.camera.position.x, context.camera.position.y, context.camera.position.z);
            }

            class_757 gameRenderer = class_310.method_1551().field_1773;

            gameRenderer.method_22974().method_3316();
            gameRenderer.method_22975().method_23209();

            context.stack.method_22903();
            context.stack.method_34426();

            emitter.lastGlobal.set(translation);
            emitter.rotation.set(modelMatrix);
            emitter.modelRenderer = context.modelRenderer;

            Color glowTint = Colors.COLOR.set(context.color, true);

            emitter.setGlow(this.form.glowSettings.get(), this.form.glowingColor.get(), glowTint.a);
            
            if (!BBSRendering.isIrisShadowPass())
            {
                boolean shadersEnabled = BBSRendering.isIrisShadersEnabled();
                boolean billboard = shadersEnabled;

                class_293 format = billboard ? class_290.field_1580 : class_290.field_1584;
                Supplier<class_5944> shader = billboard
                    ? this.getShader(context, class_757::method_34508, BBSShaders::getPickerBillboardProgram)
                    : this.getShader(context, class_757::method_34546, BBSShaders::getPickerParticlesProgram);

                emitter.render(format, shader, context.stack, context.overlay, context.getTransition());
            }

            emitter.clearGlow();

            context.stack.method_22909();

            gameRenderer.method_22974().method_3315();
            gameRenderer.method_22975().method_23213();
        }
    }

    private void updateTexture(float transition)
    {
        if (this.emitter != null)
        {
            this.emitter.texture = this.form.texture.get();
        }
    }

    @Override
    public void tick(IEntity entity)
    {
        this.ensureEmitter(entity.getWorld(), 0F);

        if (this.emitter != null)
        {
            /* Rewind the emitter if it was paused and resumed in order to make
             * particle effects with once emitter */
            if (this.restart)
            {
                this.emitter.stop();
                this.emitter.start();

                this.restart = false;
            }

            this.emitter.update();
        }
    }
}