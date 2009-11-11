package org.jvnet.hudson.reactor;

import java.util.Collection;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class TaskBuilder {
    public abstract void discoverTasks(Session session, Collection<Task> result) throws IOException;
}
