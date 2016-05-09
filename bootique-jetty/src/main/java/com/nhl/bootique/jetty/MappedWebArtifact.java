package com.nhl.bootique.jetty;

import java.util.Map;
import java.util.Set;

/**
 * @since 0.15
 */
public abstract class MappedWebArtifact<T> {

	private T artifact;
	private Set<String> urlPatterns;
	private String name;
	private Map<String, String> params;

	public MappedWebArtifact(T artifact, Set<String> urlPatterns, String name, Map<String, String> params) {
		this.artifact = artifact;
		this.name = name;
		this.urlPatterns = urlPatterns;
		this.params = params;
	}

	protected T getArtifact() {
		return artifact;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public Set<String> getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * @since 0.13
	 * @return an optional servlet name.
	 */
	public String getName() {
		return name;
	}

}
