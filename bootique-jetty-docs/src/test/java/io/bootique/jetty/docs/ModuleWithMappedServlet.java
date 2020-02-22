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

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;

import javax.servlet.http.HttpServlet;
import java.util.Collections;

public class ModuleWithMappedServlet implements BQModule {

    // tag::bindMappedServlet[]
    @Override
    public void configure(Binder binder) {
        MappedServlet mappedServlet = new MappedServlet(
                new MyServlet(),
                Collections.singleton("/c"),
                "myservlet");

        JettyModule.extend(binder).addMappedServlet(mappedServlet);
    }
    // end::bindMappedServlet[]

    public class MyServlet extends HttpServlet {
    }
}
