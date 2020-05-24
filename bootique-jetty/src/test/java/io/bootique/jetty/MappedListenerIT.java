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

package io.bootique.jetty;

import io.bootique.junit5.BQTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.servlet.Servlet;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MappedListenerIT {

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Servlet mockServlet1;


    @BeforeEach
    public void before() {
        SharedState.reset();
        this.mockServlet1 = mock(Servlet.class);
    }

    @Test
    public void testAddMappedListener_Ordering1() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(mockServlet1, "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 1))
                        .addMappedListener(new MappedListener<>(new RL2(), 2)))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        assertEquals(200, base.path("/").request().get().getStatus());
        assertEquals("_RL1_init_RL2_init_RL2_destroy_RL1_destroy", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_Ordering2() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(mockServlet1, "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 2))
                        .addMappedListener(new MappedListener<>(new RL2(), 1)))
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        assertEquals(200, base.path("/").request().get().getStatus());
        assertEquals("_RL2_init_RL1_init_RL1_destroy_RL2_destroy", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_OrderingVsUnmapped() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(mockServlet1, "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 2))
                        .addListener(new RL3())
                        .addMappedListener(new MappedListener<>(new RL2(), 1))
                )
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        assertEquals(200, base.path("/").request().get().getStatus());
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

    public static class RL1 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL1_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL1_destroy");
        }
    }

    public static class RL2 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL2_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL2_destroy");
        }
    }

    public static class RL3 implements ServletRequestListener {

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
