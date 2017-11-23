package io.bootique.jetty.server;

import io.bootique.BQRuntime;
import io.bootique.jetty.unit.JettyApp;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThreadsIT {

    @Rule
    public JettyApp app = new JettyApp();

    @Test
    public void testAcceptorSelectorThreads() {

        BQRuntime runtime = app.start("--config=classpath:io/bootique/jetty/server/threads.yml");

        Connector[] connectors = runtime.getInstance(Server.class).getConnectors();
        ServerConnector c1 = (ServerConnector) connectors[0];
        assertEquals(3, c1.getAcceptors());
        assertEquals(4, c1.getSelectorManager().getSelectorCount());

        ServerConnector c2 = (ServerConnector) connectors[1];

        // default counts are CPU count-sensitive, so do a sanity check instead of an exact match
        assertTrue(c2.getAcceptors() > 0);
        assertTrue(c2.getSelectorManager().getSelectorCount() > 0);
    }
}
