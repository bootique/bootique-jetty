/**
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * “License”); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.jetty.v11.instrumented.request;

import io.bootique.jetty.v11.request.RequestMDCItem;
import io.bootique.metrics.mdc.TransactionIdGenerator;
import io.bootique.metrics.mdc.TransactionIdMDC;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;

public class TransactionMDCItem implements RequestMDCItem {

    private final TransactionIdMDC transactionIdMDC;
    private final TransactionIdGenerator idGenerator;

    public TransactionMDCItem(TransactionIdGenerator idGenerator, TransactionIdMDC transactionIdMDC) {
        this.transactionIdMDC = transactionIdMDC;
        this.idGenerator = idGenerator;
    }

    @Override
    public void initMDC(ServletContext sc, ServletRequest request) {
        String id = idGenerator.nextId();
        transactionIdMDC.reset(id);
    }

    @Override
    public void cleanupMDC(ServletContext sc, ServletRequest request) {
        transactionIdMDC.clear();
    }
}
