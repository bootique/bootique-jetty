package io.bootique.jetty.instrumented.request;

import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * @since 0.25
 */
public class RequestMDCManager implements ServletRequestListener {

    private TransactionIdMDC transactionIdMDC;
    private TransactionIdGenerator idGenerator;

    public RequestMDCManager(TransactionIdGenerator idGenerator, TransactionIdMDC transactionIdMDC) {
        this.transactionIdMDC = transactionIdMDC;
        this.idGenerator = idGenerator;
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        String id = idGenerator.nextId();
        transactionIdMDC.reset(id);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        transactionIdMDC.clear();
    }
}
