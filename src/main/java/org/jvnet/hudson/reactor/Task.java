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
