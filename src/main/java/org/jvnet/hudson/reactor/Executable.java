package org.jvnet.hudson.reactor;

/**
 * Represents code that can be executed.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface Executable {
    /**
     * Executes a task. Any exception thrown will abort the session.
     * @param session
     */
    void run(Session session) throws Exception;
}
