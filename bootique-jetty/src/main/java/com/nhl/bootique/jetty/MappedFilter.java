package com.nhl.bootique.jetty;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;

/**
 * @since 0.11
 */
public class MappedFilter extends MappedWebArtifact<Filter> {

	private int order;

	public MappedFilter(Filter filter, Set<String> urlPatterns, int order) {
		this(filter, urlPatterns, null, order);
	}

	/**
	 * @since 0.13
	 * @param filter
	 *            a filter to install inside of Jetty.
	 * @param urlPatterns
	 *            URL patterns that this filter will respond to.
	 * @param name
	 *            filter name. If null, Jetty will assign its own name.
	 * @param order
	 *            an order of the filter among all the filters in a given app.
	 *            If two filters match the same request, filter with lower
	 *            ordering will be an outer filter and will be called first.
	 */
	public MappedFilter(Filter filter, Set<String> urlPatterns, String name, int order) {
		this(filter, urlPatterns, name, Collections.emptyMap(), order);
	}

	/**
	 * @since 0.13
	 * @param filter
	 *            a filter to install inside of Jetty.
	 * @param urlPatterns
	 *            URL patterns that this filter will respond to.
	 * @param name
	 *            filter name. If null, Jetty will assign its own name.
	 * @param params
	 *            filter init parameters map.
	 * @param order
	 *            an order of the filter among all the filters in a given app.
	 *            If two filters match the same request, filter with lower
	 *            ordering will be an outer filter and will be called first.
	 */
	public MappedFilter(Filter filter, Set<String> urlPatterns, String name, Map<String, String> params, int order) {
		super(filter, urlPatterns, name, params);
		this.order = order;
	}

	public Filter getFilter() {
		return getArtifact();
	}

	/**
	 * Returns filter relative ordering. If two filters match the same request,
	 * the filter with lower ordering will wrap the filter with higher ordering.
	 * 
	 * @return filter relative ordering.
	 */
	public int getOrder() {
		return order;
	}

}
