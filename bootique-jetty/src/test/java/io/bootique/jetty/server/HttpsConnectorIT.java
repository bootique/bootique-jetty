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

package io.bootique.jetty.server;

import io.bootique.jetty.JettyModule;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;

public class HttpsConnectorIT {

    private static final String OUT_CONTENT = "https_content_stream_content_stream";
    private static final String SERVICE_URL = "https://localhost:14001/";

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private WebTarget createHttpsClient(String keystore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore trustStore;

        try (InputStream in = getClass().getResourceAsStream(keystore)) {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(in, "supersecret".toCharArray());
        }

        return ClientBuilder.newBuilder().trustStore(trustStore).build().target(SERVICE_URL);
    }

    @Test
    public void testTlsConnector() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/HttpsConnector.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .run();

        Response r1HTTPS = createHttpsClient("testkeystore").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1HTTPS.getStatus());
        assertEquals(OUT_CONTENT + "_true", r1HTTPS.readEntity(String.class));
    }

    @Test
    public void testTlsConnector_MultiCert() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        testFactory.app("-s", "-c", "classpath:io/bootique/jetty/server/HttpsMultiCertConnector.yml")
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .run();

        // TODO: how do we verify that "jetty2" certificate was used, and noth "jetty1"?

        Response r1HTTPS = createHttpsClient("testmulticertkeystore").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1HTTPS.getStatus());
        assertEquals(OUT_CONTENT + "_true", r1HTTPS.readEntity(String.class));
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.getWriter().append(OUT_CONTENT + "_" + req.isSecure());
        }
    }
}
