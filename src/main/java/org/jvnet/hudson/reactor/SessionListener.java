package org.jvnet.hudson.reactor;

import java.util.concurrent.Executor;

/**
 * Receives callback during the {@link Session#execute(Executor, SessionListener)}.
 *
 * The callback happens by using the threads of {@link Executor}, which means these callbacks
 * can occur concurrently. The callee is responsible for synchronization, if that's desired.
 *
 * @author Kohsuke Kawaguchi
 */
public interface SessionListener {
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
     *      Either {@link Error} or {@link RuntimeException}.
     */
    void onTaskFailed(Task t, Throwable err);

    /**
     * Indicates that the following milestone was attained.
     */
    void onAttained(Object milestone);

    public static final SessionListener NOOP = new SessionListener() {
        public void onTaskStarted(Task t) {
        }
        public void onTaskCompleted(Task t) {
        }
        public void onTaskFailed(Task t, Throwable err) {
        }
        public void onAttained(Object milestone) {
        }
    };
}
