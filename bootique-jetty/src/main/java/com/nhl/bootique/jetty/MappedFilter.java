package com.nhl.bootique.jetty;

import java.util.Set;

import javax.servlet.Filter;

/**
 * @since 0.11
 */
public class MappedFilter {

	private Filter filter;
	private Set<String> urlPatterns;
	private String name;
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
		this.filter = filter;
		this.urlPatterns = urlPatterns;
		this.order = order;
		this.name = name;
	}

	public Filter getFilter() {
		return filter;
	}

	public Set<String> getUrlPatterns() {
		return urlPatterns;
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

	/**
	 * @since 0.13
	 * @return an optional filter name.
	 */
	public String getName() {
		return name;
	}
}
