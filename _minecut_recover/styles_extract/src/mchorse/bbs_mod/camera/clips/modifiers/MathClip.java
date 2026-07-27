package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValueExpression;
import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.math.Variable;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/**
 * Math modifier
 * 
 * Probably the most complex modifier in Aperture. This modifier accepts 
 * a math expression (which supports basic operators, variables and 
 * functions) written by user, and calculates the value based on that 
 * expression.
 * 
 * This modifier provides all essential input variables for math 
 * expressions, such as: position, angle, progress, progress offset from 
 * fixture, current value and more!
 */
public class MathClip extends ComponentClip
{
    public MathBuilder builder = new MathBuilder();

    public Variable varTicks;
    public Variable varOffset;
    public Variable varTransition;
    public Variable varDuration;
    public Variable varProgress;
    public Variable varFactor;
    public Variable varVelocity;
    public Variable varDistance;

    public Variable varValue;

    public Variable varX;
    public Variable varY;
    public Variable varZ;

    public Variable varYaw;
    public Variable varPitch;
    public Variable varRoll;
    public Variable varFov;

    public final ValueExpression expression = new ValueExpression("expression", this.builder);

    public MathClip()
    {
        super();

        this.add(this.expression);

        this.varTicks = this.builder.register("t");
        this.varOffset = this.builder.register("o");
        this.varTransition = this.builder.register("pt");
        this.varDuration = this.builder.register("d");
        this.varProgress = this.builder.register("p");
        this.varFactor = this.builder.register("f");
        this.varVelocity = this.builder.register("v");
        this.varDistance = this.builder.register("dt");

        this.varValue = this.builder.register("value");

        this.varX = this.builder.register("x");
        this.varY = this.builder.register("y");
        this.varZ = this.builder.register("z");

        this.varYaw = this.builder.register("yaw");
        this.varPitch = this.builder.register("pitch");
        this.varRoll = this.builder.register("roll");
        this.varFov = this.builder.register("fov");
    }

    @Override
    public void applyClip(ClipContext context, Position position)
    {
        IExpression expression = this.expression.get();

        if (expression != null)
        {
            int duration = this.duration.get();

            this.varVelocity.set(context.velocity);
            this.varDistance.set(context.distance);

            this.varTicks.set(context.ticks);
            this.varOffset.set(context.relativeTick);
            this.varTransition.set(context.transition);
            this.varDuration.set(duration);
            this.varProgress.set(context.relativeTick + context.transition);
            this.varFactor.set((double) (context.relativeTick + context.transition) / duration);

            this.varX.set(position.point.x);
            this.varY.set(position.point.y);
            this.varZ.set(position.point.z);

            this.varYaw.set(position.angle.yaw);
            this.varPitch.set(position.angle.pitch);
            this.varRoll.set(position.angle.roll);
            this.varFov.set(position.angle.fov);

            if (this.isActive(0))
            {
                this.varValue.set(position.point.x);
                position.point.x = expression.get().doubleValue();
            }

            if (this.isActive(1))
            {
                this.varValue.set(position.point.y);
                position.point.y = expression.get().doubleValue();
            }

            if (this.isActive(2))
            {
                this.varValue.set(position.point.z);
                position.point.z = expression.get().doubleValue();
            }

            if (this.isActive(3))
            {
                this.varValue.set(position.angle.yaw);
                position.angle.yaw = (float) expression.get().doubleValue();
            }

            if (this.isActive(4))
            {
                this.varValue.set(position.angle.pitch);
                position.angle.pitch = (float) expression.get().doubleValue();
            }

            if (this.isActive(5))
            {
                this.varValue.set(position.angle.roll);
                position.angle.roll = (float) expression.get().doubleValue();
            }

            if (this.isActive(6))
            {
                this.varValue.set(position.angle.fov);
                position.angle.fov = (float) expression.get().doubleValue();
            }
        }
    }

    @Override
    public Clip create()
    {
        return new MathClip();
    }
}