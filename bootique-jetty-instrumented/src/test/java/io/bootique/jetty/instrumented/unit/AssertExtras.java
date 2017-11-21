package io.bootique.jetty.instrumented.unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssertExtras {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertExtras.class);

    public static void assertWithRetry(Runnable test) {

        int maxRetries = 4;
        for (int i = maxRetries; i > 0; i--) {

            try {
                test.run();
                return;
            } catch (AssertionError e) {
                LOGGER.info("Test condition hasn't been reached, will retry {} more time(s)", i);
                try {
                    // sleep a bit longer every time
                    Thread.sleep(100 * (maxRetries - i + 1));
                } catch (InterruptedException e1) {
                }
            }
        }

        // fail for real
        test.run();
    }
}
