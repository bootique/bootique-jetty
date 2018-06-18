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

package io.bootique.jetty.metrics;

import io.bootique.metrics.health.HealthCheckOutcome;
import io.bootique.metrics.health.HealthCheckRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class HealthCheckServletTest {

    private HealthCheckRegistry mockRegistry;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @Before
    public void before() {
        mockRegistry = Mockito.mock(HealthCheckRegistry.class);
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    public void testDoGet_NoHealthChecks() throws ServletException, IOException {

        Mockito.when(mockRegistry.runHealthChecks()).thenReturn(new HashMap<>());

        StringWriter writer = new StringWriter();
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(writer));

        HealthCheckServlet servlet = new HealthCheckServlet(mockRegistry);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setStatus(501);
        assertEquals("! No health checks registered.\n", writer.toString());
    }

    @Test
    public void testDoGet_Success() throws ServletException, IOException {

        Map<String, HealthCheckOutcome> testResults = new HashMap<>();
        testResults.put("h1", HealthCheckOutcome.ok());
        testResults.put("h2", HealthCheckOutcome.ok("I am healthy"));

        Mockito.when(mockRegistry.runHealthChecks()).thenReturn(testResults);

        StringWriter writer = new StringWriter();
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(writer));

        HealthCheckServlet servlet = new HealthCheckServlet(mockRegistry);

        servlet.doGet(mockRequest, mockResponse);

        Mockito.verify(mockResponse).setStatus(200);
        assertEquals("* h1: OK\n"
                + "* h2: OK - I am healthy\n", writer.toString());
    }

    @Test
    public void testDoGet_Mixed() throws ServletException, IOException {

        Map<String, HealthCheckOutcome> testResults = new HashMap<>();
        testResults.put("h1", HealthCheckOutcome.ok());
        testResults.put("h2", HealthCheckOutcome.ok("I am healthy"));
        testResults.put("h3", HealthCheckOutcome.critical("I am not healthy"));

        Mockito.when(mockRegistry.runHealthChecks()).thenReturn(testResults);

        StringWriter writer = new StringWriter();
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(writer));

        HealthCheckServlet servlet = new HealthCheckServlet(mockRegistry);

        servlet.doGet(mockRequest, mockResponse);

        Mockito.verify(mockResponse).setStatus(500);
        assertEquals("* h1: OK\n"
                + "* h2: OK - I am healthy\n"
                + "! h3: CRITICAL - I am not healthy\n", writer.toString());
    }

    @Test
    public void testDoGet_StackTrace() throws ServletException, IOException {

        Map<String, HealthCheckOutcome> testResults = new HashMap<>();
        try {
            throw new RuntimeException("Test exception");
        } catch (RuntimeException e) {
            testResults.put("h4", HealthCheckOutcome.critical(e));
        }

        Mockito.when(mockRegistry.runHealthChecks()).thenReturn(testResults);

        StringWriter writer = new StringWriter();
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(writer));

        HealthCheckServlet servlet = new HealthCheckServlet(mockRegistry);

        servlet.doGet(mockRequest, mockResponse);

        Mockito.verify(mockResponse).setStatus(500);
        String result = writer.toString();
        assertTrue("[" + result + "]", result.startsWith("! h4: CRITICAL\n" +
                "\n" +
                "java.lang.RuntimeException: Test exception\n"));
    }
}
