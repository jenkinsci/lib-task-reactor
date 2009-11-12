package org.jvnet.hudson.reactor;

import java.util.concurrent.Executor;

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
    void onTaskStarted(Task t);

    /**
     * Notifies that the execution of the task is about to finish.
     *
     * This happens on the same thread that called {@link #onTaskStarted(Task)}.
     */
    void onTaskCompleted(Task t);

    /**
     * Notifies that the execution of the task have failed with an exception.
     *
     * @param err
     *      Either {@link Error} or {@link Exception}.
     */
    void onTaskFailed(Task t, Throwable err);

    /**
     * Indicates that the following milestone was attained.
     */
    void onAttained(Milestone milestone);

    public static final ReactorListener NOOP = new ReactorListener() {
        public void onTaskStarted(Task t) {
        }
        public void onTaskCompleted(Task t) {
        }
        public void onTaskFailed(Task t, Throwable err) {
        }
        public void onAttained(Milestone milestone) {
        }
    };
}
