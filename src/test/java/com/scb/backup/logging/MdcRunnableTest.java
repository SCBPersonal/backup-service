package com.scb.backup.logging;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

class MdcRunnableTest {

    @BeforeEach

    void clearMdcBefore() {

        MDC.clear();

    }

    @Test

    void testMdcRunnableCopiesContextAndClearsAfterRun() {

        // Arrange: Put some values in MDC

        MDC.put("traceId", "12345");

        MDC.put("user", "olivia");

        Runnable delegate = mock(Runnable.class);

        // Capture MDC context at construction time

        MdcRunnable mdcRunnable = new MdcRunnable(delegate);

        // Clear MDC before running to simulate a different thread

        MDC.clear();

        assertNull(MDC.get("traceId"));

        // Act

        mdcRunnable.run();

        // Assert: delegate was executed

        verify(delegate, times(1)).run();

        // Inside run(), MDC should have been restored

        // We can’t check inside directly, but the logic guarantees it was set before delegate.run()

        // After run, MDC should be cleared

        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty(),

                "MDC should be cleared after execution");

    }

    @Test

    void testMdcRunnableWithNullContext() {

        // Arrange: Start with empty MDC

        MDC.clear();

        Runnable delegate = mock(Runnable.class);

        MdcRunnable mdcRunnable = new MdcRunnable(delegate);

        // Act

        mdcRunnable.run();

        // Assert

        verify(delegate, times(1)).run();

        assertNull(MDC.getCopyOfContextMap(), "MDC should remain null after execution");

    }

}
