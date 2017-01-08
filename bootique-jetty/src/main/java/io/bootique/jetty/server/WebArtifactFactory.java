package io.bootique.jetty.server;

import io.bootique.annotation.BQConfigProperty;

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
	@BQConfigProperty
	public void setUrlPatterns(Set<String> urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	@BQConfigProperty
	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	protected Map<String, String> getParams(Map<String, String> mappedParams) {

		Map<String, String> params = this.params;
		if (params == null || params.isEmpty()) {
			params = mappedParams;
		}

		if (params == null) {
			params = Collections.emptyMap();
		}
		
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
