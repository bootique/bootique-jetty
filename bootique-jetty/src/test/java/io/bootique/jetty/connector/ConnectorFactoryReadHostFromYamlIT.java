package io.bootique.jetty.connector;

import io.bootique.BQRuntime;
import io.bootique.command.CommandOutcome;
import io.bootique.jetty.JettyModule;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BQTest
public class ConnectorFactoryReadHostFromYamlIT {
    private static final String OUT_CONTENT = "____content_stream____";

    private static final String configPath = "classpath:io/bootique/jetty/connector/ReadHostFromYaml.yml";

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory();

    @Test
    @DisplayName("Should read from yaml properly")
    public void readHostFromYaml() throws SocketException {
        BQRuntime app = testFactory.app("-s", "-c", configPath)
                .autoLoadModules()
                .module(b -> JettyModule.extend(b).addServlet(ContentServlet.class))
                .createRuntime();

        CommandOutcome out = app.run();
        assertTrue(out.isSuccess());

        NetworkInterface.networkInterfaces()
                .flatMap(NetworkInterface::inetAddresses)
                .map(InetAddress::getHostAddress)
                .filter(hostAddress -> hostAddress.contains("."))
                .map(host -> "http://" + host + ":8080/")
                .forEach(host -> {
                    Response response = ClientBuilder.newClient().target(host).request().get();
                    assertEquals(OK.getStatusCode(), response.getStatus());
                    assertEquals(OUT_CONTENT, response.readEntity(String.class));
                });
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }

}
