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
package io.bootique.jetty.junit5;

import io.bootique.BQRuntime;
import io.bootique.jetty.junit5.JettyTester;
import io.bootique.jetty.server.ServerHolder;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BQTest
public class JettyTester_ModuleReplacingConnectorsIT {

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory();

    @Test
    @DisplayName("Tester should work with BQTestFactory-produced runtimes")
    public void testWithBQTestFactory() {
        JettyTester tester = JettyTester.create();
        BQRuntime runtime = testFactory.app("--server").autoLoadModules().module(tester.moduleReplacingConnectors()).createRuntime();
        runtime.run();
        assertEquals(tester.getPort(), runtime.getInstance(ServerHolder.class).getConnector().getPort());
    }

    @Test
    @DisplayName("Port/URL is not known before the runtime is started")
    public void testRuntimeNotStarted() {
        JettyTester tester = JettyTester.create();
        testFactory.app("--server").autoLoadModules().module(tester.moduleReplacingConnectors()).createRuntime();
        assertThrows(AssertionFailedError.class, () -> tester.getPort());
    }

    @Test
    @DisplayName("Reusing tester for multiple runtimes must be disallowed")
    public void testDisallowMultipleRuntimes() {
        JettyTester tester = JettyTester.create();
        testFactory.app().autoLoadModules().module(tester.moduleReplacingConnectors()).createRuntime();

        assertThrows(IllegalStateException.class, () ->
                testFactory.app().autoLoadModules().module(tester.moduleReplacingConnectors()).createRuntime());
    }
}
