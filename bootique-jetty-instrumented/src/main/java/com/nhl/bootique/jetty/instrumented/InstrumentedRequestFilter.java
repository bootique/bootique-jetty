package com.nhl.bootique.jetty.instrumented;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * A binding annotation for a servlet filter that serves to collection request
 * metrics. By default such filter is added with a low ordering of -100 to wrap
 * all other filters.
 * 
 * @since 0.15
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface InstrumentedRequestFilter {

}
