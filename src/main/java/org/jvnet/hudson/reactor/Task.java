package org.jvnet.hudson.reactor;

import java.util.Collection;

/**
 * One initialization task
 *
 * @author Kohsuke Kawaguchi
 */
public interface Task extends Executable {
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
     *
     * @return
     *      null to indicate that this task doesn't have any significant to humans.
     */
    String getDisplayName();

    /**
     * Returns true if the failure of this task is fatal and should break the reactor.
     * If false, the failure is sent to the listener but the successive tasks will be
     * started as if this task was successful (and eventually resulting in the completion
     * of the reactor execution, provided that no other fatal failures occur.)
     */
    boolean failureIsFatal();
}
