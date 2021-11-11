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

import java.io.IOException;
import java.util.ArrayList;
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
            @Override
            public Iterable<? extends Task> discoverTasks(Reactor reactor) throws IOException {
                return tasks;
            }
        };
    }

    public static TaskBuilder union(final Iterable<? extends TaskBuilder> builders) {
        return new TaskBuilder() {
            @Override
            public Iterable<? extends Task> discoverTasks(Reactor reactor) throws IOException {
                List<Task> r = new ArrayList<>();
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
    public static final TaskBuilder EMPTY_BUILDER = fromTasks(Collections.emptyList());
}
