package mchorse.bbs_mod.api;

import java.lang.reflect.Method;

public class Subscription
{
    public final Object target;
    public final Method method;

    public Subscription(Object target, Method method)
    {
        this.target = target;
        this.method = method;
    }
}
