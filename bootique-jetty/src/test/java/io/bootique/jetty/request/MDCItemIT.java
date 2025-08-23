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
package io.bootique.jetty.request;

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class MDCItemIT {

    @BQApp
    final static BQRuntime app = Bootique.app("--server")
            .autoLoadModules()
            .module(b -> JettyModule.extend(b).addServlet(new MyServlet(), "my", "/*").addRequestMDCItem("xx", Callback.class))
            .createRuntime();

    private static final WebTarget target = ClientBuilder
            .newClient()
            .target("http://localhost:8080/");

    @Test
    public void checkMDC() {
        assertEquals("1:0", target.request().get(String.class));
        assertEquals("2:1", target.request().get(String.class));
        assertEquals("3:2", target.request().get(String.class));
    }

    static class Callback implements RequestMDCItem {

        static final AtomicInteger initMDC = new AtomicInteger(0);
        static final AtomicInteger cleanupMDC = new AtomicInteger(0);

        @Override
        public void initMDC(ServletContext sc, ServletRequest request) {
            initMDC.getAndIncrement();
        }

        @Override
        public void cleanupMDC(ServletContext sc, ServletRequest request) {
            cleanupMDC.getAndIncrement();
        }
    }

    static class MyServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().print(Callback.initMDC.get() + ":" + Callback.cleanupMDC.get());
        }
    }
}

