package mchorse.bbs_mod.particles.components.motion;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.molang.MolangException;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.particles.ParticleUtils;
import mchorse.bbs_mod.particles.components.IComponentParticleInitialize;
import mchorse.bbs_mod.particles.components.IComponentParticleUpdate;
import mchorse.bbs_mod.particles.components.ParticleComponentBase;
import mchorse.bbs_mod.particles.emitter.Particle;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import org.joml.Vector3f;

public class ParticleComponentMotionParametric extends ParticleComponentMotion implements IComponentParticleInitialize, IComponentParticleUpdate
{
    public MolangExpression[] position = {MolangParser.ZERO, MolangParser.ZERO, MolangParser.ZERO};
    public MolangExpression rotation = MolangParser.ZERO;

    @Override
    protected void toData(MapType data)
    {
        data.put("relative_position", ParticleUtils.vectorToList(this.position));

        if (!MolangExpression.isZero(this.rotation))
        {
            data.put("rotation", this.rotation.toData());
        }
    }

    @Override
    public ParticleComponentBase fromData(BaseType data, MolangParser parser) throws MolangException
    {
        if (!data.isMap())
        {
            return super.fromData(data, parser);
        }

        MapType map = data.asMap();

        if (map.has("relative_position") && map.get("relative_position").isList())
        {
            ParticleUtils.vectorFromList(map.getList("relative_position"), this.position, parser);
        }

        if (map.has("rotation"))
        {
            this.rotation = parser.parseDataSilently(map.get("rotation"));
        }

        return super.fromData(map, parser);
    }

    @Override
    public void apply(ParticleEmitter emitter, Particle particle)
    {
        Vector3f position = new Vector3f((float) this.position[0].get(), (float) this.position[1].get(), (float) this.position[2].get());

        particle.manual = true;
        particle.initialPosition.set(particle.position);

        particle.matrix.transform(position);
        particle.position.x = particle.initialPosition.x + position.x;
        particle.position.y = particle.initialPosition.y + position.y;
        particle.position.z = particle.initialPosition.z + position.z;
        particle.rotation = (float) this.rotation.get();
    }

    @Override
    public void update(ParticleEmitter emitter, Particle particle)
    {
        Vector3f position = new Vector3f((float) this.position[0].get(), (float) this.position[1].get(), (float) this.position[2].get());

        particle.matrix.transform(position);
        particle.position.x = particle.initialPosition.x + position.x;
        particle.position.y = particle.initialPosition.y + position.y;
        particle.position.z = particle.initialPosition.z + position.z;
        particle.rotation = (float) this.rotation.get();
    }

    @Override
    public int getSortingIndex()
    {
        return 10;
    }
}