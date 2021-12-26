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

import junit.framework.TestCase;
import org.apache.commons.io.output.TeeWriter;
import org.jvnet.hudson.reactor.TaskGraphBuilder.Handle;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Kohsuke Kawaguchi
 */
public class SessionTest extends TestCase {
    /**
     * Makes sure the ordering happens.
     */
    public void testSequentialOrdering() throws Exception {
        Reactor s = buildSession("->t1->m1 m1->t2->m2 m2->t3->", (session, id) -> System.out.println(id));
        assertEquals(3,s.size());

        String sw = execute(s);

        assertEqualsIgnoreNewlineStyle("Started t1\nEnded t1\nAttained m1\nStarted t2\nEnded t2\nAttained m2\nStarted t3\nEnded t3\n", sw);
    }

    private String execute(Reactor s, ReactorListener... addedListeners) throws Exception {
        StringWriter sw = new StringWriter();
        System.out.println("----");
        final PrintWriter w = new PrintWriter(new TeeWriter(sw,new OutputStreamWriter(System.out)),true);

        ReactorListener listener = new ReactorListener() {

            //TODO: Does it really needs handlers to be synchronized?
            @Override
            public synchronized void onTaskStarted(Task t) {
                w.println("Started "+t.getDisplayName());
            }

            @Override
            public synchronized void onTaskCompleted(Task t) {
                w.println("Ended "+t.getDisplayName());
            }

            @Override
            public synchronized void onTaskFailed(Task t, Throwable err, boolean fatal) {
                w.println("Failed "+t.getDisplayName()+" with "+err);
            }

            @Override
            public synchronized void onAttained(Milestone milestone) {
                w.println("Attained "+milestone);
            }
        };
        if (addedListeners.length > 0) {
            List<ReactorListener> listeners = Arrays.stream(addedListeners).collect(Collectors.toList());
            listeners.add(0, listener);
            listener = new ReactorListener.Aggregator(listeners);
        }
        s.execute(Executors.newCachedThreadPool(), listener);
        return sw.toString();
    }

    /**
     * Makes sure tasks can be executed in parallel.
     */
    public void testConcurrentExecution() throws Exception {
        execute(buildSession("->t1-> ->t2->", createLatch(2)));
    }

    /**
     * Scheduling of downstream jobs go through slightly different path, so test that too.
     */
    public void testConcurrentExecution2() throws Exception {
        execute(buildSession("->t1->m m->t2-> m->t3->", new TestTask() {
            TestTask latch = createLatch(2);
            @Override
            public void run(Reactor reactor, String id) throws Exception {
                if (id.equals("t1"))    return;
                latch.run(reactor, id);
            }
        }));
    }

    /**
     * Is the exception properly forwarded?
     */
    public void testFailure() throws Exception {
        final Exception[] e = new Exception[1];
        try {
            execute(buildSession("->t1->", (reactor, id) -> {
                throw e[0]=new IOException("Yep");
            }));
            fail();
        } catch (ReactorException x) {
            assertSame(e[0],x.getCause());
        }
    }

    /**
     * Is the exception in listener's onTaskStarted properly forwarded?
     */
    public void testListenerOnTaskStartedFailure() throws Exception {
        final List<Throwable> errors = new ArrayList<>();
        try {
            execute(buildSession("->t1->m", createNoOp()),
                new ReactorListener() {
                    @Override
                    public void onTaskStarted(Task t) {
                        Error ex = new AssertionError("Listener error 1");
                        errors.add(ex);
                        throw ex;
                    }
                }, new ReactorListener() {
                    @Override
                    public void onTaskStarted(Task t) {
                        RuntimeException ex = new NullPointerException("Listener runtime exception");
                        errors.add(ex);
                        throw ex;
                    }
                }
            );
            fail();
        } catch (ReactorException x) {
            assertEquals(2, errors.size());
            assertSame(errors.get(0),x.getCause());
            assertSame(errors.get(1),x.getCause().getSuppressed()[0]);
        }
    }

    /**
     * Is the exception in listener's onTaskCompleted properly forwarded?
     */
    public void testListenerOnTaskCompletedFailure() throws Exception {
        final List<Throwable> errors = new ArrayList<>();
        try {
            execute(buildSession("->t1->m", createNoOp()),
                   new ReactorListener() {
                    @Override
                    public void onTaskCompleted(Task t) {
                        Error ex = new AssertionError("Listener error");
                        errors.add(ex);
                        throw ex;
                    }
                }, new ReactorListener() {
                    @Override
                    public void onTaskCompleted(Task t) {
                        RuntimeException ex = new NullPointerException("Listener runtime exception");
                        errors.add(ex);
                        throw ex;
                    }
                }
            );
            fail();
        } catch (ReactorException x) {
            assertEquals(2, errors.size());
            assertSame(errors.get(0),x.getCause());
            assertSame(errors.get(1),x.getCause().getSuppressed()[0]);
        }
    }

    /**
     * Is the exception in listener's onTaskFailed properly forwarded?
     */
    public void testListenerOnTaskFailedFailure() throws Exception {
        final List<Throwable> errors = new ArrayList<>();
        final Exception[] ex = new Exception[1];
        try {
            execute(buildSession("->t1->m", (reactor, id) -> {
                throw ex[0]=new IOException("Yep");
            }), new ReactorListener() {
                    @Override
                    public void onTaskFailed(Task t, Throwable err, boolean fatal) {
                        Error ex = new AssertionError("Listener error");
                        errors.add(ex);
                        throw ex;
                    }
                }, new ReactorListener() {
                    @Override
                    public void onTaskFailed(Task t, Throwable err, boolean fatal) {
                        RuntimeException ex = new NullPointerException("Listener runtime exception");
                        errors.add(ex);
                        throw ex;
                    }
                }
            );
            fail();
        } catch (ReactorException x) {
            assertEquals(2, errors.size());
            assertSame(errors.get(0),x.getCause());
            assertSame(errors.get(1),x.getCause().getSuppressed()[0]);
            assertSame(ex[0],x.getCause().getSuppressed()[1]);
        }
    }

    /**
     * Is the exception in listener's onAttained properly forwarded?
     */
    public void testListenerOnAttainedFailure() throws Exception {
        final List<Throwable> errors = new ArrayList<>();
        try {
            execute(buildSession("->t1->m", createNoOp()),
                new ReactorListener() {
                    @Override
                    public void onAttained(Milestone milestone) {
                        Error ex = new AssertionError("Listener error");
                        errors.add(ex);
                        throw ex;
                    }
                }, new ReactorListener() {
                    @Override
                    public void onAttained(Milestone milestone) {
                        RuntimeException ex = new NullPointerException("Listener runtime exception");
                        errors.add(ex);
                        throw ex;
                    }
                }
            );
            fail();
        } catch (ReactorException x) {
            assertEquals(2, errors.size());
            assertSame(errors.get(0),x.getCause());
            assertSame(errors.get(1),x.getCause().getSuppressed()[0]);
        }
    }

    /**
     * Dynamically add a new task that can run immediately.
     */
    public void testDynamicTask() throws Exception {
        final Reactor s = buildSession("->t1->m1 m1->t2->", new TestTask() {
            @Override
            public void run(Reactor session, String id) {
                if (id.equals("t2")) {
                    // should start running immediately because it's prerequisite is already met.
                    session.add(new TaskImpl("m1->t3->",this));
                }
            }
        });
        assertEquals(2,s.size());
        String result = execute(s);

        // one more task added  during execution
        assertEquals(3,s.size());
        assertEqualsIgnoreNewlineStyle("Started t1\nEnded t1\nAttained m1\nStarted t2\nEnded t2\nStarted t3\nEnded t3\n", result);
    }

    /**
     * Dynamically add a new task that can be only run later
     */
    public void testDynamicTask2() throws Exception {
        final Reactor s = buildSession("->t1->m1 m1->t2->m2 m2->t3->m3", new TestTask() {
            @Override
            public void run(Reactor session, String id) {
                if (id.equals("t2")) {
                    // should block until m3 is attained
                    session.add(new TaskImpl("m3->t4->",this));
                }
            }
        });
        assertEquals(3,s.size());
        String result = execute(s);

        // one more task added  during execution
        assertEquals(4,s.size());
        assertEqualsIgnoreNewlineStyle("Started t1\n" +
                "Ended t1\n" +
                "Attained m1\n" +
                "Started t2\n" +
                "Ended t2\n" +
                "Attained m2\n" +
                "Started t3\n" +
                "Ended t3\n" +
                "Attained m3\n" +
                "Started t4\n" +
                "Ended t4\n", result);
    }

    /**
     * Milestones that no one attains should be attained by default.
     */
    public void testDanglingMilestone() throws Exception {
        Reactor s = buildSession("m1->t1->m2", (session, id) -> { });
        String result = execute(s);
        assertEqualsIgnoreNewlineStyle("Attained m1\nStarted t1\nEnded t1\nAttained m2\n",result);
    }

    /**
     * Tasks that are non-fatal.
     */
    public void testNonFatalTask() throws Exception {
        TaskGraphBuilder g = new TaskGraphBuilder();
        Handle h = g.notFatal().add("1st", reactor -> {
            throw new IllegalArgumentException();   // simulated failure
        });
        g.requires(h).add("2nd", reactor -> { });
        String result = execute(new Reactor(g));
        assertEqualsIgnoreNewlineStyle(
                "Started 1st\n" +
                "Failed 1st with java.lang.IllegalArgumentException\n" +
                "Attained 1st\n" +
                "Started 2nd\n" +
                "Ended 2nd\n" +
                "Attained 2nd\n",result);
    }

    /**
     * Creates {@link TestTask} that waits for multiple tasks to be blocked together.
     */
    private TestTask createLatch(final int threshold) {
        return new TestTask() {
            final Object lock = new Object();
            int pending = 0;
            boolean go = false;

            @Override
            public void run(Reactor reactor, String id) throws InterruptedException {
                synchronized (lock) {
                    pending++;
                    if (pending==threshold) {
                        // make sure two of us execute at the same time
                        go = true;
                        lock.notifyAll();
                    }

                    while (!go)
                        lock.wait();
                }
            }
        };
    }

    /**
     * Creates {@link TestTask} that does nothing.
     */
    private TestTask createNoOp() {
        return (reactor, id) -> {
            // do nothing
        };
    }

    interface TestTask {
        void run(Reactor reactor, String id) throws Exception;
    }

    private Reactor buildSession(String spec, final TestTask work) throws Exception {
        Collection<TaskImpl> tasks = new ArrayList<>();
        for (String node : spec.split(" "))
            tasks.add(new TaskImpl(node,work));

        return new Reactor(TaskBuilder.fromTasks(tasks));
    }

    static class TaskImpl implements Task {
        final String id;
        final Collection<Milestone> requires;
        final Collection<Milestone> attains;
        final TestTask work;

        TaskImpl(String id, TestTask work) {
            String[] tokens = id.split("->");
            this.id = tokens[1];
            // tricky handling necessary due to inconsistency in how split works
            this.requires = adapt(tokens[0].length()==0 ? Collections.emptyList() : Arrays.asList(tokens[0].split(",")));
            this.attains = adapt(tokens.length<3 ? Collections.emptyList() : Arrays.asList(tokens[2].split(",")));
            this.work = work;
        }

        private Collection<Milestone> adapt(List<String> strings) {
            List<Milestone> r = new ArrayList<>();
            for (String s : strings)
                r.add(new MilestoneImpl(s));
            return r;
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
            return id;
        }

        @Override
        public void run(Reactor reactor) throws Exception {
            work.run(reactor, id);
        }

        @Override
        public boolean failureIsFatal() {
            return true;
        }
    }

    private static class MilestoneImpl implements Milestone {
        private final String id;

        private MilestoneImpl(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MilestoneImpl milestone = (MilestoneImpl) o;
            return id.equals(milestone.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static void assertEqualsIgnoreNewlineStyle(String s1, String s2) {
        assertEquals(normalizeLineEnds(s1), normalizeLineEnds(s2));
    }

    private static String normalizeLineEnds(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }
}
