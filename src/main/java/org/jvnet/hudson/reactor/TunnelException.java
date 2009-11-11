package org.jvnet.hudson.reactor;

/**
 * @author Kohsuke Kawaguchi
 */
class TunnelException extends RuntimeException {
    TunnelException(Throwable cause) {
        super(cause);
    }
}
