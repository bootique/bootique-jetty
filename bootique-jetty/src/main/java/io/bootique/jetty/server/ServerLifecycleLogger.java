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

package io.bootique.jetty.server;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * @deprecated The users are encouraged to switch to the Jakarta-based flavor
 */
@Deprecated(since = "3.0", forRemoval = true)
public class ServerLifecycleLogger extends AbstractLifeCycle.AbstractLifeCycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLifecycleLogger.class);

    private ServerHolder serverHolder;
    private long t0;

    public ServerLifecycleLogger(ServerHolder serverHolder) {
        this.serverHolder = serverHolder;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
        t0 = System.currentTimeMillis();
        LOGGER.info("Starting jetty...");
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        long time = System.currentTimeMillis() - t0;

        switch (serverHolder.getConnectorsCount()) {
            case 0:
                LOGGER.info("Started Jetty in {} ms. No connectors configured", time);
                return;
            case 1:
                LOGGER.info("Started Jetty in {} ms. Base URL: {}", time, serverHolder.getUrl());
                return;
            default:
                LOGGER.info("Started Jetty in {} ms. Base URLs: {}", time, serverHolder.getUrls().collect(Collectors.joining(", ")));
                return;
        }
    }

}
