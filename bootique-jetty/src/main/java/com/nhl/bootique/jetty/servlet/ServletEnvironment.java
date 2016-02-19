package com.nhl.bootique.jetty.servlet;

import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides access to servlet spec objects active at any particular moment.
 * Normally used by the services that operate in a context of a web request.
 * <p>
 * Take extra care to avoid inadvertently caching returned objects as they
 * should not be retained once they go out of scope of Jetty threads.
 * 
 * @since 0.12
 */
public interface ServletEnvironment {

	Optional<ServletContext> context();

	/**
	 * Returns an optional for HttpServletRequest currently in progress. Will
	 * only return a non-empty Optional when invoked within an ongoing request
	 * thread.
	 * 
	 * @return an optional for HttpServletRequest currently in progress.
	 */
	Optional<HttpServletRequest> request();
}
