package org.jvnet.hudson.reactor;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Executes a set of {@link Task}s that dependend on each other.
 *
 * <p>
 * As a {@link Set}, this object represents a read-only view of all {@link Task}s. 
 *
 * @author Kohsuke Kawaguchi
 */
public class Session extends AbstractSet<Task> {
    private final Set<Task> tasks = new HashSet<Task>();

    public Session(Collection<? extends TaskBuilder> builders) throws IOException {
        for (TaskBuilder b : builders)
            b.discoverTasks(this,tasks);
    }

    public Session(TaskBuilder... builders) throws IOException {
        this(Arrays.asList(builders));
    }

    public Iterator<Task> iterator() {
        return tasks.iterator();
    }

    public int size() {
        return tasks.size();
    }

    public void execute(Executor e) throws InterruptedException, ReactorException {
        execute(e,SessionListener.NOOP);
    }

    /**
     * Executes this initialization session with the given executor.
     *
     * @param e
     *      Used for executing {@link Task}s.
     * @param listener
     *      Receives callbacks during the execution.
     *
     * @throws InterruptedException
     *      if this thread is interrupted while waiting for the execution of tasks to complete.
     * @throws ReactorException
     *      if one of the tasks failed by throwing an exception. The caller is responsible for canceling
     *      existing {@link Task}s that are in progress in {@link Executor}, if that's desired. 
     */
    public void execute(final Executor e, final SessionListener listener) throws InterruptedException, ReactorException {
        // make sure that scheduling of the tasks happens sequentially to avoid race condition
        final Object schedulingLock = new Object();

        // number of tasks pending execution
        final int[] pending = new int[1];
        // RuntimeException or Error that indicates a fatal failure in a task
        final TunnelException[] fatal = new TunnelException[1];

        /**
         * A node in DAG.
         */
        final class Node implements Runnable {
            /**
             * All of them have to run before this task can be executed.
             */
            private final Set<Node> prerequisites = new HashSet<Node>();

            /**
             * What to run
             */
            private final Runnable task;

            /**
             * These nodes have this node in {@link #prerequisites}.
             */
            private final Set<Node> downstream = new HashSet<Node>();

            private boolean submitted;
            private boolean done;

            private Node(Runnable task) {
                this.task = task;
            }

            private void addPrerequisite(Node n) {
                prerequisites.add(n);
                n.downstream.add(this);
            }

            /**
             * Can this node be executed?
             */
            private boolean canRun() {
                if (submitted)  return false;
                for (Node n : prerequisites)
                    if (!n.done)        return false;
                return true;
            }

            public void run() {
                try {
                    task.run();
                } catch(TunnelException t) {
                    fatal[0] = t;
                } finally {
                    done = true;
                }

                // trigger downstream
                synchronized (schedulingLock) {
                    if (fatal[0]==null) {
                        for (Node n : downstream) {
                            if (n.canRun())
                                n.submit();
                        }
                    }
                    pending[0]--;
                    schedulingLock.notify();
                }
            }

            public void submit() {
                pending[0]++;
                submitted = true;
                e.execute(this);
            }
        }

        // checkpoints are union of goals
        Set<Object> milestones = new HashSet<Object>();
        for (Task t : tasks) {
            milestones.addAll(t.attains());
            milestones.addAll(t.requires());
        }
        Map<Object,Node> checkpoints = new HashMap<Object,Node>();
        for (final Object milestone : milestones) {
            checkpoints.put(milestone,new Node(new Runnable() {
                public void run() {
                    listener.onAttained(milestone);
                }
            }));

        }

        // build DAG
        Set<Node> dag = new HashSet<Node>(checkpoints.values());
        for (final Task t : tasks) {
            Node n = new Node(new Runnable() {
                public void run() {
                    listener.onTaskStarted(t);
                    try {
                        t.run();
                        listener.onTaskCompleted(t);
                    } catch (Throwable x) {
                        listener.onTaskFailed(t,x);
                        throw new TunnelException(x);
                    }
                }
            });
            for (Object req : t.requires())
                n.addPrerequisite(checkpoints.get(req));
            for (Object a : t.attains())
                checkpoints.get(a).addPrerequisite(n);
            dag.add(n);
        }

        // kick of initialization
        synchronized (schedulingLock) {
            for (Node n : dag) {
                if (n.prerequisites.isEmpty())
                    n.submit();
            }

            // block until everything is done
            while(pending[0]>0) {
                schedulingLock.wait();
                if (fatal[0]!=null)
                    throw new ReactorException(fatal[0].getCause());
            }
        }
    }

    public static Session fromTasks(final Collection<? extends Task> tasks) {
        try {
            return new Session(Collections.singleton(new TaskBuilder() {
                public void discoverTasks(Session session, Collection<Task> result) {
                    result.addAll(tasks);
                }
            }));
        } catch (IOException e) {
            throw new AssertionError(e); // impossible
        }
    }
}
