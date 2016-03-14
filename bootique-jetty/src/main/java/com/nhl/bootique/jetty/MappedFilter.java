package com.nhl.bootique.jetty;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;

/**
 * @since 0.11
 */
public class MappedFilter {

	private Filter filter;
	private Set<String> urlPatterns;
	private String name;
	private Map<String, String> initParams;
	private int order;

	public MappedFilter(Filter filter, Set<String> urlPatterns, int order) {
		this(filter, urlPatterns, null, Collections.emptyMap(), order);
	}

	/**
	 * @since 0.13
	 * @param filter
	 *            a filter to install inside of Jetty.
	 * @param urlPatterns
	 *            URL patterns that this filter will respond to.
	 * @param name
	 *            filter name.
	 * @param initParams
	 *            parameters passed to the filter in
	 *            {@link Filter#init(javax.servlet.FilterConfig)}.
	 * @param order
	 *            an order of the filter among all the filters in a given app.
	 */
	public MappedFilter(Filter filter, Set<String> urlPatterns, String name, Map<String, String> initParams,
			int order) {
		this.filter = filter;
		this.urlPatterns = urlPatterns;
		this.order = order;
		this.name = name;
		this.initParams = initParams;
	}

	public Filter getFilter() {
		return filter;
	}

	public Set<String> getUrlPatterns() {
		return urlPatterns;
	}

	public int getOrder() {
		return order;
	}

	/**
	 * @since 0.13
	 * @return a potentially empty map of filter initialization parameters.
	 */
	public Map<String, String> getInitParams() {
		return initParams;
	}

	/**
	 * @since 0.13
	 * @return an optional filter name.
	 */
	public String getName() {
		return name;
	}
}
