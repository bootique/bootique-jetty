package com.nhl.bootique.jetty.server;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @since 0.15
 */
public abstract class WebArtifactFactory {

	private Map<String, String> params;
	private Set<String> urlPatterns;

	/**
	 * @param urlPatterns
	 *            a set of URL patterns for the servlet created by this factory.
	 */
	public void setUrlPatterns(Set<String> urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	protected Map<String, String> getParams() {
		return params;
	}

	protected Set<String> getUrlPatterns(Set<String> mappedPatterns) {

		Set<String> urlPatterns = this.urlPatterns;
		if (urlPatterns == null || urlPatterns.isEmpty()) {
			urlPatterns = mappedPatterns;
		}

		if (urlPatterns == null) {
			urlPatterns = Collections.emptySet();
		}

		return urlPatterns;
	}

}
