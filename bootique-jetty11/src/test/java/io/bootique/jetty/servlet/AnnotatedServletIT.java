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

package io.bootique.jetty.servlet;

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNull;

@BQTest
public class AnnotatedServletIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Runnable assertion;

    @AfterEach
    public void after() {
        assertion = null;
    }

    @Test
    public void testServletContainerState() {
        testFactory.app("-s").module(new ServletModule()).run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        assertNull(assertion);

        base.path("/b").request().get();
        Objects.requireNonNull(assertion).run();
        assertion = null;

        base.path("/b/1").request().get();
        Objects.requireNonNull(assertion).run();
        assertion = null;

        base.path("/b/2").request().get();
        Objects.requireNonNull(assertion).run();
    }

    @Test
    public void testConfig_Override() {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/servlet/AnnotatedServletIT1.yml")
                .module(new ServletModule())
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");

        assertNull(assertion);

        base.path("/b").request().get();
        assertNull(assertion);

        base.path("/c").request().get();
        Objects.requireNonNull(assertion).run();
        assertion = null;
    }

    class ServletModule implements BQModule {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addServlet(AnnotatedServlet.class);
        }

        @Provides
        AnnotatedServlet createAnnotatedServlet() {
            return new AnnotatedServlet();
        }

        @WebServlet(name = "s1", urlPatterns = "/b/*")
        class AnnotatedServlet extends HttpServlet {

            private static final long serialVersionUID = -8896839263652092254L;

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                assertion = () -> {
                };
            }
        }
    }

}
