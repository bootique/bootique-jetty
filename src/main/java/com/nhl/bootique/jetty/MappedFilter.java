package com.nhl.bootique.jetty;

import java.util.Set;

import javax.servlet.Filter;

/**
 * @since 0.11
 */
public class MappedFilter {

	private Filter filter;
	private Set<String> urlPatterns;

	public MappedFilter(Filter filter, Set<String> urlPatterns) {
		this.filter = filter;
		this.urlPatterns = urlPatterns;
	}
	
	public Filter getFilter() {
		return filter;
	}

	public Set<String> getUrlPatterns() {
		return urlPatterns;
	}
}
