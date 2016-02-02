package com.nhl.bootique.jetty;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;

/**
 * @since 0.10
 * @deprecated since 0.11 in favor of static binding methods on
 *             {@link JettyModule}:
 *             {@link JettyModule#contributeServlets(Binder)},
 *             {@link JettyModule#contributeFilters(Binder)}.
 */
@Deprecated
public class JettyBinder {

	public static JettyBinder contributeTo(Binder binder) {
		return new JettyBinder(binder);
	}

	private Binder binder;

	JettyBinder(Binder binder) {
		this.binder = binder;
	}

	Multibinder<MappedServlet> servletsBinder() {
		return Multibinder.newSetBinder(binder, MappedServlet.class);
	}

	Multibinder<MappedFilter> filtersBinder() {
		return Multibinder.newSetBinder(binder, MappedFilter.class);
	}

	public void servlet(Servlet servlet, String... urlPatterns) {
		Set<String> urlSet = new HashSet<>(Arrays.asList(urlPatterns));
		servletsBinder().addBinding().toInstance(new MappedServlet(servlet, urlSet));
	}

	/**
	 * Adds a {@link MappedServlet} defined elsewhere in the DI container to the
	 * DI servlets collection.
	 * 
	 * @param mappedServletAnnotation
	 *            an annotation type for the MappedServlet DI key.
	 */
	public void servlet(Class<? extends Annotation> mappedServletAnnotation) {
		servletsBinder().addBinding().to(Key.get(MappedServlet.class, mappedServletAnnotation));
	}

	/**
	 * @since 0.11
	 */
	public void filter(Filter filter, int order, String... urlPatterns) {
		Set<String> urlSet = new HashSet<>(Arrays.asList(urlPatterns));
		filtersBinder().addBinding().toInstance(new MappedFilter(filter, urlSet, order));
	}

	/**
	 * Adds a {@link MappedFilter} defined elsewhere in the DI container to the
	 * DI filters collection.
	 * 
	 * @param mappedServletAnnotation
	 *            an annotation type for the MappedServlet DI key.
	 * @since 0.11
	 */
	public void filter(Class<? extends Annotation> mappedFilterAnnotation) {
		filtersBinder().addBinding().to(Key.get(MappedFilter.class, mappedFilterAnnotation));
	}

}
