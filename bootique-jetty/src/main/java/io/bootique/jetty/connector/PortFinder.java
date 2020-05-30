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

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * Find a port on a host machine that is not taken.
 */
// high-level idea is inspired by Spring SocketUtils
class PortFinder {

    static final int MIN_PORT = 1024;
    static final int MAX_PORT = 65535;

    static int findAvailablePort(String host) {
        return findAvailablePort(host, MIN_PORT, MAX_PORT);
    }

    static int findAvailablePort(String host, int minPort, int maxPort) {

        if (minPort <= 0) {
            throw new IllegalArgumentException("Illegal min port: " + minPort);
        }

        if (maxPort <= 0) {
            throw new IllegalArgumentException("Illegal max port: " + minPort);
        }

        if (minPort > maxPort) {
            throw new IllegalArgumentException("Min port must be <= than max port: " + minPort + ", " + maxPort);
        }

        Random rnd = new Random();
        InetAddress hostAddress = resolveHost(host);
        int portRange = maxPort - minPort + 1;

        // limit to a large, but finite number of attempts
        for (int i = 0; i < 10000; i++) {

            int port = minPort + rnd.nextInt(portRange);
            if (isPortAvailable(port, hostAddress)) {
                return port;
            }
        }

        throw new RuntimeException("Failed to find an available TCP port between " + minPort + " and " + maxPort);
    }

    static InetAddress resolveHost(String host) {
        String hostname = host != null && !host.isEmpty() ? host : "127.0.0.1";
        try {
            return InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error resolving localhost", e);
        }
    }

    static boolean isPortAvailable(int port, InetAddress localhost) {
        try {
            ServerSocketFactory.getDefault().createServerSocket(port, 1, localhost).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
