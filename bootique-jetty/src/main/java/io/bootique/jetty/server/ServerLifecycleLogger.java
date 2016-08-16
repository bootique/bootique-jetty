package io.bootique.jetty.server;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.stream.Collectors;

public class ServerLifecycleLogger extends AbstractLifeCycle.AbstractLifeCycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLifecycleLogger.class);

    private Collection<Integer> ports;
    private String context;

    private long t0;

    public ServerLifecycleLogger(Collection<Integer> ports, String context) {
        this.ports = ports;
        this.context = context;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        t0 = System.currentTimeMillis();
        LOGGER.info("Starting jetty...");
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        long t1 = System.currentTimeMillis();

        if (ports.isEmpty()) {
            LOGGER.info("Started Jetty in {} ms. No connectors configured", t1 - t0);
        }
        if (ports.size() == 1) {
            String url = baseUrl(ports.iterator().next());
            LOGGER.info("Started Jetty in {} ms. Base URL: {}", t1 - t0, url);
        } else {
            String urls = ports.stream().map(this::baseUrl).collect(Collectors.joining(", "));
            LOGGER.info("Started Jetty in {} ms. Base URLs: {}", t1 - t0, urls);
        }
    }

    private String baseUrl(int port) {
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error getting localhost", e);
        }
        return "http://" + host + ":" + port + context;
    }
}
