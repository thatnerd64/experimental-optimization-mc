package net.minecraftforge.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ASMEventHandler implements IEventListener {
    private final Object target;
    private final Method method;
    private final boolean isStatic;
    private final ForgeSubscribe subInfo;
    private final String readable;

    public ASMEventHandler(Object target, Method method) throws Exception {
        this.target = target;
        this.method = method;
        this.isStatic = Modifier.isStatic(method.getModifiers());
        this.subInfo = method.getAnnotation(ForgeSubscribe.class);
        this.readable = "ASM: " + target + " " + method.getName();
        try {
            method.setAccessible(true);
        } catch (RuntimeException e) {
        }
    }

    @Override
    public void invoke(Event event) {
        if (event.isCancelable() && event.isCanceled() && (subInfo == null || !subInfo.receiveCanceled())) {
            return;
        }
        try {
            method.invoke(isStatic ? null : target, event);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public EventPriority getPriority() {
        return subInfo != null ? subInfo.priority() : EventPriority.NORMAL;
    }

    public Class<?> createWrapper(Method callback) {
        throw new UnsupportedOperationException("handlers are called reflectively");
    }

    @Override
    public String toString() {
        return readable;
    }
}
