package org.jvnet.hudson.reactor;

/**
 * Used to tunnel application-thrown {@link Throwable} (Error or Exception) to the caller.
 * @author Kohsuke Kawaguchi
 */
public class ReactorException extends Exception {
    ReactorException(Throwable cause) {
        super(cause);
    }
}
