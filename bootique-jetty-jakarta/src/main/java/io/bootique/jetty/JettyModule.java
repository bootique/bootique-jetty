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

import io.bootique.BQCoreModule;
import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.jetty.request.RequestMDCItem;
import io.bootique.jetty.request.RequestMDCManager;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.jetty.server.ServerHolder;
import io.bootique.jetty.servlet.DefaultServletEnvironment;
import io.bootique.jetty.servlet.ServletEnvironment;
import org.eclipse.jetty.server.Server;

import javax.inject.Singleton;
import java.util.Map;
import java.util.logging.Level;

public class JettyModule implements BQModule {

    private static final String CONFIG_PREFIX = "jetty";

    /**
     * Returns an instance of {@link JettyModuleExtender} used by downstream modules to load custom extensions of
     * services declared in the JettyModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JettyModuleExtender} that can be used to load Jetty custom extensions.
     */
    public static JettyModuleExtender extend(Binder binder) {
        return new JettyModuleExtender(binder);
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Integrates Jetty web server")
                .config(CONFIG_PREFIX, ServerFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {

        BQCoreModule.extend(binder)
                .addCommand(ServerCommand.class)
                // make Jetty less verbose ..
                .setLogLevel("org.eclipse.jetty", Level.INFO);

        // trigger extension points creation and init defaults
        JettyModule.extend(binder)
                .initAllExtensions()
                .addListener(DefaultServletEnvironment.class);
    }

    @Singleton
    @Provides
    ServletEnvironment createStateTracker(DefaultServletEnvironment stateImpl) {
        return stateImpl;
    }

    @Singleton
    @Provides
    DefaultServletEnvironment createStateTrackerImpl() {
        return new DefaultServletEnvironment();
    }

    @Singleton
    @Provides
    Server providerServer(ServerHolder holder) {
        return holder.getServer();
    }

    @Singleton
    @Provides
    ServerHolder provideServerHolder(ServerFactory factory) {
        return factory.createServerHolder();
    }

    @Provides
    @Singleton
    RequestMDCManager provideRequestMDCManager(Map<String, RequestMDCItem> items) {
        return new RequestMDCManager(items);
    }

    @Singleton
    @Provides
    ServerFactory providerServerFactory(ConfigurationFactory configFactory) {
        return configFactory.config(ServerFactory.class, CONFIG_PREFIX);
    }
}
