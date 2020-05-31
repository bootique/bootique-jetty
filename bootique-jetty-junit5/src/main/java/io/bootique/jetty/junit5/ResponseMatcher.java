/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.jetty.junit5;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @since 2.0
 */
public class ResponseMatcher {

    private Response response;
    private String content;

    public ResponseMatcher(Response response) {
        this.response = response;
    }

    public ResponseMatcher assertStatus(int expectedStatus) {
        assertEquals(expectedStatus, response.getStatus());
        return this;
    }

    public ResponseMatcher assertOk() {
        return assertStatus(200);
    }

    public ResponseMatcher assertNotFound() {
        return assertStatus(404);
    }

    public ResponseMatcher assertContent(String expectedContent) {
        assertEquals(expectedContent, getContent());
        return this;
    }

    /**
     * Performs content assertions using a custom assertion checker. E.g. Hamcrest or AssertJ.
     */
    public ResponseMatcher assertContent(Consumer<String> assertionChecker) {
        assertionChecker.accept(getContent());
        return this;
    }

    public ResponseMatcher assertContentType(MediaType expectedType) {
        String header = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
        assertNotNull(header, "'Content-Type' is undefined");

        MediaType actualType = MediaType.valueOf(header);
        assertTrue(expectedType.isCompatible(actualType), () -> "'" + header + "' is not compatible with '" + expectedType + "'");

        return this;
    }

    public ResponseMatcher assertContentType(String expectedType) {
        String header = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
        assertNotNull(header, "'Content-Type' is undefined");

        MediaType expectedMT = MediaType.valueOf(expectedType);
        MediaType actualType = MediaType.valueOf(header);
        assertTrue(expectedMT.isCompatible(actualType), () -> "'" + header + "' is not compatible with '" + expectedType + "'");

        return this;
    }

    public ResponseMatcher assertHeader(String headerName, String expectedValue) {
        String actualValue = response.getHeaderString(headerName);
        assertNotNull(actualValue, "'" + headerName + "' is undefined");
        assertEquals(expectedValue, actualValue);
        return this;
    }

    protected String getContent() {

        // cache content locally in case we need to do multiple assertions
        if (content == null) {
            content = response.readEntity(String.class);
        }

        return content;
    }
}
