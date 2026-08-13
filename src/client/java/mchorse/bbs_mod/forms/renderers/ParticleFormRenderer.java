package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.World;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ParticleFormRenderer extends FormRenderer<ParticleForm> implements ITickable
{
    public static long lastUpdate = 0L;

    private ParticleEmitter emitter;
    private final Map<Integer, ParticleEmitter> illusionEmitters = new HashMap<>();
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

    public void ensureEmitter(World world, float transition)
    {
        if (this.lastParticleUpdate < lastUpdate)
        {
            this.lastParticleUpdate = lastUpdate;
            this.checked = false;
        }

        if (!this.checked)
        {
            ParticleScheme scheme = BBSModClient.getParticles().load(this.form.effect.get());

            this.illusionEmitters.clear();

            if (scheme != null)
            {
                this.emitter = new ParticleEmitter();
                this.emitter.setScheme(scheme);
                this.emitter.setWorld(world);
            }
            else
            {
                this.emitter = null;
            }

            this.checked = true;
        }

        this.syncIllusionEmitters(world);
        this.applyEmitterRuntimeState();
    }

    private void syncIllusionEmitters(World world)
    {
        if (this.emitter == null)
        {
            this.illusionEmitters.clear();

            return;
        }

        boolean independent = FormIllusionRenderer.shouldUseIndependentParticles(this.form);

        if (!independent)
        {
            this.illusionEmitters.clear();
            this.emitter.spawnRateScale = 1F;

            return;
        }

        List<Integer> keys = FormIllusionRenderer.collectEmissionTrailKeys(this.form);
        float scale = FormIllusionRenderer.shouldDistributeParticles(this.form) && keys.size() > 1
            ? 1F / keys.size()
            : 1F;

        this.emitter.spawnRateScale = scale;

        Iterator<Map.Entry<Integer, ParticleEmitter>> it = this.illusionEmitters.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<Integer, ParticleEmitter> entry = it.next();

            if (!keys.contains(entry.getKey()) || entry.getKey() == 0)
            {
                it.remove();
            }
        }

        ParticleScheme scheme = this.emitter.scheme;

        for (Integer key : keys)
        {
            if (key == null || key == 0)
            {
                continue;
            }

            ParticleEmitter siteEmitter = this.illusionEmitters.get(key);

            if (siteEmitter == null)
            {
                siteEmitter = new ParticleEmitter();
                siteEmitter.setScheme(scheme);
                siteEmitter.setWorld(world);
                this.illusionEmitters.put(key, siteEmitter);
            }
            else
            {
                siteEmitter.setWorld(world);

                if (siteEmitter.scheme != scheme)
                {
                    siteEmitter.setScheme(scheme);
                }
            }

            siteEmitter.spawnRateScale = scale;
        }
    }

    private void applyEmitterRuntimeState()
    {
        if (this.emitter == null || BBSRendering.isIrisShadowPass())
        {
            return;
        }

        boolean paused = this.form.paused.get();

        this.applyPaused(this.emitter, paused);

        for (ParticleEmitter siteEmitter : this.illusionEmitters.values())
        {
            this.applyPaused(siteEmitter, paused);
        }
    }

    private void applyPaused(ParticleEmitter emitter, boolean paused)
    {
        boolean lastPaused = emitter.paused;

        emitter.paused = paused;

        if (lastPaused != emitter.paused && !emitter.paused && emitter.age > 0 && !this.restart)
        {
            this.restart = true;
        }
    }

    private ParticleEmitter emitterForTrail(int trailInstance)
    {
        if (trailInstance == 0 || !FormIllusionRenderer.shouldUseIndependentParticles(this.form))
        {
            return this.emitter;
        }

        ParticleEmitter siteEmitter = this.illusionEmitters.get(trailInstance);

        return siteEmitter != null ? siteEmitter : this.emitter;
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.ensureEmitter(MinecraftClient.getInstance().world, context.getTransition());

        ParticleEmitter emitter = this.emitter;

        if (emitter != null)
        {
            MatrixStack stack = context.batcher.getContext().getMatrices();
            int scale = (y2 - y1) / 2;

            stack.push();
            stack.translate((x2 + x1) / 2, (y2 + y1) / 2, 40);
            MatrixStackUtils.scaleStack(stack, scale, scale, scale);

            this.updateTexture(emitter, context.getTransition());
            emitter.lastGlobal.set(new Vector3f(0, 0, 0));
            emitter.rotation.identity();

            emitter.setGlow(this.form.glowSettings.get(), this.form.glowingColor.get(), 1F);
            emitter.renderUI(stack, context.getTransition());
            emitter.clearGlow();

            stack.pop();
        }
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        this.ensureEmitter(MinecraftClient.getInstance().world, context.transition);

        ParticleEmitter emitter = this.emitterForTrail(context.trailInstance);

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

            this.updateTexture(emitter, context.transition);

            boolean useGameCamera = !context.modelRenderer && context.type != FormRenderType.PREVIEW;

            if (useGameCamera)
            {
                /* For game rendering, use the main camera for emitter properties to ensure
                 * correct yaw/pitch for billboards (avoiding 180 degree flip in Camera wrapper) */
                emitter.setupCameraProperties(MinecraftClient.getInstance().gameRenderer.getCamera());
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

            Matrix4f modelMatrix = new Matrix4f(context.stack.peek().getPositionMatrix());

            Vector3d translation = new Vector3d(modelMatrix.getTranslation(Vectors.TEMP_3F));

            if (!context.modelRenderer)
            {
                translation.add(context.camera.position.x, context.camera.position.y, context.camera.position.z);
            }

            GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();

            context.stack.push();
            context.stack.loadIdentity();

            emitter.lastGlobal.set(translation);
            emitter.rotation.set(modelMatrix);
            emitter.modelRenderer = context.modelRenderer;

            Color glowTint = Colors.COLOR.set(context.color, true);

            emitter.setGlow(this.form.glowSettings.get(), this.form.glowingColor.get(), glowTint.a);

            if (!BBSRendering.isIrisShadowPass())
            {
                boolean shadersEnabled = BBSRendering.isIrisShadersEnabled();
                boolean billboard = shadersEnabled;

                VertexFormat format = billboard ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR_LIGHT;
                Supplier<ShaderProgram> shader = billboard
                    ? this.getShader(context, GameRenderer::getRenderTypeEntityTranslucentProgram, BBSShaders::getPickerBillboardProgram)
                    : this.getShader(context, GameRenderer::getParticleProgram, BBSShaders::getPickerParticlesProgram);

                emitter.render(format, shader, context.stack, context.overlay, context.getTransition());
            }

            emitter.clearGlow();

            context.stack.pop();

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private void updateTexture(ParticleEmitter emitter, float transition)
    {
        if (emitter != null)
        {
            emitter.texture = this.form.texture.get();
        }
    }

    @Override
    public void tick(IEntity entity)
    {
        this.ensureEmitter(entity.getWorld(), 0F);

        if (this.emitter != null)
        {
            /* Rewind emitters if paused and resumed in order to make
             * particle effects with once emitter */
            if (this.restart)
            {
                this.restartEmitter(this.emitter);

                for (ParticleEmitter siteEmitter : this.illusionEmitters.values())
                {
                    this.restartEmitter(siteEmitter);
                }

                this.restart = false;
            }

            this.emitter.setUserVariables(
                this.form.user1.get(),
                this.form.user2.get(),
                this.form.user3.get(),
                this.form.user4.get(),
                this.form.user5.get(),
                this.form.user6.get()
            );
            this.emitter.update();

            if (FormIllusionRenderer.shouldUseIndependentParticles(this.form))
            {
                for (ParticleEmitter siteEmitter : this.illusionEmitters.values())
                {
                    siteEmitter.setUserVariables(
                        this.form.user1.get(),
                        this.form.user2.get(),
                        this.form.user3.get(),
                        this.form.user4.get(),
                        this.form.user5.get(),
                        this.form.user6.get()
                    );
                    siteEmitter.update();
                }
            }
        }
    }

    private void restartEmitter(ParticleEmitter emitter)
    {
        emitter.stop();
        emitter.start();
    }
}
