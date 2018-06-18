/**
 *  Licensed to ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.jetty.server;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

public class ServerLifecycleLogger extends AbstractLifeCycle.AbstractLifeCycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLifecycleLogger.class);

    private Collection<ConnectorDescriptor> connectorDescriptors;
    private String context;

    private long t0;

    public ServerLifecycleLogger(Collection<ConnectorDescriptor> connectorDescriptors, String context) {
        this.connectorDescriptors = connectorDescriptors;
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

        if (connectorDescriptors.isEmpty()) {
            LOGGER.info("Started Jetty in {} ms. No connectors configured", t1 - t0);
        }
        if (connectorDescriptors.size() == 1) {
            String url = connectorDescriptors.iterator().next().getUrl(context);
            LOGGER.info("Started Jetty in {} ms. Base URL: {}", t1 - t0, url);
        } else {
            String urls = connectorDescriptors.stream().map(cd -> cd.getUrl(context)).collect(Collectors.joining(", "));
            LOGGER.info("Started Jetty in {} ms. Base URLs: {}", t1 - t0, urls);
        }
    }

}
