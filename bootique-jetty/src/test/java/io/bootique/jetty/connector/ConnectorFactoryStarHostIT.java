package io.bootique.jetty.connector;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.command.CommandOutcome;
import io.bootique.jetty.JettyModule;
import io.bootique.junit.BQTest;
import io.bootique.junit.BQTestFactory;
import io.bootique.junit.BQTestTool;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class ConnectorFactoryStarHostIT {
    private static final String OUT_CONTENT = "____content_stream____";

    @BQTestTool
    static final BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void listenOnAllInterfaces() throws SocketException {
        BQRuntime app = testFactory.app("-s")
                .autoLoadModules()
                .module(b -> {
                    BQCoreModule.extend(b)
                            .setProperty("bq.jetty.connectors[0].host", "*")
                            .setProperty("bq.jetty.connectors[0].port", "8080")
                            .setProperty("bq.jetty.connectors[0].type", "http");
                    JettyModule.extend(b).addServlet(ContentServlet.class);
                })
                .createRuntime();

        CommandOutcome out = app.run();
        assertTrue(out.isSuccess());

        List<String> urls = localIPv4Urls();
        assertFalse(urls.isEmpty(), "No local IPv4 interfaces to test against");

        // Short timeouts so a stray unreachable address fails fast instead of hanging the build for minutes
        try (Client client = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()) {
            for (String url : urls) {
                Response response = client.target(url).request().get();
                assertEquals(OK.getStatusCode(), response.getStatus());
                assertEquals(OUT_CONTENT, response.readEntity(String.class));
            }
        }
    }

    // Collects IPv4 URLs for "real" local interfaces only. Point-to-point (VPN tunnels like macOS utun*),
    // virtual, and down interfaces are skipped: with host "*" Jetty binds to 0.0.0.0 and is reachable on
    // physical/loopback interfaces, but a VPN tunnel's local address typically won't accept a connect and
    // would otherwise hang the test.
    private static List<String> localIPv4Urls() throws SocketException {
        return NetworkInterface.networkInterfaces()
                .filter(ConnectorFactoryStarHostIT::isReachableInterface)
                .flatMap(NetworkInterface::inetAddresses)
                .map(InetAddress::getHostAddress)
                .filter(hostAddress -> hostAddress.contains("."))
                .map(host -> "http://" + host + ":8080/")
                .toList();
    }

    private static boolean isReachableInterface(NetworkInterface iface) {
        try {
            return iface.isUp() && !iface.isPointToPoint() && !iface.isVirtual();
        } catch (SocketException e) {
            return false;
        }
    }

    @WebServlet(urlPatterns = "/*")
    static class ContentServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().append(OUT_CONTENT);
        }
    }

}
