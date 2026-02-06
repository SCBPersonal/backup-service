package com.scb.backup.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MdcRunnable - Wrapper for Runnable that preserves MDC (Mapped Diagnostic Context).
 *
 * This class wraps a Runnable to ensure that the SLF4J MDC context is properly
 * propagated when executing tasks in different threads. This is essential for
 * maintaining correlation IDs and other contextual logging information across
 * asynchronous operations.
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * Runnable task = new MdcRunnable(() -> {
 *     // Task code here - MDC context is preserved
 * });
 * executor.execute(task);
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@Slf4j
public class MdcRunnable implements Runnable {
    private final Runnable delegate;
    private final Map<String, String> contextMap;

    /**
     * Constructs an MdcRunnable that wraps the given Runnable.
     *
     * The current MDC context is captured at construction time and will be
     * restored when the task is executed.
     *
     * @param delegate The Runnable to wrap and execute with MDC context
     */
    public MdcRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.contextMap = MDC.getCopyOfContextMap();
    }

    /**
     * Executes the wrapped Runnable with the captured MDC context.
     *
     * The MDC context is set before execution and cleared after completion
     * to prevent context leakage between tasks.
     */
    @Override
    public void run() {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
        try {
            delegate.run();
        } finally {
            log.info("clearing MDC context after execution of Runnable");
            MDC.clear();
        }
    }
}