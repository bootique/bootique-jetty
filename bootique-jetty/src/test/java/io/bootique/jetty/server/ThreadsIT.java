/**
 *    Licensed to the ObjectStyle LLC under one
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

import io.bootique.BQRuntime;
import io.bootique.test.junit.BQTestFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThreadsIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testAcceptorSelectorThreads() {

        BQRuntime runtime = testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/threads.yml")
                .autoLoadModules()
                .createRuntime();

        runtime.run();

        Connector[] connectors = runtime.getInstance(Server.class).getConnectors();
        ServerConnector c1 = (ServerConnector) connectors[0];
        assertEquals(3, c1.getAcceptors());
        assertEquals(4, c1.getSelectorManager().getSelectorCount());

        ServerConnector c2 = (ServerConnector) connectors[1];

        // default counts are CPU count-sensitive, so do a sanity check instead of an exact match
        assertTrue(c2.getAcceptors() > 0);
        assertTrue(c2.getSelectorManager().getSelectorCount() > 0);
    }
}
