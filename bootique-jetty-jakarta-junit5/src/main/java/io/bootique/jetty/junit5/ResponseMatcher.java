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

import io.bootique.resource.ResourceFactory;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @since 3.0.M1
 */
public class ResponseMatcher {

    private final Response response;
    private String content;
    private byte[] binContent;

    public ResponseMatcher(Response response) {
        this.response = response;
    }

    public ResponseMatcher assertStatus(int expectedStatus) {
        assertEquals(expectedStatus, response.getStatus());
        return this;
    }

    public ResponseMatcher assertStatus(int expectedStatus, String message) {
        assertEquals(expectedStatus, response.getStatus(), message);
        return this;
    }

    public ResponseMatcher assertOk() {
        return assertStatus(200);
    }

    public ResponseMatcher assertOk(String message) {
        return assertStatus(200, message);
    }

    public ResponseMatcher assertCreated() {
        return assertStatus(201);
    }

    public ResponseMatcher assertCreated(String message) {
        return assertStatus(201, message);
    }

    public ResponseMatcher assertBadRequest() {
        return assertStatus(400);
    }

    public ResponseMatcher assertBadRequest(String message) {
        return assertStatus(400, message);
    }

    public ResponseMatcher assertUnauthorized() {
        return assertStatus(401);
    }

    public ResponseMatcher assertUnauthorized(String message) {
        return assertStatus(401, message);
    }

    public ResponseMatcher assertForbidden() {
        return assertStatus(403);
    }

    public ResponseMatcher assertForbidden(String message) {
        return assertStatus(403, message);
    }

    public ResponseMatcher assertNotFound() {
        return assertStatus(404);
    }

    public ResponseMatcher assertNotFound(String message) {
        return assertStatus(404, message);
    }

    public ResponseMatcher assertContent(String expectedContent) {
        assertEquals(expectedContent, getContentAsString());
        return this;
    }

    public ResponseMatcher assertContent(String expectedContent, String message) {
        assertEquals(expectedContent, getContentAsString(), message);
        return this;
    }

    /**
     * Performs content assertions using a custom assertion checker. E.g. Hamcrest or AssertJ.
     */
    public ResponseMatcher assertContent(Consumer<String> assertionChecker) {
        assertionChecker.accept(getContentAsString());
        return this;
    }

    public ResponseMatcher assertContent(ResourceFactory expectedContentSource) {
        return assertContent(resourceContent(expectedContentSource, "UTF-8"));
    }

    protected String resourceContent(ResourceFactory resource, String encoding) {
        URL url = resource.getUrl();

        try (InputStream in = url.openStream()) {

            // read as bytes to preserve line breaks
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                out.write(data, 0, nRead);
            }

            return new String(out.toByteArray(), encoding);

        } catch (IOException e) {
            throw new RuntimeException("Error reading resource contents: " + url, e);
        }
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

    public String getContentAsString() {

        if (binContent != null) {
            throw new IllegalStateException("Response already read as binary");
        }

        // cache read content, as Response won't allow to read it twice..
        if (content == null) {
            content = response.readEntity(String.class);
        }

        return content;
    }

    public byte[] getContentAsBytes() {
        if (content != null) {
            throw new IllegalStateException("Response already read as String");
        }

        // cache read content, as Response won't allow to read it twice..
        if (binContent == null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try (InputStream in = response.readEntity(InputStream.class)) {

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
                    out.write(buffer, 0, read);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.binContent = out.toByteArray();
        }

        return binContent;
    }
}
