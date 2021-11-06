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
package io.bootique.jetty.connector;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortFinderTest {

    @Test
    public void findAvailablePort() {
        int port = PortFinder.findAvailablePort(null);
        assertTrue(port >= PortFinder.MIN_PORT);
        assertTrue(port <= PortFinder.MAX_PORT);
    }

    @Test
    public void findAvailablePortInRange() {
        int port = PortFinder.findAvailablePort(null, 21000, 21005);
        assertTrue(port >= 21000);
        assertTrue(port <= 21005);
    }

    @RepeatedTest(10)
    public void findAvailablePort_SkipUnavailable() throws IOException {

        // block a port and ensure it is skipped by finder
        int blockedPort = 21002;
        int minPort = blockedPort - 1;
        int maxPort = blockedPort + 1;

        InetAddress localhost = InetAddress.getByName("localhost");
        try (ServerSocket s = ServerSocketFactory.getDefault().createServerSocket(blockedPort, 1, localhost)) {
            int port = PortFinder.findAvailablePort("localhost", minPort, maxPort);
            assertTrue(port >= minPort);
            assertTrue(port <= maxPort);
            assertNotEquals(blockedPort, port);
        }
    }
}
