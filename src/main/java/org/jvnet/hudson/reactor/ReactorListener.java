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

import java.util.concurrent.Executor;
import java.util.Collection;

/**
 * Receives callback during the {@link Reactor#execute(Executor, ReactorListener)}.
 *
 * The callback happens by using the threads of {@link Executor}, which means these callbacks
 * can occur concurrently. The callee is responsible for synchronization, if that's desired.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ReactorListener {
    /**
     * Notifies that the execution of the task is about to start.
     */
    default void onTaskStarted(Task t) {
        // Do nothing by default
    }

    /**
     * Notifies that the execution of the task is about to finish.
     *
     * This happens on the same thread that called {@link #onTaskStarted(Task)}.
     */
    default void onTaskCompleted(Task t) {
        // Do nothing by default
    }

    /**
     * Notifies that the execution of the task have failed with an exception.
     *
     * @param err
     *      Either {@link Error} or {@link Exception}, indicating the cause of the failure.
     * @param fatal
     *      If true, this problem is {@linkplain Task#failureIsFatal() fatal}, and the reactor
     *      is going to terminate. If false, the reactor will continue executing after this failure.
     */
    default void onTaskFailed(Task t, Throwable err, boolean fatal)  {
        // Do nothing by default
    }

    /**
     * Indicates that the following milestone was attained.
     */
    default void onAttained(Milestone milestone)  {
        // Do nothing by default
    }

    public static final ReactorListener NOOP = new ReactorListener() {
        // Default implementation for all handlers
    };

    /**
     * Bundles multiple listeners into one.
     */
    public static class Aggregator implements ReactorListener {
        private final Collection<ReactorListener> listeners;

        public Aggregator(Collection<ReactorListener> listeners) {
            this.listeners = listeners;
        }

        public void onTaskStarted(Task t) {
            for (ReactorListener listener : listeners)
                listener.onTaskStarted(t);
        }

        public void onTaskCompleted(Task t) {
            for (ReactorListener listener : listeners)
                listener.onTaskCompleted(t);
        }

        public void onTaskFailed(Task t, Throwable err, boolean fatal) {
            for (ReactorListener listener : listeners)
                listener.onTaskFailed(t,err,fatal);
        }

        public void onAttained(Milestone milestone) {
            for (ReactorListener listener : listeners)
                listener.onAttained(milestone);
        }
    }
}
