package org.jvnet.hudson.reactor;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class TaskBuilder {
    public abstract Iterable<? extends Task> discoverTasks(Session session) throws IOException;
}
