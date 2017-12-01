package io.bootique.jetty;

import io.bootique.test.junit.BQTestFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.Servlet;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class MappedListenerIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory().autoLoadModules();

    private Servlet mockServlet1;


    @Before
    public void before() {
        SharedState.reset();
        this.mockServlet1 = mock(Servlet.class);
    }

    @Test
    public void testAddMappedListener_Ordering1() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(mockServlet1, "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 1))
                        .addMappedListener(new MappedListener<>(new RL2(), 2)))
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        assertEquals(200, base.path("/").request().get().getStatus());
        assertEquals("_RL1_init_RL2_init_RL2_destroy_RL1_destroy", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_Ordering2() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(mockServlet1, "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 2))
                        .addMappedListener(new MappedListener<>(new RL2(), 1)))
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        assertEquals(200, base.path("/").request().get().getStatus());
        assertEquals("_RL2_init_RL1_init_RL1_destroy_RL2_destroy", SharedState.getAndReset());
    }

    @Test
    public void testAddMappedListener_OrderingVsUnmapped() {

        testFactory.app("-s")
                .module(b -> JettyModule.extend(b)
                        .addServlet(mockServlet1, "s1", "/*")
                        .addMappedListener(new MappedListener<>(new RL1(), 2))
                        .addListener(new RL3())
                        .addMappedListener(new MappedListener<>(new RL2(), 1))
                )
                .createRuntime()
                .run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:8080");
        assertEquals(200, base.path("/").request().get().getStatus());
        assertEquals("_RL2_init_RL1_init_RL3_init_RL3_destroy_RL1_destroy_RL2_destroy", SharedState.getAndReset());
    }

    public static class SharedState {
        private static StringBuilder BUFFER;

        static void reset() {
            BUFFER = new StringBuilder();
        }

        static void append(String value) {
            BUFFER.append(value);
        }

        static String getAndReset() {
            String val = BUFFER.toString();
            reset();
            return val;
        }
    }

    public static class RL1 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL1_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL1_destroy");
        }
    }

    public static class RL2 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL2_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL2_destroy");
        }
    }

    public static class RL3 implements ServletRequestListener {

        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            SharedState.append("_RL3_init");
        }

        @Override
        public void requestDestroyed(ServletRequestEvent sre) {
            SharedState.append("_RL3_destroy");
        }
    }
}
