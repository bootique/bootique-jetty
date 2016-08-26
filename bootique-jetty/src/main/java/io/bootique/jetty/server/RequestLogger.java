package io.bootique.jetty.server;

import org.eclipse.jetty.server.Slf4jRequestLog;

/**
 * Extending Jetty request logger without adding any functionality, simply to separate logging configuration between
 * Jetty and Bootique.
 *
 * @since 0.18
 */
public class RequestLogger extends Slf4jRequestLog {

    public RequestLogger() {
        setLoggerName(getClass().getName());
        setExtended(true);
    }
}
