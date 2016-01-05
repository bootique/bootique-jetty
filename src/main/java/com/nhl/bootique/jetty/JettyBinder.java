package com.nhl.bootique.jetty;

import java.lang.annotation.Annotation;

import javax.servlet.Servlet;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;

/**
 * @since 0.10
 */
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

	public void servlet(Servlet servlet, String path) {
		servletsBinder().addBinding().toInstance(new MappedServlet(servlet, path));
	}

	public void servlet(Class<? extends Annotation> mappedServletAnnotation) {
		servletsBinder().addBinding().to(Key.get(MappedServlet.class, mappedServletAnnotation));
	}
}
