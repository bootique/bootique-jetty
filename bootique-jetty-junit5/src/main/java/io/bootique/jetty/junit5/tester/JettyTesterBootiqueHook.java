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
package io.bootique.jetty.junit5.tester;

import io.bootique.jetty.server.ConnectorHolder;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @since 2.0
 */
public class JettyTesterBootiqueHook {

    private String context;
    private ConnectorHolder connectorHolder;

    protected void init(String context, ConnectorHolder connectorHolder) {
        checkUnused(connectorHolder);

        this.context = Objects.requireNonNull(context);
        this.connectorHolder = Objects.requireNonNull(connectorHolder);
    }

    private void checkUnused(ConnectorHolder connectorHolder) {
        if (this.connectorHolder != null && this.connectorHolder != connectorHolder) {
            throw new IllegalStateException("ConnectorHolder is already initialized. " +
                    "Likely this JettyTester is already in connected to another BQRuntime. " +
                    "To fix this error use one JettyTester per BQRuntime.");
        }
    }

    public ConnectorHolder getConnectorHolder() {
        assertNotNull(connectorHolder, "ConnectorHolder is not initialized. Not connected to Bootique runtime?");
        return connectorHolder;
    }

    public String getUrl() {
        return getConnectorHolder().getUrl(context);
    }
}
