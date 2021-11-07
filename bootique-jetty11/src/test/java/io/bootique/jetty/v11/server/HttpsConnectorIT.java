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

package io.bootique.jetty.v11.server;

import io.bootique.jetty.v11.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class HttpsConnectorIT {

    private static final String OUT_CONTENT = "____content_stream____";

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testTlsConnector() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        startJetty("classpath:io/bootique/jetty/v11/server/HttpsConnector.yml");

        Response r1HTTPS = createHttpsClient("testkeystore").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1HTTPS.getStatus());
        assertEquals(OUT_CONTENT + "_true", r1HTTPS.readEntity(String.class));
    }

    @Test
    public void testTlsConnector_MultiCert() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        startJetty("classpath:io/bootique/jetty/v11/server/HttpsConnectorIT_MultiCert.yml");

        // TODO: how do we verify that "jetty2" certificate was used, and noth "jetty1"?

        Response r1HTTPS = createHttpsClient("testmulticertkeystore").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), r1HTTPS.getStatus());
        assertEquals(OUT_CONTENT + "_true", r1HTTPS.readEntity(String.class));
    }

    private void startJetty(String config) {
        testFactory.app("-s", "-c", config)
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .run();
    }

    private WebTarget createHttpsClient(String keystore) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore trustStore;

        try (InputStream in = getClass().getResourceAsStream(keystore)) {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(in, "supersecret".toCharArray());
        }

        return ClientBuilder.newBuilder().trustStore(trustStore).build().target("https://localhost:14001/");
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append(OUT_CONTENT + "_" + req.isSecure());
        }
    }
}
