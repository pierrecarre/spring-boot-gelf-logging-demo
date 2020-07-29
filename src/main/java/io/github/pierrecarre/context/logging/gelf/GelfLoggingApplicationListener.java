package io.github.pierrecarre.context.logging.gelf;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.logback.GelfLogbackLoggingSystem;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

public class GelfLoggingApplicationListener implements GenericApplicationListener {

    private static final int ORDER = LoggingApplicationListener.DEFAULT_ORDER - 1;

    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        Class<?> type = resolvableType.getRawClass();
        if (type == null) {
            return false;
        }
        return ApplicationStartingEvent.class.isAssignableFrom(type);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.setProperty(LoggingSystem.SYSTEM_PROPERTY, GelfLogbackLoggingSystem.class.getName());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
