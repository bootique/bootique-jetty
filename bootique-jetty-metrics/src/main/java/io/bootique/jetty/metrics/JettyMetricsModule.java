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

package io.bootique.jetty.metrics;

import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.di.TypeLiteral;
import io.bootique.jetty.JettyModule;
import io.bootique.jetty.MappedServlet;
import io.bootique.metrics.health.HealthCheckRegistry;

import javax.inject.Singleton;
import java.util.Collections;

public class JettyMetricsModule implements BQModule {

    @Override
    public void configure(Binder binder) {
        JettyModule.extend(binder).addMappedServlet(new TypeLiteral<MappedServlet<HealthCheckServlet>>() {
        });
    }

    @Singleton
    @Provides
    MappedServlet<HealthCheckServlet> provideHealthCheckServlet(HealthCheckRegistry registry) {
        HealthCheckServlet servlet = new HealthCheckServlet(registry);
        return new MappedServlet<>(servlet, Collections.singleton("/health"), "health");
    }
}
