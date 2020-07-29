package io.github.pierrecarre.logging.gelf.annotation;

import io.github.pierrecarre.logging.gelf.autoconfigure.GelfLoggingAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GelfLoggingAutoConfiguration.class)
public @interface EnableGelfLogging {
}
