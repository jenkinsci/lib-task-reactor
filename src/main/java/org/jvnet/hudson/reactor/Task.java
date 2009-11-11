package org.jvnet.hudson.reactor;

import java.util.Collection;

/**
 * One initialization task
 *
 * @author Kohsuke Kawaguchi
 */
public interface Task {
    /**
     * Indicates the milestones necessary before executing this.
     */
    Collection<? extends Milestone> requires();

    /**
     * Indicates the milestones that this initializer contributes.
     *
     * A milestone is considered attained if all the initializers that attains the given milestone
     * completes. So it works as a kind of join.
     */
    Collection<? extends Milestone> attains();

    /**
     * Human readable description of this task. Used for progress report.
     */
    String getDisplayName();

    /**
     * Executes a task. Any exception thrown will abort the session.
     */
    void run() throws Exception;
}
