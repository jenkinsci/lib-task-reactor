/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jvnet.hudson.reactor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private final Set<Task> tasks = new HashSet<>();
    private final List<Milestone> requiresForNextTask = new ArrayList<>();
    private final List<Milestone> attainsForNextTask = new ArrayList<>();
    private boolean fatalForNextTask = true;
    private Handle last;

    @Override
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
        private final Set<Milestone> requires = new HashSet<>();
        private final Set<Milestone> attains = new HashSet<>();
        private boolean fatal;

        private TaskImpl(String displayName, Executable executable) {
            this.displayName = displayName;
            this.executable = executable;
            attains.add(this);
        }

        @Override
        public Collection<Milestone> requires() {
            return requires;
        }

        @Override
        public Collection<Milestone> attains() {
            return attains;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public boolean failureIsFatal() {
            return fatal;
        }

        @Override
        @SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", justification = "TODO needs triage")
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
        @Override
        public Handle requires(Milestone m) {
            if (m!=null)
                requires.add(m);
            return this;
        }

        @Override
        public Handle requires(Milestone... ms) {
            return requires(Arrays.asList(ms));
        }

        @Override
        public Handle requires(Collection<? extends Milestone> ms) {
            for (Milestone m : ms)
                requires(m);
            return this;
        }

        @Override
        public Handle attains(Milestone m) {
            attains.add(m);
            return this;
        }

        @Override
        public Handle attains(Collection<? extends Milestone> ms) {
            for (Milestone m : ms)
                attains(m);
            return this;
        }

        @Override
        public Task asTask() {
            return this;
        }

        @Override
        public Task notFatal() {
            fatal = false;
            return this;
        }
    }
}
