package io.bootique.jetty;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
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
public class JettyModuleExtender {

    private Binder binder;

    private Multibinder<Filter> filters;
    private Multibinder<Servlet> servlets;
    private Multibinder<MappedFilter> mappedFilters;
    private Multibinder<MappedServlet> mappedServlets;
    private Multibinder<EventListener> listeners;

    protected JettyModuleExtender(Binder binder) {
        this.binder = binder;
    }

    /**
     * Should be called by owning Module to initialize all contribution maps and collections. Failure to call this
     * method may result in injection failures for empty maps and collections.
     *
     * @return this extender instance.
     */
    JettyModuleExtender initAllExtensions() {
        contributeFilters();
        contributeServlets();
        contributeMappedFilters();
        contributeMappedServlets();
        contributeListeners();

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

    protected Multibinder<Filter> contributeFilters() {
        if (filters == null) {
            filters = Multibinder.newSetBinder(binder, Filter.class);
        }

        return filters;
    }

    protected Multibinder<Servlet> contributeServlets() {
        if (servlets == null) {
            servlets = Multibinder.newSetBinder(binder, Servlet.class);
        }

        return servlets;
    }

    protected Multibinder<MappedFilter> contributeMappedFilters() {
        if (mappedFilters == null) {
            mappedFilters = Multibinder.newSetBinder(binder, MappedFilter.class);
        }

        return mappedFilters;
    }

    protected Multibinder<MappedServlet> contributeMappedServlets() {
        if (mappedServlets == null) {
            mappedServlets = Multibinder.newSetBinder(binder, MappedServlet.class);
        }

        return mappedServlets;
    }

    protected Multibinder<EventListener> contributeListeners() {
        if (listeners == null) {
            listeners = Multibinder.newSetBinder(binder, EventListener.class);
        }
        return listeners;
    }
}
