package org.jvnet.hudson.reactor;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class TaskBuilder {
    public abstract Iterable<? extends Task> discoverTasks(Reactor reactor) throws IOException;

    /**
     * Creates a {@link TaskBuilder} that always discovers the given set of tasks.
     */
    public static TaskBuilder fromTasks(final Collection<? extends Task> tasks) {
        return new TaskBuilder() {
            public Iterable<? extends Task> discoverTasks(Reactor reactor) throws IOException {
                return tasks;
            }
        };
    }
}
