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

/**
 * Default {@link Milestone} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public class MilestoneImpl implements Milestone {
    private final String id;

    /**
     * Compares equal with other {@link MilestoneImpl}s with the same ID.
     */
    public MilestoneImpl(String id) {
        this.id = id;
    }

    /**
     * Only compare equal with this {@link MilestoneImpl} and nothing else.
     */
    public MilestoneImpl() {
        this(null);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj)  return true;
        if (obj instanceof MilestoneImpl) {
            MilestoneImpl that = (MilestoneImpl) obj;
            return this.id!=null && that.id!=null && this.id.equals(that.id);
        }
        return false;
    }
}
