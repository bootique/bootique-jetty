package com.nhl.bootique.jetty;

import java.util.Set;

import javax.servlet.Filter;

/**
 * @since 0.11
 */
public class MappedFilter {

	private Filter filter;
	private Set<String> urlPatterns;
	private int order;

	public MappedFilter(Filter filter, Set<String> urlPatterns, int order) {
		this.filter = filter;
		this.urlPatterns = urlPatterns;
		this.order = order;
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
}
