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

    public String toString() {
        return id;
    }

    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }

    public boolean equals(Object obj) {
        if (this==obj)  return true;
        if (obj instanceof MilestoneImpl) {
            MilestoneImpl that = (MilestoneImpl) obj;
            return this.id!=null && that.id!=null && this.id.equals(that.id);
        }
        return false;
    }
}
