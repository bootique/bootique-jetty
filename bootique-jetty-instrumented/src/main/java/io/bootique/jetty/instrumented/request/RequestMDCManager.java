/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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

        // do nothing ...

        // not cleaning up the request id at the end of the request is dirty, yet it allows to maintain
        // context beyond the reach of ServletRequestListener (e.g. within the Jetty logger). It gets reset for a given
        // thread in the next 'requestInitialized()' call. This will cause confusion if somebody uses a listener
        // preceding BUSINESS_TX_LISTENER_ORDER
    }
}
