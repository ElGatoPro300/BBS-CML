package mchorse.bbs_mod.particles.components.motion;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.Operation;
import mchorse.bbs_mod.math.molang.MolangException;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.particles.components.IComponentParticleUpdate;
import mchorse.bbs_mod.particles.components.ParticleComponentBase;
import mchorse.bbs_mod.particles.emitter.Particle;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import net.minecraft.class_1297;
import net.minecraft.class_238;
import net.minecraft.class_243;
import org.joml.Vector3d;

import java.util.Collections;

public class ParticleComponentMotionCollision extends ParticleComponentBase implements IComponentParticleUpdate
{
    public MolangExpression enabled = MolangParser.ONE;
    public float collisionDrag = 0;
    public float bounciness = 1;
    public float radius = 0.01F;
    public boolean expireOnImpact;

    /* Runtime options */
    private Vector3d previous = new Vector3d();
    private Vector3d current = new Vector3d();

    @Override
    public BaseType toData()
    {
        MapType object = new MapType();

        if (MolangExpression.isZero(this.enabled))
        {
            return object;
        }

        if (!MolangExpression.isOne(this.enabled)) object.put("enabled", this.enabled.toData());
        if (this.collisionDrag != 0) object.putFloat("collision_drag", this.collisionDrag);
        if (this.bounciness != 1) object.putFloat("coefficient_of_restitution", this.bounciness);
        if (this.radius != 0.01F) object.putFloat("collision_radius", this.radius);
        if (this.expireOnImpact) object.putBool("expire_on_contact", true);

        return object;
    }

    @Override
    public ParticleComponentBase fromData(BaseType data, MolangParser parser) throws MolangException
    {
        if (!data.isMap())
        {
            return super.fromData(data, parser);
        }

        MapType map = data.asMap();

        if (map.has("enabled")) this.enabled = parser.parseDataSilently(map.get("enabled"));
        if (map.has("collision_drag")) this.collisionDrag = map.getFloat("collision_drag");
        if (map.has("coefficient_of_restitution")) this.bounciness = map.getFloat("coefficient_of_restitution");
        if (map.has("collision_radius")) this.radius = map.getFloat("collision_radius");
        if (map.has("expire_on_contact")) this.expireOnImpact = map.getBool("expire_on_contact");

        return super.fromData(map, parser);
    }

    @Override
    public void update(ParticleEmitter emitter, Particle particle)
    {
        if (emitter.world == null)
        {
            return;
        }

        if (!particle.manualPosition && Operation.equals(this.enabled.get(), 1))
        {
            float r = this.radius;

            this.previous.set(particle.getGlobalPosition(emitter, particle.prevPosition));
            this.current.set(particle.getGlobalPosition(emitter));

            Vector3d prev = this.previous;
            Vector3d now = this.current;

            double x = now.x - prev.x;
            double y = now.y - prev.y;
            double z = now.z - prev.z;
            boolean veryBig = Math.abs(x) > 10 || Math.abs(y) > 10 || Math.abs(z) > 10;

            if (veryBig)
            {
                return;
            }

            class_238 box = new class_238(prev.x - r, prev.y - r, prev.z - r, prev.x + r, prev.y + r, prev.z + r);
            class_243 vec = class_1297.method_20736(null, new class_243(x, y, z), box, emitter.world, Collections.emptyList());

            if (vec.field_1352 != x || vec.field_1351 != y || vec.field_1350 != z)
            {
                if (this.expireOnImpact)
                {
                    particle.setDead();

                    return;
                }

                if (particle.relativePosition)
                {
                    particle.relativePosition = false;
                    particle.prevPosition.set(prev);
                }

                now.set(prev).add(vec.field_1352, vec.field_1351, vec.field_1350);

                if (vec.field_1351 != y)
                {
                    particle.accelerationFactor.y *= -this.bounciness;
                }

                if (vec.field_1352 != x)
                {
                    particle.accelerationFactor.x *= -this.bounciness;
                }

                if (vec.field_1350 != z)
                {
                    particle.accelerationFactor.z *= -this.bounciness;
                }

                particle.position.set(now);
                particle.dragFactor += this.collisionDrag;
            }
        }
    }

    @Override
    public int getSortingIndex()
    {
        return 50;
    }
}