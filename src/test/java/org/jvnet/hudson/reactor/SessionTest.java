package org.jvnet.hudson.reactor;

import junit.framework.TestCase;
import org.objectweb.carol.cmi.test.TeeWriter;
import org.jvnet.hudson.reactor.TaskGraphBuilder.Handle;

import javax.naming.NamingException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author Kohsuke Kawaguchi
 */
public class SessionTest extends TestCase {
    /**
     * Makes sure the ordering happens.
     */
    public void testSequentialOrdering() throws Exception {
        Reactor s = buildSession("->t1->m1 m1->t2->m2 m2->t3->",new TestTask() {
            public void run(Reactor session, String id) throws Exception {
                System.out.println(id);
            }
        });
        assertEquals(3,s.size());

        String sw = execute(s);

        assertEquals("Started t1\nEnded t1\nAttained m1\nStarted t2\nEnded t2\nAttained m2\nStarted t3\nEnded t3\n", sw);
    }

    private String execute(Reactor s) throws Exception {
        StringWriter sw = new StringWriter();
        System.out.println("----");
        final PrintWriter w = new PrintWriter(new TeeWriter(sw,new OutputStreamWriter(System.out)),true);

        s.execute(Executors.newCachedThreadPool(),new ReactorListener() {
            public synchronized void onTaskStarted(Task t) {
                w.println("Started "+t.getDisplayName());
            }

            public synchronized void onTaskCompleted(Task t) {
                w.println("Ended "+t.getDisplayName());
            }

            public synchronized void onTaskFailed(Task t, Throwable err, boolean fatal) {
                w.println("Failed "+t.getDisplayName()+" with "+err);
            }

            public synchronized void onAttained(Milestone milestone) {
                w.println("Attained "+milestone);
            }
        });
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
            execute(buildSession("->t1->", new TestTask() {
                public void run(Reactor reactor, String id) throws Exception {
                    throw e[0]=new NamingException("Yep");
                }
            }));
            fail();
        } catch (ReactorException x) {
            assertSame(e[0],x.getCause());
        }
    }

    /**
     * Dynamically add a new task that can run immediately.
     */
    public void testDynamicTask() throws Exception {
        final Reactor s = buildSession("->t1->m1 m1->t2->", new TestTask() {
            public void run(Reactor session, String id) throws Exception {
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
        assertEquals("Started t1\nEnded t1\nAttained m1\nStarted t2\nEnded t2\nStarted t3\nEnded t3\n", result);
    }

    /**
     * Dynamically add a new task that can be only run later
     */
    public void testDynamicTask2() throws Exception {
        final Reactor s = buildSession("->t1->m1 m1->t2->m2 m2->t3->m3", new TestTask() {
            public void run(Reactor session, String id) throws Exception {
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
        assertEquals("Started t1\n" +
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
        Reactor s = buildSession("m1->t1->m2",new TestTask() {
            public void run(Reactor session, String id) throws Exception {
            }
        });
        String result = execute(s);
        assertEquals("Attained m1\nStarted t1\nEnded t1\nAttained m2\n",result);
    }

    /**
     * Tasks that are non-fatal.
     */
    public void testNonFatalTask() throws Exception {
        TaskGraphBuilder g = new TaskGraphBuilder();
        Handle h = g.notFatal().add("1st", new Executable() {
            public void run(Reactor reactor) throws Exception {
                throw new IllegalArgumentException();   // simulated failure
            }
        });
        g.requires(h).add("2nd",new Executable() {
            public void run(Reactor reactor) throws Exception {
            }
        });
        String result = execute(new Reactor(g));
        assertEquals(
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

    interface TestTask {
        void run(Reactor reactor, String id) throws Exception;
    }

    private Reactor buildSession(String spec, final TestTask work) throws Exception {
        Collection<TaskImpl> tasks = new ArrayList<TaskImpl>();
        for (String node : spec.split(" "))
            tasks.add(new TaskImpl(node,work));

        return new Reactor(TaskBuilder.fromTasks(tasks));
    }

    class TaskImpl implements Task {
        final String id;
        final Collection<Milestone> requires;
        final Collection<Milestone> attains;
        final TestTask work;

        TaskImpl(String id, TestTask work) {
            String[] tokens = id.split("->");
            this.id = tokens[1];
            // tricky handling necessary due to inconsistency in how split works
            this.requires = adapt(tokens[0].length()==0 ? Collections.<String>emptyList() : Arrays.asList(tokens[0].split(",")));
            this.attains = adapt(tokens.length<3 ? Collections.<String>emptyList() : Arrays.asList(tokens[2].split(",")));
            this.work = work;
        }

        private Collection<Milestone> adapt(List<String> strings) {
            List<Milestone> r = new ArrayList<Milestone>();
            for (String s : strings)
                r.add(new MilestoneImpl(s));
            return r;
        }

        public Collection<Milestone> requires() {
            return requires;
        }

        public Collection<Milestone> attains() {
            return attains;
        }

        public String getDisplayName() {
            return id;
        }

        public void run(Reactor reactor) throws Exception {
            work.run(reactor, id);
        }

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
}
