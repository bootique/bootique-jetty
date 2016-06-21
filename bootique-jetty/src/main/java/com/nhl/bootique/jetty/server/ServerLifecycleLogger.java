package com.nhl.bootique.jetty.server;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerLifecycleLogger extends AbstractLifeCycle.AbstractLifeCycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLifecycleLogger.class);

    private int port;
    private String context;

    private long t0;

    public ServerLifecycleLogger(int port, String context) {
        this.port = port;
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
        String url = baseUrl();
        LOGGER.info("Started Jetty in {} ms. Base URL: {}", t1 - t0, url);
    }

    private String baseUrl() {
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error getting localhost", e);
        }
        return "http://" + host + ":" + port + context;
    }
}
