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
package io.bootique.jetty.docs;

import io.bootique.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import jakarta.servlet.http.HttpServlet;

import javax.inject.Singleton;
import java.util.Collections;

public class ModuleWithMappedServletAndDI implements BQModule {

    // tag::bindMappedServlet[]
    @Override
    public void configure(Binder binder) {

        // must use TypeLiteral to identify which kind of MappedServlet<..> to add
        TypeLiteral<MappedServlet<MyServlet>> tl = new TypeLiteral<>() {
        };
        JettyModule.extend(binder).addMappedServlet(tl);
    }

    @Singleton
    @Provides
    MappedServlet<MyServlet> provideMyServlet(MyService1 s1) {
        MyServlet servlet = new MyServlet(s1);
        return new MappedServlet<>(servlet, Collections.singleton("/c"), "myservlet");
    }
    // end::bindMappedServlet[]

    public static class MyServlet extends HttpServlet {

        public MyServlet(MyService1 service1) {
        }
    }

    public static class MyService1 {

    }
}
