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
import io.bootique.ModuleExtender;
import io.bootique.di.*;
import io.bootique.jetty.request.RequestMDCItem;
import io.bootique.jetty.server.ServletContextHandlerExtender;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides API to contribute custom extensions to {@link JettyModule}. This class is a syntactic sugar for Bootique
 * MapBuilder and SetBuilder.
 */
public class JettyModuleExtender extends ModuleExtender<JettyModuleExtender> {

    private SetBuilder<Filter> filters;
    private SetBuilder<Servlet> servlets;
    private SetBuilder<EventListener> listeners;

    private SetBuilder<MappedFilter> mappedFilters;
    private SetBuilder<MappedServlet> mappedServlets;
    private SetBuilder<MappedListener> mappedListeners;
    private MapBuilder<String, RequestMDCItem> mdcItems;

    private SetBuilder<ServletContextHandlerExtender> contextHandlerExtenders;

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

        contributeMdcItems();

        return this;
    }

    /**
     * @since 2.0
     */
    public JettyModuleExtender addRequestMDCItem(String mdcKey, RequestMDCItem item) {
        contributeMdcItems().putInstance(mdcKey, item);
        return this;
    }

    /**
     * @since 2.0
     */
    public JettyModuleExtender addRequestMDCItem(String mdcKey, Class<? extends RequestMDCItem> itemType) {
        contributeMdcItems().put(mdcKey, itemType);
        return this;
    }

    public JettyModuleExtender addListener(EventListener listener) {
        contributeListeners().addInstance(listener);
        return this;
    }

    public JettyModuleExtender addListener(Class<? extends EventListener> listenerType) {
        contributeListeners().add(listenerType);
        return this;
    }

    /**
     * @return this extender instance
     */
    public <T extends EventListener> JettyModuleExtender addMappedListener(MappedListener<T> mappedListener) {
        contributeMappedListeners().addInstance(mappedListener);
        return this;
    }

    /**
     * @return this extender instance
     */
    public <T extends EventListener> JettyModuleExtender addMappedListener(Key<MappedListener<T>> mappedListenerKey) {
        contributeMappedListeners().add(mappedListenerKey);
        return this;
    }

    /**
     * @return this extender instance
     */
    public <T extends EventListener> JettyModuleExtender addMappedListener(TypeLiteral<MappedListener<T>> mappedListenerType) {
        return addMappedListener(Key.get(mappedListenerType));
    }

    /**
     * @deprecated in favor of {@link #addMappedServlet(MappedServlet)} with {@link MappedServlet#ofStatic(String...)}
     */
    @Deprecated(since = "3.0", forRemoval = true)
    public JettyModuleExtender useDefaultServlet() {
        return addMappedServlet(MappedServlet.ofStatic("/").name("default").build());
    }

    /**
     * Sets an init parameter of a named servlet. Same thing can be achieved via configuration.
     *
     * @return this extender instance
     * @since 2.0
     */
    public JettyModuleExtender setServletParam(String servletName, String propertyName, String propertyValue) {
        BQCoreModule.extend(binder).setProperty("bq.jetty.servlets." + servletName + ".params." + propertyName, propertyValue);
        return this;
    }

    /**
     * Adds a servlet of the specified type to the set of Jetty servlets. "servletType" must be annotated with
     * {@link jakarta.servlet.annotation.WebServlet}. Otherwise it should be mapped via other add(Mapped)Servlet methods,
     * where you can explicitly specify URL patterns.
     *
     * @param servletType a class of the servlet to map.
     * @return this extender instance.
     */
    public JettyModuleExtender addServlet(Class<? extends Servlet> servletType) {
        contributeServlets().add(servletType);
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
        contributeMappedServlets().addInstance(mappedServlet);
        return this;
    }

    public <T extends Servlet> JettyModuleExtender addMappedServlet(Key<MappedServlet<T>> mappedServletKey) {
        contributeMappedServlets().add(mappedServletKey);
        return this;
    }

    public <T extends Servlet> JettyModuleExtender addMappedServlet(TypeLiteral<MappedServlet<T>> mappedServletType) {
        return addMappedServlet(Key.get(mappedServletType));
    }

    /**
     * Adds a filter of the specified type to the set of Jetty filters. "filterType" must be annotated with
     * {@link jakarta.servlet.annotation.WebFilter}. Otherwise it should be mapped via other add(Mapped)Filter methods,
     * where you can explicitly specify URL patterns, ordering, etc.
     *
     * @param filterType a class of the filter to map.
     * @return this extender instance.
     */
    public JettyModuleExtender addFilter(Class<? extends Filter> filterType) {
        contributeFilters().add(filterType);
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
        contributeMappedFilters().addInstance(mappedFilter);
        return this;
    }

    public <T extends Filter> JettyModuleExtender addMappedFilter(Key<MappedFilter<T>> mappedFilterKey) {
        contributeMappedFilters().add(mappedFilterKey);
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
     */
    public JettyModuleExtender addContextHandlerExtender(ServletContextHandlerExtender extender) {
        contributeContextHandlerExtenders().addInstance(extender);
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
     */
    public JettyModuleExtender addContextHandlerExtender(Class<? extends ServletContextHandlerExtender> type) {
        contributeContextHandlerExtenders().add(type);
        return this;
    }

    protected SetBuilder<Filter> contributeFilters() {
        return filters != null ? filters : (filters = newSet(Filter.class));
    }

    protected SetBuilder<Servlet> contributeServlets() {
        return servlets != null ? servlets : (servlets = newSet(Servlet.class));
    }

    protected SetBuilder<EventListener> contributeListeners() {
        return listeners != null ? listeners : (listeners = newSet(EventListener.class));
    }

    protected SetBuilder<MappedFilter> contributeMappedFilters() {
        return mappedFilters != null ? mappedFilters : (mappedFilters = newSet(MappedFilter.class));
    }

    protected SetBuilder<MappedServlet> contributeMappedServlets() {
        return mappedServlets != null ? mappedServlets : (mappedServlets = newSet(MappedServlet.class));
    }

    protected SetBuilder<MappedListener> contributeMappedListeners() {
        return mappedListeners != null ? mappedListeners : (mappedListeners = newSet(MappedListener.class));
    }

    protected MapBuilder<String, RequestMDCItem> contributeMdcItems() {
        return mdcItems != null ? mdcItems : (mdcItems = newMap(String.class, RequestMDCItem.class));
    }

    protected SetBuilder<ServletContextHandlerExtender> contributeContextHandlerExtenders() {
        return contextHandlerExtenders != null
                ? contextHandlerExtenders
                : (contextHandlerExtenders = newSet(ServletContextHandlerExtender.class));
    }

}
