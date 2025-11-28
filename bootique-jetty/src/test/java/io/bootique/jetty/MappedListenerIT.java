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

package io.bootique.jetty;

import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class MappedListenerIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @BeforeEach
    public void before() {
        SharedState.reset();
    }

    @Test
    public void addMappedListener_Ordering1() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(new TestServlet(), "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 1))
                        .addMappedListener(new MappedListener<>(new RL2(), 2)))
                .run();

        try (Client client = ClientBuilder.newClient()) {
            WebTarget base = client.target("http://localhost:8080");
            assertEquals(200, base.path("/").request().get().getStatus());
        }

        assertEquals("_RL1_init_RL2_init_RL2_destroy_RL1_destroy", SharedState.getAndReset());
    }

    @Test
    public void addMappedListener_Ordering2() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(new TestServlet(), "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 2))
                        .addMappedListener(new MappedListener<>(new RL2(), 1)))
                .run();

        try (Client client = ClientBuilder.newClient()) {
            WebTarget base = client.target("http://localhost:8080");
            assertEquals(200, base.path("/").request().get().getStatus());
        }
        assertEquals("_RL2_init_RL1_init_RL1_destroy_RL2_destroy", SharedState.getAndReset());
    }

    @Test
    public void addMappedListener_OrderingVsUnmapped() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(new TestServlet(), "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 2))
                        .addListener(new RL3())
                        .addMappedListener(new MappedListener<>(new RL2(), 1))
                )
                .run();

        try (Client client = ClientBuilder.newClient()) {
            WebTarget base = client.target("http://localhost:8080");
            assertEquals(200, base.path("/").request().get().getStatus());
        }
        assertEquals("_RL2_init_RL1_init_RL3_init_RL3_destroy_RL1_destroy_RL2_destroy", SharedState.getAndReset());
    }

    public static class SharedState {
        private static StringBuilder BUFFER;

        static void reset() {
            BUFFER = new StringBuilder();
        }

        static void append(String value) {
            BUFFER.append(value);
        }

        static String getAndReset() {
            String val = BUFFER.toString();
            reset();
            return val;
        }
    }

    static class TestServlet extends HttpServlet {

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        }
    }

    static class RL1 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL1_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL1_destroy");
        }
    }

    static class RL2 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL2_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL2_destroy");
        }
    }

    static class RL3 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL3_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL3_destroy");
        }
    }
}
