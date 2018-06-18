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

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jetty.command.ServerCommand;
import io.bootique.jetty.server.MappedFilterFactory;
import io.bootique.jetty.server.MappedServletFactory;
import io.bootique.jetty.server.ServerFactory;
import io.bootique.jetty.servlet.DefaultServletEnvironment;
import io.bootique.jetty.servlet.ServletEnvironment;
import io.bootique.log.BootLogger;
import io.bootique.shutdown.ShutdownManager;
import org.eclipse.jetty.server.Server;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class JettyModule extends ConfigModule {

    public JettyModule(String configPrefix) {
        super(configPrefix);
    }

    public JettyModule() {
    }

    /**
     * Returns an instance of {@link JettyModuleExtender} used by downstream modules to load custom extensions of
     * services declared in the JettyModule. Should be invoked from a downstream Module's "configure" method.
     *
     * @param binder DI binder passed to the Module that invokes this method.
     * @return an instance of {@link JettyModuleExtender} that can be used to load Jetty custom extensions.
     * @since 0.20
     */
    public static JettyModuleExtender extend(Binder binder) {
        return new JettyModuleExtender(binder);
    }

    static int maxOrder(Set<MappedFilter> mappedFilters) {
        return mappedFilters.stream().map(MappedFilter::getOrder).max(Integer::compare).orElse(0);
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
    Server createServer(ServerFactory factory,
                        Set<Servlet> servlets,
                        Set<MappedServlet> mappedServlets,
                        Set<Filter> filters,
                        Set<MappedFilter> mappedFilters,
                        Set<EventListener> listeners,
                        Set<MappedListener> mappedListeners,
                        BootLogger bootLogger,
                        ShutdownManager shutdownManager) {

        Server server = factory.createServer(
                allServlets(servlets, mappedServlets),
                allFilters(filters, mappedFilters),
                allListeners(listeners, mappedListeners));

        shutdownManager.addShutdownHook(() -> {
            bootLogger.trace(() -> "stopping Jetty...");
            server.stop();
        });

        return server;
    }

    private Set<MappedServlet> allServlets(Set<Servlet> servlets, Set<MappedServlet> mappedServlets) {
        if (servlets.isEmpty()) {
            return mappedServlets;
        }

        Set<MappedServlet> mappedServletsClone = new HashSet<>(mappedServlets);
        MappedServletFactory mappedServletFactory = new MappedServletFactory();
        servlets.forEach(servlet -> mappedServletsClone.add(mappedServletFactory.toMappedServlet(servlet)));
        return mappedServletsClone;
    }

    private Set<MappedFilter> allFilters(Set<Filter> filters, Set<MappedFilter> mappedFilters) {
        if (filters.isEmpty()) {
            return mappedFilters;
        }

        // place annotated filters after the last explicit filter.. In any event
        // the actual ordering is unpredictable (depends on the set iteration
        // order).
        AtomicInteger order = new AtomicInteger(maxOrder(mappedFilters) + 1);

        Set<MappedFilter> mappeFiltersClone = new HashSet<>(mappedFilters);
        MappedFilterFactory mappedFilterFactory = new MappedFilterFactory();
        filters.forEach(
                filter -> mappeFiltersClone.add(mappedFilterFactory.toMappedFilter(filter, order.getAndIncrement())));

        return mappeFiltersClone;
    }

    private Set<MappedListener> allListeners(Set<EventListener> listeners, Set<MappedListener> mappedListeners) {
        if (listeners.isEmpty()) {
            return mappedListeners;
        }

        Set<MappedListener> mappedListenersClone = new HashSet<>(mappedListeners);

        //  Integer.MAX_VALUE means placing bare unordered listeners after (== inside) mapped listeners
        listeners.forEach(
                listener -> mappedListenersClone.add(new MappedListener<>(listener, Integer.MAX_VALUE)));

        return mappedListenersClone;
    }

    @Singleton
    @Provides
    ServerFactory providerServerFactory(ConfigurationFactory configFactory) {
        return configFactory.config(ServerFactory.class, configPrefix);
    }
}
