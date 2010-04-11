package org.jvnet.hudson.reactor;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Contributes {@link Task}s to a {@link Reactor} execution.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class TaskBuilder {
    /**
     * Returns all the tasks that this builder contributes to.
     */
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

    public static TaskBuilder union(final Iterable<? extends TaskBuilder> builders) {
        return new TaskBuilder() {
            public Iterable<? extends Task> discoverTasks(Reactor reactor) throws IOException {
                List<Task> r = new ArrayList<Task>();
                for (TaskBuilder b : builders)
                    for (Task t : b.discoverTasks(reactor))
                        r.add(t);
                return r;
            }
        };
    }

    public static TaskBuilder union(TaskBuilder... builders) {
        return union(asList(builders));
    }

    /**
     * {@link TaskBuilder} that contributes no task.
     */
    public static final TaskBuilder EMPTY_BUILDER = fromTasks(Collections.<Task>emptyList());
}
