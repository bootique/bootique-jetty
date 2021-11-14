/**
 *  Licensed to ObjectStyle LLC under one
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

package io.bootique.jetty.jakarta.instrumented.unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssertExtras {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertExtras.class);

    public static void assertWithRetry(Runnable test) {

        int maxRetries = 4;
        for (int i = maxRetries; i > 0; i--) {

            try {
                test.run();
                return;
            } catch (AssertionError e) {
                LOGGER.info("Test condition hasn't been reached, will retry {} more time(s)", i);
                try {
                    // sleep a bit longer every time
                    Thread.sleep(100 * (maxRetries - i + 1));
                } catch (InterruptedException e1) {
                }
            }
        }

        // fail for real
        test.run();
    }
}
