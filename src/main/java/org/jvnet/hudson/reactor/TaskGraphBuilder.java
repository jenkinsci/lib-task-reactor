package org.jvnet.hudson.reactor;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Builder/fluent-API pattern to build up a series of related tasks.
 *
 * @author Kohsuke Kawaguchi
 */
public class TaskGraphBuilder extends TaskBuilder {
    private final Set<Task> tasks = new HashSet<Task>();
    private final List<Milestone> requiresForNextTask = new ArrayList<Milestone>();
    private final List<Milestone> attainsForNextTask = new ArrayList<Milestone>();
    private boolean fatalForNextTask = true;
    private Handle last;

    public Iterable<? extends Task> discoverTasks(Reactor reactor) throws IOException {
        return Collections.unmodifiableSet(tasks);
    }

    /**
     * Adds a new work unit and returns its handle, which can then be used to set up dependencies among them.
     *
     * @param displayName
     *      Display name of the task.
     * @param e
     *      The actual work.
     */
    public Handle add(String displayName, Executable e) {
        TaskImpl t = new TaskImpl(displayName, e);
        tasks.add(t);
        t.requires(requiresForNextTask);
        t.attains(attainsForNextTask);
        t.fatal = fatalForNextTask;

        // reset
        requiresForNextTask.clear();
        attainsForNextTask.clear();
        fatalForNextTask = true;

        last = t;
        return t;
    }

    /**
     * Indicates that the task to be added requires the completion of the last added task.
     */
    public TaskGraphBuilder followedBy() {
        return requires(last);
    }

    /**
     * Given milestones will be set as pre-requisites for the next task to be added.
     */
    public TaskGraphBuilder requires(Milestone... milestones) {
        requiresForNextTask.addAll(Arrays.asList(milestones));
        return this;
    }

    /**
     * Given milestones will be set as achievements for the next task. 
     */
    public TaskGraphBuilder attains(Milestone... milestones) {
        attainsForNextTask.addAll(Arrays.asList(milestones));
        return this;
    }

    public TaskGraphBuilder notFatal() {
        fatalForNextTask = false;
        return this;
    }

    /**
     * Handle to the task. Call methods on this interface to set up dependencies among tasks.
     *
     * <p>
     * This interface is the fluent interface pattern, and all the methods return {@code this} to enable chaining.
     */
    public interface Handle extends Milestone {
        /**
         * Adds a pre-requisite to this task.
         */
        Handle requires(Milestone m);
        /**
         * Adds pre-requisites to this task.
         */
        Handle requires(Milestone... m);
        Handle requires(Collection<? extends Milestone> m);

        /**
         * Designates that the execution of this task contributes to the given milestone.
         */
        Handle attains(Milestone m);
        Handle attains(Collection<? extends Milestone> m);

        /**
         * Returns the task that this handle represents.
         */
        Task asTask();

        /**
         * Marks this task as non-fatal.
         *
         * See {@link Task#failureIsFatal()}.
         */
        Task notFatal();
    }

    private static final class TaskImpl implements Task, Milestone, Handle {
        private final String displayName;
        private final Executable executable;
        private final Set<Milestone> requires = new HashSet<Milestone>();
        private final Set<Milestone> attains = new HashSet<Milestone>();
        private boolean fatal;

        private TaskImpl(String displayName, Executable executable) {
            this.displayName = displayName;
            this.executable = executable;
            attains.add(this);
        }

        public Collection<Milestone> requires() {
            return requires;
        }

        public Collection<Milestone> attains() {
            return attains;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean failureIsFatal() {
            return fatal;
        }

        public void run(Reactor reactor) throws Exception {
            executable.run(reactor);
        }

        @Override
        public String toString() {
            return displayName;
        }
        
    //
    // mutators
    //
        public Handle requires(Milestone m) {
            if (m!=null)
                requires.add(m);
            return this;
        }

        public Handle requires(Milestone... ms) {
            return requires(Arrays.asList(ms));
        }

        public Handle requires(Collection<? extends Milestone> ms) {
            for (Milestone m : ms)
                requires(m);
            return this;
        }

        public Handle attains(Milestone m) {
            attains.add(m);
            return this;
        }

        public Handle attains(Collection<? extends Milestone> ms) {
            for (Milestone m : ms)
                attains(m);
            return this;
        }

        public Task asTask() {
            return this;
        }

        public Task notFatal() {
            fatal = false;
            return this;
        }
    }
}
