package org.jvnet.hudson.reactor;

import junit.framework.TestCase;

import java.util.concurrent.Executors;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;

import org.objectweb.carol.cmi.test.TeeWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class SessionTest extends TestCase {
    /**
     * Makes sure the ordering happens.
     */
    public void testSequentialOrdering() throws Exception {
        Session s = buildSession("->t1->m1 m1->t2->m2 m2->t3->",new TestTask() {
            public void run(String id) throws Exception {
                System.out.println(id);
            }
        });
        assertEquals(3,s.size());

        String sw = execute(s);

        assertEquals("Started t1\nEnded t1\nAttained m1\nStarted t2\nEnded t2\nAttained m2\nStarted t3\nEnded t3\n", sw);
    }

    private String execute(Session s) throws InterruptedException, InvocationTargetException {
        StringWriter sw = new StringWriter();
        System.out.println("----");
        final PrintWriter w = new PrintWriter(new TeeWriter(sw,new OutputStreamWriter(System.out)),true);

        s.execute(Executors.newCachedThreadPool(),new SessionListener() {
            public synchronized void onTaskStarted(Task t) {
                w.println("Started "+t.getDisplayName());
            }

            public synchronized void onTaskCompleted(Task t) {
                w.println("Ended "+t.getDisplayName());
            }

            public synchronized void onTaskFailed(Task t, Throwable err) {
                w.println("Failed "+t.getDisplayName()+" with "+err);
            }

            public synchronized void onAttained(Object milestone) {
                w.println("Attained "+milestone);
            }
        });
        return sw.toString();
    }

    @Initializer(attains="foundation",displayName="1")
    public static void init1() {
    }
    @Initializer(requires="foundation",attains="roof",displayName="2")
    public static void init2() {
    }
    @Initializer(requires="roof",displayName="3")
    public static void init3() {
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
            public void run(String id) throws Exception {
                if (id.equals("t1"))    return;
                latch.run(id);
            }
        }));
    }

    /**
     * Creates {@link TestTask} that waits for multiple tasks to be blocked together.
     */
    private TestTask createLatch(final int threshold) {
        return new TestTask() {
            final Object lock = new Object();
            int pending = 0;
            boolean go = false;

            public void run(String id) throws InterruptedException {
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
        void run(String id) throws Exception;
    }

    private Session buildSession(String spec, final TestTask work) {
        class TaskImpl implements Task {
            final String id;
            final Collection<String> requires;
            final Collection<String> attains;

            TaskImpl(String id) {
                String[] tokens = id.split("->");
                this.id = tokens[1];
                // tricky handling necessary due to inconsistency in how split works
                this.requires = tokens[0].length()==0 ? Collections.<String>emptyList() : Arrays.asList(tokens[0].split(","));
                this.attains = tokens.length<3 ? Collections.<String>emptyList() : Arrays.asList(tokens[2].split(","));
            }

            public Collection<?> requires() {
                return requires;
            }

            public Collection<?> attains() {
                return attains;
            }

            public String getDisplayName() {
                return id;
            }

            public void run() {
                try {
                    work.run(id);
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        Collection<TaskImpl> tasks = new ArrayList<TaskImpl>();
        for (String node : spec.split(" "))
            tasks.add(new TaskImpl(node));

        return Session.fromTasks(tasks);
    }
}
