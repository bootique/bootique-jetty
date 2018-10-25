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

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.bootique.ModuleExtender;
import io.bootique.jetty.server.ServletContextHandlerExtender;
import org.eclipse.jetty.servlet.DefaultServlet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides API to contribute custom extensions to {@link JettyModule}. This class is a syntactic sugar for Guice
 * MapBinder and Multibinder.
 *
 * @since 0.20
 */
public class JettyModuleExtender extends ModuleExtender<JettyModuleExtender> {


    private Multibinder<Filter> filters;
    private Multibinder<Servlet> servlets;
    private Multibinder<EventListener> listeners;

    private Multibinder<MappedFilter> mappedFilters;
    private Multibinder<MappedServlet> mappedServlets;
    private Multibinder<MappedListener> mappedListeners;

    private Multibinder<ServletContextHandlerExtender> contextHandlerExtenders;

    public JettyModuleExtender(Binder binder) {
        super(binder);
    }

    /**
     * Should be called by owning Module to initialize all contribution maps and collections. Failure to call this
     * method may result in injection failures for empty maps and collections.
     *
     * @return this extender instance.
     */
    @Override
    public JettyModuleExtender initAllExtensions() {
        contributeFilters();
        contributeServlets();
        contributeListeners();

        contributeMappedFilters();
        contributeMappedServlets();
        contributeMappedListeners();

        contributeContextHandlerExtenders();

        return this;
    }

    public JettyModuleExtender addListener(EventListener listener) {
        contributeListeners().addBinding().toInstance(listener);
        return this;
    }

    public JettyModuleExtender addListener(Class<? extends EventListener> listenerType) {
        contributeListeners().addBinding().to(listenerType);
        return this;
    }

    /**
     * @param mappedListener
     * @param <T>
     * @return
     * @since 0.25
     */
    public <T extends EventListener> JettyModuleExtender addMappedListener(MappedListener<T> mappedListener) {
        contributeMappedListeners().addBinding().toInstance(mappedListener);
        return this;
    }

    /**
     * @param mappedListenerKey
     * @param <T>
     * @return
     * @since 0.25
     */
    public <T extends EventListener> JettyModuleExtender addMappedListener(Key<MappedListener<T>> mappedListenerKey) {
        contributeMappedListeners().addBinding().to(mappedListenerKey);
        return this;
    }

    /**
     * @param mappedListenerType
     * @param <T>
     * @return
     * @since 0.25
     */
    public <T extends EventListener> JettyModuleExtender addMappedListener(TypeLiteral<MappedListener<T>> mappedListenerType) {
        return addMappedListener(Key.get(mappedListenerType));
    }

    public JettyModuleExtender addStaticServlet(String name, String... urlPatterns) {
        return addServlet(new DefaultServlet(), name, urlPatterns);
    }

    public JettyModuleExtender useDefaultServlet() {
        return addStaticServlet("default", "/");
    }

    /**
     * Adds a servlet of the specified type to the set of Jetty servlets. "servletType" must be annotated with
     * {@link javax.servlet.annotation.WebServlet}. Otherwise it should be mapped via other add(Mapped)Servlet methods,
     * where you can explicitly specify URL patterns.
     *
     * @param servletType a class of the servlet to map.
     * @return this extender instance.
     */
    public JettyModuleExtender addServlet(Class<? extends Servlet> servletType) {
        contributeServlets().addBinding().to(servletType);
        return this;
    }

    public JettyModuleExtender addServlet(Servlet servlet, String name, String... urlPatterns) {

        Set<String> urlPatternsSet = new HashSet<>();
        if (urlPatterns != null) {
            Collections.addAll(urlPatternsSet, urlPatterns);
        }

        return addMappedServlet(new MappedServlet<>(servlet, urlPatternsSet, name));
    }

    public <T extends Servlet> JettyModuleExtender addMappedServlet(MappedServlet<T> mappedServlet) {
        contributeMappedServlets().addBinding().toInstance(mappedServlet);
        return this;
    }

    public <T extends Servlet> JettyModuleExtender addMappedServlet(Key<MappedServlet<T>> mappedServletKey) {
        contributeMappedServlets().addBinding().to(mappedServletKey);
        return this;
    }

    public <T extends Servlet> JettyModuleExtender addMappedServlet(TypeLiteral<MappedServlet<T>> mappedServletType) {
        return addMappedServlet(Key.get(mappedServletType));
    }

    /**
     * Adds a filter of the specified type to the set of Jetty filters. "filterType" must be annotated with
     * {@link javax.servlet.annotation.WebFilter}. Otherwise it should be mapped via other add(Mapped)Filter methods,
     * where you can explicitly specify URL patterns, ordering, etc.
     *
     * @param filterType a class of the filter to map.
     * @return this extender instance.
     */
    public JettyModuleExtender addFilter(Class<? extends Filter> filterType) {
        contributeFilters().addBinding().to(filterType);
        return this;
    }

    public JettyModuleExtender addFilter(Filter filter, String name, int order, String... urlPatterns) {

        Set<String> urlPatternsSet = new HashSet<>();
        if (urlPatterns != null) {
            Collections.addAll(urlPatternsSet, urlPatterns);
        }

        return addMappedFilter(new MappedFilter<>(filter, urlPatternsSet, name, order));
    }

    public <T extends Filter> JettyModuleExtender addMappedFilter(MappedFilter<T> mappedFilter) {
        contributeMappedFilters().addBinding().toInstance(mappedFilter);
        return this;
    }

    public <T extends Filter> JettyModuleExtender addMappedFilter(Key<MappedFilter<T>> mappedFilterKey) {
        contributeMappedFilters().addBinding().to(mappedFilterKey);
        return this;
    }

    public <T extends Filter> JettyModuleExtender addMappedFilter(TypeLiteral<MappedFilter<T>> mappedFilterType) {
        return addMappedFilter(Key.get(mappedFilterType));
    }

    /**
     * Registers an extender of the Jetty {@link org.eclipse.jetty.servlet.ServletContextHandler}. This is a low-level
     * extension point that allows to install some Jetty extensions like the WebSockets engine. This should be usually
     * of no interest to regular bootique-jetty users.
     *
     * @param extender an "extender" object that can customize {@link org.eclipse.jetty.servlet.ServletContextHandler}.
     * @return this extender instance
     * @since 1.0.RC1
     */
    public JettyModuleExtender addContextHandlerExtender(ServletContextHandlerExtender extender) {
        contributeContextHandlerExtenders().addBinding().toInstance(extender);
        return this;
    }

    /**
     * Registers an extender of the Jetty {@link org.eclipse.jetty.servlet.ServletContextHandler}. This is a low-level
     * extension point that allows to install some Jetty extensions like the WebSockets engine. This should be usually
     * of no interest to regular bootique-jetty users.
     *
     * @param type a class of an "extender" object that can customize
     *             {@link org.eclipse.jetty.servlet.ServletContextHandler}.
     * @return this extender instance
     * @since 1.0.RC1
     */
    public JettyModuleExtender addContextHandlerExtender(Class<? extends ServletContextHandlerExtender> type) {
        contributeContextHandlerExtenders().addBinding().to(type);
        return this;
    }

    protected Multibinder<Filter> contributeFilters() {
        return filters != null ? filters : (filters = newSet(Filter.class));
    }

    protected Multibinder<Servlet> contributeServlets() {
        return servlets != null ? servlets : (servlets = newSet(Servlet.class));
    }

    protected Multibinder<EventListener> contributeListeners() {
        return listeners != null ? listeners : (listeners = newSet(EventListener.class));
    }

    protected Multibinder<MappedFilter> contributeMappedFilters() {
        return mappedFilters != null ? mappedFilters : (mappedFilters = newSet(MappedFilter.class));
    }

    protected Multibinder<MappedServlet> contributeMappedServlets() {
        return mappedServlets != null ? mappedServlets : (mappedServlets = newSet(MappedServlet.class));
    }

    protected Multibinder<MappedListener> contributeMappedListeners() {
        return mappedListeners != null ? mappedListeners : (mappedListeners = newSet(MappedListener.class));
    }

    protected Multibinder<ServletContextHandlerExtender> contributeContextHandlerExtenders() {
        return contextHandlerExtenders != null
                ? contextHandlerExtenders
                : (contextHandlerExtenders = newSet(ServletContextHandlerExtender.class));
    }

}
