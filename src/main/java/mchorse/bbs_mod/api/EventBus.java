package mchorse.bbs_mod.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event bus that registers and dispatches events to addon subscribers.
 */
public class EventBus
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscribers = new HashMap<>();

    /**
     * Registers the given subscriber to receive events.
     *
     * <p>Methods are collected from the whole class hierarchy so addons can keep shared
     * subscriptions in base classes. The most specific declaration of a method wins.</p>
     */
    public void register(Object subscriber)
    {
        Set<String> visited = new HashSet<>();

        for (Class<?> clazz = subscriber.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
        {
            for (Method method : clazz.getDeclaredMethods())
            {
                if (visited.add(this.getSignature(method)))
                {
                    this.subscribe(subscriber, method);
                }
            }
        }
    }

    private String getSignature(Method method)
    {
        StringBuilder builder = new StringBuilder(method.getName());

        for (Class<?> type : method.getParameterTypes())
        {
            builder.append(':').append(type.getName());
        }

        return builder.toString();
    }

    private void subscribe(Object subscriber, Method method)
    {
        if (method.isAnnotationPresent(Subscribe.class))
        {
            if (method.getParameterCount() != 1)
            {
                return;
            }

            method.setAccessible(true);

            this.subscribers
                .computeIfAbsent(method.getParameterTypes()[0], (clazz) -> new CopyOnWriteArrayList<>())
                .add(new Subscription(subscriber, method));
        }
    }

    /**
     * Posts the given event to the event bus.
     *
     * <p>Subscribers of the event's exact class are called first, then those of its superclasses.</p>
     */
    public void post(Object event)
    {
        for (Class<?> clazz = event.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
        {
            this.post(event, this.subscribers.get(clazz));
        }
    }

    private void post(Object event, CopyOnWriteArrayList<Subscription> eventSubscribers)
    {
        if (eventSubscribers == null || eventSubscribers.isEmpty())
        {
            return;
        }

        for (Subscription subscription : eventSubscribers)
        {
            try
            {
                subscription.method.invoke(subscription.target, event);
            }
            catch (Throwable e)
            {
                Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;

                LOGGER.error("Subscriber {}.{}() failed to handle {}!",
                    subscription.target.getClass().getName(),
                    subscription.method.getName(),
                    event.getClass().getSimpleName(),
                    cause);
            }
        }
    }
}
