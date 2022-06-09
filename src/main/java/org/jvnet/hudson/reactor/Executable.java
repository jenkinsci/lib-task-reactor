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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents code that can be executed.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface Executable {

    /**
     * Executes a task. Any exception thrown will abort the session.
     * @param reactor Reactor to be executed
     * @throws Exception Execution error
     */
    @SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", justification = "TODO needs triage")
    void run(Reactor reactor) throws Exception;

    /**
     * No-op implementation.
     */
    public static final Executable NOOP = new Executable() {
        @Override
        @SuppressFBWarnings(value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", justification = "TODO needs triage")
        public void run(Reactor reactor) throws Exception {
        }

        @Override
        public String toString() {
            return "noop";
        }
    };
}
