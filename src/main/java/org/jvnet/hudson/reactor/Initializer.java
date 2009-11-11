package org.jvnet.hudson.reactor;

import org.jvnet.hudson.annotation_indexer.Indexed;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Placed on static methods to indicate that it's an initialization task.
 *
 * @author Kohsuke Kawaguchi
 */
@Indexed
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Initializer {
    /**
     * Indicates the milestones necessary before executing this.
     */
    String[] requires() default {};

    /**
     * Indicates the milestones that this initializer contributes.
     *
     * A milestone is considered attained if all the initializers that attains the given milestone
     * completes. So it works as a kind of join.
     */
    String[] attains() default {};

    /**
     * Used as {@link Task#getDisplayName()} by the default implementation of {@link InitializerFinder}.
     */
    String displayName() default "";
}
