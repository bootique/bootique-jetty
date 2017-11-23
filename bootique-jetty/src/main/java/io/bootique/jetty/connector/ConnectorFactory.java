package io.bootique.jetty.connector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.config.PolymorphicConfiguration;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.Objects;

/**
 * @since 0.18
 */
@BQConfig
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = HttpConnectorFactory.class)
public abstract class ConnectorFactory implements PolymorphicConfiguration {

    private int acceptorThreads;
    private int selectorThreads;
    private int port;
    private String host;
    private int responseHeaderSize;
    private int requestHeaderSize;

    public ConnectorFactory() {
        this.port = 8080;
        this.requestHeaderSize = 8 * 1024;
        this.responseHeaderSize = 8 * 1024;
    }

    public ServerConnector createConnector(Server server) {

        // a few things are hardcoded for now... if needed we can turn these
        // into properties

        HttpConfiguration httpConfig = buildHttpConfiguration();
        ConnectionFactory[] connectionFactories = buildHttpConnectionFactories(httpConfig);
        Scheduler scheduler = new ScheduledExecutorScheduler();
        ByteBufferPool bufferPool = buildBufferPool();

        // "-1" is Jetty default for acceptor and selector threads that triggers default init algorithm based on
        // the number of machine cores
        int acceptorThreads = this.acceptorThreads > 0 ? this.acceptorThreads : -1;
        int selectorThreads = this.selectorThreads > 0 ? this.selectorThreads : -1;

        ThreadPool threadPool = Objects.requireNonNull(server.getThreadPool());

        ServerConnector connector = new ServerConnector(
                server,
                threadPool,
                scheduler,
                bufferPool,
                acceptorThreads,
                selectorThreads,
                connectionFactories);

        connector.setPort(getPort());
        connector.setIdleTimeout(30 * 1000);
        connector.setHost(getHost());

        return connector;
    }

    protected abstract ConnectionFactory[] buildHttpConnectionFactories(HttpConfiguration httpConfig);

    protected HttpConfiguration buildHttpConfiguration() {

        HttpConfiguration httpConfig = new HttpConfiguration();

        // most parameters are hardcoded for now... we should turn these
        // into properties

        httpConfig.setHeaderCacheSize(512);
        httpConfig.setOutputBufferSize(32 * 1024);
        httpConfig.setRequestHeaderSize(requestHeaderSize);
        httpConfig.setResponseHeaderSize(responseHeaderSize);
        httpConfig.setSendDateHeader(true);
        httpConfig.setSendServerVersion(true);

        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        return httpConfig;
    }

    protected ByteBufferPool buildBufferPool() {
        // hardcoded for now... if needed we can turn these into properties
        return new ArrayByteBufferPool(64, 1024, 64 * 1024);
    }

    /**
     * @return configured listen port.
     * @since 0.15
     */
    public int getPort() {
        return port;
    }

    @BQConfigProperty
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return configured host value.
     * @since 0.18
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the network interface this connector should bind to, either as IP or hostname. This setting is optional.
     * By default connector binds to all interfaces.
     *
     * @param host the network interface this connector binds to, either as IP or hostname.
     * @since 0.18
     */
    @BQConfigProperty
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return max size of Jetty request headers (and GET URLs).
     * @since 0.15
     */
    public int getRequestHeaderSize() {
        return requestHeaderSize;
    }

    /**
     * Sets a max size in bytes of Jetty request headers (and GET URLs). By
     * default it is 8K.
     *
     * @param requestHeaderSize request header size value in bytes.
     * @since 0.15
     */
    @BQConfigProperty
    public void setRequestHeaderSize(int requestHeaderSize) {
        this.requestHeaderSize = requestHeaderSize;
    }

    /**
     * @return max size of Jetty response headers.
     * @since 0.15
     */
    public int getResponseHeaderSize() {
        return responseHeaderSize;
    }

    /**
     * Sets a max size in bytes of Jetty response headers. By default it is 8K.
     *
     * @param responseHeaderSize response header size value in bytes.
     * @since 0.15
     */
    @BQConfigProperty
    public void setResponseHeaderSize(int responseHeaderSize) {
        this.responseHeaderSize = responseHeaderSize;
    }

    /**
     * @return a configured number of acceptor threads.
     * @since 0.25
     */
    public int getAcceptorThreads() {
        return acceptorThreads;
    }

    /**
     * @param acceptorThreads A desired number of acceptor threads.
     * @since 0.25
     */
    @BQConfigProperty("A desired number of acceptor threads. If not provided, Jetty will calculate an optimal value based " +
            "on the number of available processor cores.")
    public void setAcceptorThreads(int acceptorThreads) {
        this.acceptorThreads = acceptorThreads;
    }

    /**
     * @return a configured number of selector threads.
     * @since 0.25
     */
    public int getSelectorThreads() {
        return selectorThreads;
    }

    /**
     * @param selectorThreads A desired number of selector threads.
     * @since 0.25
     */
    @BQConfigProperty("A desired number of selector threads. If not provided, Jetty will calculate an optimal" +
            " value based on the number of available processor cores.")
    public void setSelectorThreads(int selectorThreads) {
        this.selectorThreads = selectorThreads;
    }
}
