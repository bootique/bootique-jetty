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
import io.bootique.di.DIRuntimeException;
import io.bootique.di.Provides;
import io.bootique.jetty.JettyModule;
import io.bootique.test.junit5.BQTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class NotAnnotatedServletIT {

    @RegisterExtension
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    @Test
    public void testServletContainerState() {
        assertThrows(DIRuntimeException.class, () -> testFactory.app("-s").module(new ServletModule()).run());
    }

    class ServletModule implements BQModule {

        @Override
        public void configure(Binder binder) {
            JettyModule.extend(binder).addServlet(NotAnnotatedServlet.class);
        }

        @Provides
        NotAnnotatedServlet createAnnotatedServlet() {
            return new NotAnnotatedServlet();
        }

        class NotAnnotatedServlet extends HttpServlet {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

            }
        }
    }

}
