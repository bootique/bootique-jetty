/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.v11.connector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.config.PolymorphicConfiguration;
import io.bootique.value.Duration;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.Objects;

@BQConfig
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = HttpConnectorFactory.class)
public abstract class ConnectorFactory implements PolymorphicConfiguration {

    private int acceptorThreads;
    private int selectorThreads;
    private PortFactory port;
    private String host;
    private int responseHeaderSize;
    private int requestHeaderSize;
    private Duration idleTimeout;
    private boolean sendServerVersion;

    public ConnectorFactory() {
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

        connector.setPort(resolvePort());
        connector.setIdleTimeout(getIdleTimeoutMs());
        connector.setHost(getHost());

        return connector;
    }

    protected long getIdleTimeoutMs() {
        return idleTimeout != null ? idleTimeout.getDuration().toMillis() : 30 * 1000;
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
        httpConfig.setSendServerVersion(sendServerVersion);

        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        return httpConfig;
    }

    protected ByteBufferPool buildBufferPool() {
        // hardcoded for now... if needed we can turn these into properties
        return new ArrayByteBufferPool(64, 1024, 64 * 1024);
    }

    protected int resolvePort() {
        return port != null
                // passing "host" is important as the connector can listen on specific interfaces
                // that have different port availability situation.
                ? port.resolve(getHost())
                : PortFactory.DEFAULT_PORT;
    }

    public PortFactory getPort() {
        return port;
    }

    @BQConfigProperty("Connector listen port. Can be either a positive integer or a string 'any'. The latter " +
            "would cause Jetty to listen on an arbitrary available port determined during startup")
    public void setPort(PortFactory port) {
        this.port = port;
    }

    /**
     * @return configured host value.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the network interface this connector should bind to, either as IP or hostname. This setting is optional.
     * By default connector binds to all interfaces.
     *
     * @param host the network interface this connector binds to, either as IP or hostname.
     */
    @BQConfigProperty
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return max size of Jetty request headers (and GET URLs).
     */
    public int getRequestHeaderSize() {
        return requestHeaderSize;
    }

    /**
     * Sets a max size in bytes of Jetty request headers (and GET URLs). By
     * default it is 8K.
     *
     * @param requestHeaderSize request header size value in bytes.
     */
    @BQConfigProperty
    public void setRequestHeaderSize(int requestHeaderSize) {
        this.requestHeaderSize = requestHeaderSize;
    }

    /**
     * @return max size of Jetty response headers.
     */
    public int getResponseHeaderSize() {
        return responseHeaderSize;
    }

    /**
     * Sets a max size in bytes of Jetty response headers. By default it is 8K.
     *
     * @param responseHeaderSize response header size value in bytes.
     */
    @BQConfigProperty
    public void setResponseHeaderSize(int responseHeaderSize) {
        this.responseHeaderSize = responseHeaderSize;
    }

    /**
     * @return a configured number of acceptor threads.
     */
    public int getAcceptorThreads() {
        return acceptorThreads;
    }

    /**
     * @param acceptorThreads A desired number of acceptor threads.
     */
    @BQConfigProperty("A desired number of acceptor threads. If not provided, Jetty will calculate an optimal value based " +
            "on the number of available processor cores.")
    public void setAcceptorThreads(int acceptorThreads) {
        this.acceptorThreads = acceptorThreads;
    }

    /**
     * @return a configured number of selector threads.
     */
    public int getSelectorThreads() {
        return selectorThreads;
    }

    /**
     * @param selectorThreads A desired number of selector threads.
     */
    @BQConfigProperty("A desired number of selector threads. If not provided, Jetty will calculate an optimal" +
            " value based on the number of available processor cores.")
    public void setSelectorThreads(int selectorThreads) {
        this.selectorThreads = selectorThreads;
    }

    /**
     * @param idleTimeout
     */
    @BQConfigProperty
    public void setIdleTimeout(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * @since 1.1
     */
    public boolean isSendServerVersion() {
        return sendServerVersion;
    }

    /**
     * @param sendServerVersion
     * @since 1.1
     */
    @BQConfigProperty("Property to include server version header in responses.")
    public void setSendServerVersion(boolean sendServerVersion) {
        this.sendServerVersion = sendServerVersion;
    }
}
