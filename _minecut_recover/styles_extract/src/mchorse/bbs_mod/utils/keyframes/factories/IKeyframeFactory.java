package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.Objects;

public interface IKeyframeFactory <T>
{
    public T fromData(BaseType data);

    public BaseType toData(T value);

    public T createEmpty();

    public default boolean compare(Object a, Object b)
    {
        return Objects.equals(a, b);
    }

    public T copy(T value);

    public default T interpolate(Keyframe<T> preA, Keyframe<T> a, Keyframe<T> b, Keyframe<T> postB, IInterp interpolation, float x)
    {
        return this.interpolate(preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
    }

    public T interpolate(T preA, T a, T b, T postB, IInterp interpolation, float x);

    public default double getY(T value)
    {
        return 0D;
    }

    public default Object yToValue(double y)
    {
        return y;
    }
}