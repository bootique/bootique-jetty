package io.bootique.jetty.command;

import io.bootique.BQRuntime;
import io.bootique.BootiqueException;
import io.bootique.command.CommandOutcome;
import io.bootique.jetty.test.junit.JettyTestFactory;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Deprecated
public class JettyTestFactoryIT {

    @Rule
    public JettyTestFactory testFactory = new JettyTestFactory();

    @Test
    public void testStartSuccess() {

        BQRuntime runtime = testFactory.app().start();

        Optional<CommandOutcome> outcome = testFactory.getOutcome(runtime);
        assertTrue(outcome.isPresent());
        assertTrue(outcome.get().isSuccess());
        assertTrue(outcome.get().forkedToBackground());

        new ThreadTester().assertPoolSize(3);
    }

    @Test
    public void testStart_AlreadyRunning() {

        // block port
        testFactory.app().start();

        try {
            testFactory.app().start();
            fail("Must have failed to start - address is already in use");
        } catch (BootiqueException e) {
            assertEquals(1, e.getOutcome().getExitCode());
            assertEquals("Daemon failed to start: [1: Address already in use]", e.getMessage());
        }
    }

    private static class ThreadTester {

        public void assertPoolSize(int expectedAtLeast) {
            long matched = allThreads().filter(this::isJettyThread).count();
            assertTrue("Too few Jetty threads: " + matched, expectedAtLeast <= matched);
        }

        private boolean isJettyThread(Thread t) {
            return t.getName().startsWith("bootique-http-");
        }

        private Stream<Thread> allThreads() {
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            while (tg.getParent() != null) {
                tg = tg.getParent();
            }

            Thread[] active = new Thread[tg.activeCount()];
            tg.enumerate(active);
            return Arrays.stream(active);
        }
    }
}
