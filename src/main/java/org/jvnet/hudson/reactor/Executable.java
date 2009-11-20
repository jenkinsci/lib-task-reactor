package org.jvnet.hudson.reactor;

/**
 * Represents code that can be executed.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface Executable {
    /**
     * Executes a task. Any exception thrown will abort the session.
     * @param reactor
     */
    void run(Reactor reactor) throws Exception;

    /**
     * No-op implementation.
     */
    public static final Executable NOOP = new Executable() {
        public void run(Reactor reactor) throws Exception {
        }

        @Override
        public String toString() {
            return "noop";
        }
    };
}
