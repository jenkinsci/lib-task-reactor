package org.jvnet.hudson.reactor;

import org.jvnet.hudson.annotation_indexer.Index;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

/**
 * Discovers initialization tasks from {@link Initializer}.
 *
 * @author Kohsuke Kawaguchi
 */
public class InitializerFinder extends TaskBuilder {
    private final ClassLoader cl;

    public InitializerFinder(ClassLoader cl) {
        this.cl = cl;
    }

    public InitializerFinder() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public void discoverTasks(Session session, Collection<Task> result) throws IOException {
        for ( final Method e : Index.list(Initializer.class,cl,Method.class)) {
            if (!Modifier.isStatic(e.getModifiers()))
                throw new IOException(e+" is not a static method");

            final Initializer i = e.getAnnotation(Initializer.class);
            if (i==null)        continue; // stale index?

            result.add(new Task() {
                public Collection<?> requires() {
                    return Arrays.asList(i.requires());
                }

                public Collection<?> attains() {
                    return Arrays.asList(i.attains());
                }

                public String getDisplayName() {
                    return getDisplayNameOf(e,i);
                }

                public void run() {
                    invoke(e,i);
                }

                public String toString() {
                    return e.toString();
                }
            });
        }
    }

    /**
     * Obtains the display name of the given initialization task
     */
    protected String getDisplayNameOf(Method e, Initializer i) {
        String s = i.displayName();
        if (s.length()==0)  return e.getName();
        return s;
    }

    /**
     * Invokes the given initialization method.
     */
    protected void invoke(Method e, Initializer i) {
        try {
            e.invoke(null,new Object[0]);
        } catch (IllegalAccessException x) {
            throw (Error)new IllegalAccessError().initCause(x);
        } catch (InvocationTargetException x) {
            throw new Error(x);
        }
    }
}
