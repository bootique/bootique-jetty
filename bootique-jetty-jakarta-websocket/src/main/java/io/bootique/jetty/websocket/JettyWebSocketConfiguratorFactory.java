/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.jetty.websocket;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import io.bootique.jetty.request.RequestMDCManager;
import io.bootique.value.Bytes;
import io.bootique.value.Duration;

import java.util.Set;

/**
 * @since 3.0.M1
 */
@BQConfig
public class JettyWebSocketConfiguratorFactory {

    private Duration asyncSendTimeout;
    private Duration maxSessionIdleTimeout;
    private Bytes maxBinaryMessageBufferSize;
    private Bytes maxTextMessageBufferSize;

    public JettyWebSocketConfigurator createConfigurator(
            Injector injector,
            Set<EndpointKeyHolder> endpointKeys,
            RequestMDCManager mdcManager) {
        return new JettyWebSocketConfigurator(mdcManager, endpointKeys, injector, createConfig());
    }

    protected JettyWebSocketConfigurator.Config createConfig() {
        return new JettyWebSocketConfigurator.Config(
                asyncSendTimeout != null ? asyncSendTimeout.getDuration().toMillis() : 60000L,
                maxSessionIdleTimeout != null ? maxSessionIdleTimeout.getDuration().toMillis() : 300000L,
                maxBinaryMessageBufferSize != null ? (int) maxBinaryMessageBufferSize.getBytes() : 32768,
                maxTextMessageBufferSize != null ? (int) maxTextMessageBufferSize.getBytes() : 32768
        );
    }

    @BQConfigProperty("Time after which the operation will timeout attempting to send a websocket message for all " +
            "RemoteEndpoints. Zero indicates no timeout. The default is 1 minute.")
    public void setAsyncSendTimeout(Duration asyncSendTimeout) {
        this.asyncSendTimeout = asyncSendTimeout;
    }

    @BQConfigProperty("The default time after which any web socket session will be closed if it has been inactive. " +
            "A value that is 0 or negative indicates the sessions will never timeout due to inactivity. Default is 5 min.")
    public void setMaxSessionIdleTimeout(Duration maxSessionIdleTimeout) {
        this.maxSessionIdleTimeout = maxSessionIdleTimeout;
    }

    @BQConfigProperty("Sets the default maximum size of incoming binary message that this container will buffer. Default is 32K.")
    public void setMaxBinaryMessageBufferSize(Bytes maxBinaryMessageBufferSize) {
        this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
    }

    @BQConfigProperty("The default maximum size of incoming text message that this container will buffer. Default is 32K.")
    public void setMaxTextMessageBufferSize(Bytes maxTextMessageBufferSize) {
        this.maxTextMessageBufferSize = maxTextMessageBufferSize;
    }
}
