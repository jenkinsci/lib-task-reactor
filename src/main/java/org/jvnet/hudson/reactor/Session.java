package org.jvnet.hudson.reactor;

import java.io.IOException;
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
public class Session implements Iterable<Session.Node> {
    private final Set<Node> nodes = new HashSet<Node>();

    /**
     * Number of tasks pending execution
     */
    private int pending = 0;

    /**
     * RuntimeException or Error that indicates a fatal failure in a task
     */
    private TunnelException fatal;

    /**
     * Milestones as nodes in DAG. Guarded by 'this'.
     */
    private final Map<Milestone,Node> milestones = new HashMap<Milestone,Node>();

    private Executor executor;

    private SessionListener listener;

    private boolean executed = false;

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
                fatal = t;
            } finally {
                done = true;
            }

            // trigger downstream
            synchronized (Session.this) {
                if (fatal==null) {
                    for (Node n : downstream)
                        n.runIfPossible();
                }
                pending--;
                Session.this.notify();
            }
        }

        public void runIfPossible() {
            if (!canRun())  return;
            pending++;
            submitted = true;
            executor.execute(this);
        }
    }


    public Session(Collection<? extends TaskBuilder> builders) throws IOException {
        for (TaskBuilder b : builders)
            for (Task t :b.discoverTasks(this))
                add(t);
    }

    public Session(TaskBuilder... builders) throws IOException {
        this(Arrays.asList(builders));
    }

    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

    public int size() {
        return nodes.size();
    }

    public void execute(Executor e) throws InterruptedException, ReactorException {
        execute(e,SessionListener.NOOP);
    }

    private synchronized Node milestone(final Milestone m) {
        Node n = milestones.get(m);
        if (n==null)
            milestones.put(m,n=new Node(new Runnable() {
                public void run() {
                    listener.onAttained(m);
                }
            }));
        return n;
    }

    /**
     * Adds a new {@link Task} to the reactor.
     *
     * <p>
     * This can be even invoked during execution.
     */
    public synchronized void add(final Task t) {
        Node n = new Node(new Runnable() {
            public void run() {
                listener.onTaskStarted(t);
                try {
                    t.run(Session.this);
                    listener.onTaskCompleted(t);
                } catch (Throwable x) {
                    listener.onTaskFailed(t,x);
                    throw new TunnelException(x);
                }
            }
        });
        for (Milestone req : t.requires())
            n.addPrerequisite(milestone(req));
        for (Milestone a : t.attains())
            milestone(a).addPrerequisite(n);
        nodes.add(n);

        if (executor!=null)
            n.runIfPossible();
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
    public synchronized void execute(final Executor e, final SessionListener listener) throws InterruptedException, ReactorException {
        if (executed)   throw new IllegalStateException("This session is already executed");
        executed = true;

        this.executor = e;
        this.listener = listener;
        try {
            // start everything that can run
            for (Node n : nodes) {
                if (n.prerequisites.isEmpty())
                    n.runIfPossible();
            }

            // block until everything is done
            while(pending>0) {
                wait();
                if (fatal!=null)
                    throw new ReactorException(fatal.getCause());
            }
        } finally {
            // avoid memory leak
            this.executor = null;
            this.listener = null;
        }
    }

    public static Session fromTasks(final Collection<? extends Task> tasks) {
        try {
            return new Session(Collections.singleton(new TaskBuilder() {
                public Iterable<? extends Task> discoverTasks(Session session) throws IOException {
                    return tasks;
                }
            }));
        } catch (IOException e) {
            throw new AssertionError(e); // impossible
        }
    }
}
