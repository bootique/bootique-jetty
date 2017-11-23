package io.bootique.jetty.metrics;

import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import io.bootique.metrics.health.HealthCheckStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * A servlet that executes app healthchecks and returns text status document. By default mapped as "/health" in the
 * web application context.
 *
 * @since 0.20
 */
// inspired com.yammer.metrics.servlet.HealthCheckServlet, only better integrated to Bootique and using our own format
// TODO: config-driven verbosity levels .. perhaps use nagios plugin format?
public class HealthCheckServlet extends HttpServlet {

    private static final String CONTENT_TYPE = "text/plain";
    private HealthCheckRegistry registry;

    public HealthCheckServlet(HealthCheckRegistry registry) {
        this.registry = registry;
    }

    private static boolean isAllHealthy(Map<String, HealthCheckOutcome> results) {
        for (HealthCheckOutcome result : results.values()) {
            if (result.getStatus() != HealthCheckStatus.OK) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (PrintWriter writer = response.getWriter()) {
            doWrite(response, writer);
        }
    }

    protected void doWrite(HttpServletResponse response, PrintWriter writer) throws IOException {

        Map<String, HealthCheckOutcome> results = registry.runHealthChecks();

        response.setContentType(CONTENT_TYPE);
        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

        if (results.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            writer.println("! No health checks registered.");
            return;
        }

        if (isAllHealthy(results)) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        results.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).forEach(e -> {
            HealthCheckOutcome result = e.getValue();
            if (result.getStatus() == HealthCheckStatus.OK) {
                if (result.getMessage() != null) {
                    writer.format("* %s: OK - %s\n", e.getKey(), result.getMessage());
                } else {
                    writer.format("* %s: OK\n", e.getKey());
                }
            } else {
                if (result.getMessage() != null) {
                    writer.format("! %s: %s - %s\n", e.getKey(), result.getStatus().name(), result.getMessage());
                }

                @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                Throwable error = result.getError();
                if (error != null) {
                    writer.println();
                    error.printStackTrace(writer);
                    writer.println();
                }
            }
        });
    }
}
