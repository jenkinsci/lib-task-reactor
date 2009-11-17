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
     *      Either {@link Error} or {@link Exception}, indicating the cause of the failure.
     * @param fatal
     *      If true, this problem is {@linkplain Task#failureIsFatal() fatal}, and the reactor
     *      is going to terminate. If false, the reactor will continue executing after this failure.
     */
    void onTaskFailed(Task t, Throwable err, boolean fatal);

    /**
     * Indicates that the following milestone was attained.
     */
    void onAttained(Milestone milestone);

    public static final ReactorListener NOOP = new ReactorListener() {
        public void onTaskStarted(Task t) {
        }
        public void onTaskCompleted(Task t) {
        }
        public void onTaskFailed(Task t, Throwable err, boolean fatal) {
        }
        public void onAttained(Milestone milestone) {
        }
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
