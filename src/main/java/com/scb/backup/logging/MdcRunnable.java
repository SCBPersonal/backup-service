package com.scb.backup.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;

@Slf4j
public class MdcRunnable implements Runnable {
    private final Runnable delegate;
    private final Map<String, String> contextMap;

    public MdcRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.contextMap = MDC.getCopyOfContextMap();
    }

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