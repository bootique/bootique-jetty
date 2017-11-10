package io.bootique.jetty;

import java.util.EventListener;

/**
 * A wrapper around servlet specification listener object that defines registration order of the listener.
 *
 * @param <T>
 * @since 0.25
 */
public class MappedListener<T extends EventListener> {

    private T listener;
    private int order;

    public MappedListener(T listener, int order) {
        this.listener = listener;
        this.order = order;
    }

    public T getListener() {
        return listener;
    }

    public int getOrder() {
        return order;
    }
}
