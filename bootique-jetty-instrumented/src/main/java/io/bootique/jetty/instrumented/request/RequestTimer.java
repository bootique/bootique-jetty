package io.bootique.jetty.instrumented.request;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import java.util.Objects;

/**
 * Provides timing metrics for request execution, optionally logging of request start and finish marks.
 *
 * @since 0.15
 */
public class RequestTimer implements ServletRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTimer.class);
    private static final String TIMER_KEY = RequestTimer.class.getName();

    private Timer requestTimer;

    public RequestTimer(MetricRegistry metricRegistry) {
        this.requestTimer = metricRegistry.timer(MetricRegistry.name(RequestTimer.class, "request-timer"));
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        Timer.Context requestTimerContext = requestTimer.time();
        sre.getServletRequest().setAttribute(TIMER_KEY, requestTimerContext);
        LOGGER.info("started");
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {

        // note that we are skipping request parameter/URL/etc. logging...
        // This is done by Slf4jRequestLog. Here we only log timing

        Timer.Context requestTimerContext = (Timer.Context) sre.getServletRequest().getAttribute(TIMER_KEY);
        Objects.requireNonNull(requestTimerContext, "No timer found at the end of request");
        long timeNanos = requestTimerContext.stop();
        LOGGER.info("finished in {} ms", timeNanos / 1000000);
    }
}
